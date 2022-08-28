package com.twiceyuan.imagepickcompat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.twiceyuan.imagepickcompat.options.CropOptions
import com.twiceyuan.imagepickcompat.result.CropCallback
import com.twiceyuan.imagepickcompat.result.CropResult
import com.twiceyuan.imagepickcompat.result.PickCallback
import com.twiceyuan.imagepickcompat.result.PickResult
import com.twiceyuan.imagepickcompat.result.TakePhotoCallback
import com.twiceyuan.imagepickcompat.result.TakePhotoResult
import com.twiceyuan.imagepickcompat.utils.ActivityResult
import com.twiceyuan.imagepickcompat.utils.FileProviderUtil.getUriByFileProvider
import com.twiceyuan.imagepickcompat.utils.PermissionUtil.grantUriPermission
import com.twiceyuan.imagepickcompat.utils.startWithCallback
import java.io.File
import java.io.FileDescriptor
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

/**
 * Created by twiceYuan on 2017/6/16.
 *
 *
 * AppCompatImagePick - Provide universal api to take / pick photo or crop a image.
 *
 *
 * Compatible for Google Photos and Android File Provider.
 */
@Suppress("TooManyFunctions")
object ImagePick {

    private var isLoggable = BuildConfig.DEBUG
    private const val TAG = "ImagePick"

    /**
     * 从相册选择图片，只关心成功结果
     */
    fun pickGallery(activity: ComponentActivity, callback: (Uri) -> Unit) {
        pickGallery(activity, PickCallback {
            if (it is PickResult.Success) {
                callback(it.uri)
            }
        })
    }

    /**
     * 从相册选择图片
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun pickGallery(activity: ComponentActivity, callback: PickCallback) {
        kotlin.runCatching {
            pickGalleryInternal(activity, callback)
        }.onFailure {
            callback.onPick(PickResult.Unknown(it))
        }
    }

    /**
     * 从相册选择图片
     */
    private fun pickGalleryInternal(activity: ComponentActivity, callback: PickCallback) {
        activity.registerCleaner()
        //选择图库的图片
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.startWithCallback(activity) {
            when (it.resultCode) {
                Activity.RESULT_OK -> {
                    callback.onPick(onPickResultOk(activity, it))
                }
                Activity.RESULT_CANCELED -> {
                    callback.onPick(PickResult.Cancelled)
                }
                else -> {
                    callback.onPick(PickResult.Unknown(unknownActivityResult(it)))
                }
            }
        }
    }

    private fun onPickResultOk(activity: ComponentActivity, result: ActivityResult): PickResult {
        val originUri = result.result?.data
            ?: return PickResult.Unknown(unknownActivityResult(result))

        if (isRemoteUri(originUri) || isGooglePhotosUri(originUri)) {
            // 网络图片或者 Google Photos 的图片没有权限进行进一步操作，所以缓存保存到本地
            val bitmapFromUri = getBitmapFromUri(activity, originUri)
                ?: return PickResult.Success(originUri)

            val bitmapToFile = saveBitmapToFile(activity, bitmapFromUri)
            return PickResult.Success(
                getImageContentUri(
                    activity,
                    bitmapToFile
                )
            )
        } else {
            return (PickResult.Success(originUri))
        }
    }

    /**
     * 调用相机拍照，只关心成功结果
     */
    fun takePhoto(activity: ComponentActivity, callback: (Uri) -> Unit) {
        takePhoto(activity, TakePhotoCallback {
            if (it is TakePhotoResult.Success) {
                callback(it.uri)
            }
        })
    }

    /**
     * 调用相机拍照
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun takePhoto(activity: ComponentActivity, callback: TakePhotoCallback) {
        kotlin.runCatching {
            takePhotoInternal(activity, callback)
        }.onFailure {
            callback.onTake(TakePhotoResult.Unknown(it))
        }
    }

    private fun takePhotoInternal(activity: ComponentActivity, callback: TakePhotoCallback) {
        activity.registerCleaner()
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(activity.packageManager) == null) {
            callback.onTake(TakePhotoResult.NoCamera)
            return
        }
        val photoFile: File = createPhotoFile(activity)
        val photoURI = FileProvider.getUriForFile(
            activity,
            activity.packageName + Constants.FILE_PROVIDER_NAME,
            photoFile
        )
        val list = activity.packageManager.queryIntentActivities(intent, 0)
        grantUriPermission(activity, list, photoURI)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        intent.startWithCallback(activity) {
            when (it.resultCode) {
                Activity.RESULT_OK -> {
                    callback.onTake(TakePhotoResult.Success(photoURI))
                }
                Activity.RESULT_CANCELED -> {
                    callback.onTake(TakePhotoResult.Cancelled)
                }
                else -> {
                    callback.onTake(TakePhotoResult.Unknown(unknownActivityResult(it)))
                }
            }
        }
    }

    /**
     * Crop use default options
     *
     *
     * other see [ImagePick.crop]
     */
    fun crop(activity: ComponentActivity, uri: Uri, callback: (Uri) -> Unit) {
        crop(activity, uri, CropOptions()) {
            if (it is CropResult.Success) {
                callback(it.uri)
            }
        }
    }

    /**
     * Crop use default options
     *
     *
     * other see [ImagePick.crop]
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun crop(activity: ComponentActivity, options: CropOptions, uri: Uri, callback: (Uri) -> Unit) {
        crop(activity, uri, options) {
            if (it is CropResult.Success) {
                callback(it.uri)
            }
        }
    }

    /**
     * Crop a image from a uri
     *
     * @param activity Context
     * @param uri      Image Uri
     * @param options  Crop options
     * @param callback Crop result callback
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun crop(activity: ComponentActivity, uri: Uri, options: CropOptions, callback: CropCallback) {
        runCatching {
            cropInternal(activity, uri, options, callback)
        }.onFailure {
            callback.onCrop(CropResult.Unknown(it))
        }
    }

    /**
     * Crop a image from a uri
     *
     * @param activity Context
     * @param uri      Image Uri
     * @param options  Crop options
     * @param callback Crop result callback
     */
    private fun cropInternal(
        activity: ComponentActivity,
        uri: Uri,
        options: CropOptions,
        callback: CropCallback
    ) {
        activity.registerCleaner()
        val intent = Intent("com.android.camera.action.CROP")
        intent.setDataAndType(uri, "image/*")
        val list = activity.packageManager.queryIntentActivities(intent, 0)
        if (list.isEmpty()) {
            callback.onCrop(CropResult.NoCropHandler)
            return
        }
        intent.putExtra("outputX", options.outputX)
        intent.putExtra("outputY", options.outputY)
        intent.putExtra("aspectX", options.aspectX)
        intent.putExtra("aspectY", options.aspectY)
        intent.putExtra("scale", options.scale)
        intent.putExtra("return-data", false)
        val cropFile: File = try {
            createCropFile(activity)
        } catch (e: IOException) {
            callback.onCrop(CropResult.Unknown(e))
            return
        }
        val outputUri = getUriByFileProvider(activity, cropFile)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        grantUriPermission(activity, list, outputUri)

        // 设置裁剪结果输出 uri
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
        intent.startWithCallback(activity) {
            when (it.resultCode) {
                Activity.RESULT_OK -> {
                    callback.onCrop(CropResult.Success(outputUri))
                }
                Activity.RESULT_CANCELED -> {
                    callback.onCrop(CropResult.Cancelled)
                }
                else -> {
                    callback.onCrop(CropResult.Unknown(unknownActivityResult(it)))
                }
            }
        }
    }

    private fun unknownActivityResult(it: ActivityResult) =
        IllegalStateException(
            "Crop failed: resultCode=${it.resultCode}, data=${it.result}"
        )

    /**
     * Is a google photos uri?
     */
    private fun isGooglePhotosUri(uri: Uri): Boolean {
        return uri.host == Constants.PACKAGE_NAME_GOOGLE_PHOTOS
    }

    private fun isRemoteUri(uri: Uri): Boolean {
        val lastPathSegment = uri.lastPathSegment
        return lastPathSegment!!.startsWith("http")
    }

    /**
     * Fetch a content uri with the file provider api.
     *
     *
     * Don’t use Uri.fromFile(). It forces receiving apps to have the READ_EXTERNAL_STORAGE permission, won’t work at
     * all if you are trying to share across users, and in versions of Android lower than 4.4 (API level 19), would
     * require your app to have WRITE_EXTERNAL_STORAGE. And really important share targets, such as the Gmail app,
     * don't have the READ_EXTERNAL_STORAGE, causing this call to fail. Instead, you can use URI permissions to grant
     * other apps access to specific URIs. While URI permissions don’t work on file:// URIs as is generated by
     * Uri.fromFile(), they do work on Uris associated with Content Providers. Rather than implement your own just for
     * this, you can and should use FileProvider as explained in File Sharing.
     */
    private fun getImageContentUri(context: Context, imageFile: File?): Uri {
        return FileProvider.getUriForFile(
            context,
            context.packageName + ".image_provider",
            imageFile!!
        )
    }

    private fun saveBitmapToFile(context: Context, bitmap: Bitmap): File? {
        val stream: FileOutputStream
        val file: File
        return try {
            file = createPhotoFile(context)
            try {
                stream = FileOutputStream(file)
                @Suppress("MagicNumber")
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                file
            } catch (e: FileNotFoundException) {
                Log.e(TAG, "saveBitmapToFile", e)
                null
            }
        } catch (e: IOException) {
            Log.e(TAG, "saveBitmapToFile", e)
            null
        }
    }

    private fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        var parcelFileDescriptor: ParcelFileDescriptor? = null
        try {
            parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "getBitmapFromUri", e)
        }
        val fileDescriptor: FileDescriptor
        return if (parcelFileDescriptor != null) {
            fileDescriptor = parcelFileDescriptor.fileDescriptor
            val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
            try {
                parcelFileDescriptor.close()
                image
            } catch (e: IOException) {
                Log.e(TAG, "getBitmapFromUri", e)
                null
            }
        } else {
            null
        }
    }

    @Suppress("unused")
    fun setLogEnable(isLoggable: Boolean) {
        this.isLoggable = isLoggable
    }

    @Throws(IOException::class)
    private fun createPhotoFile(context: Context): File {
        val imageFileName = "IMAGE_PICK_" + System.currentTimeMillis()
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    @Throws(IOException::class)
    private fun createCropFile(context: Context): File {
        val imageFileName = "CROP_" + System.currentTimeMillis()
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    private fun clearImageDir(context: Context) {
        val storageDir = context.getDir(Constants.CACHE_DIR_NAME, Context.MODE_PRIVATE) ?: return
        val files = storageDir.listFiles()
        if (files != null) {
            for (file in files) {
                if (!file.delete()) {
                    log("文件删除失败: $file")
                }
            }
        }
    }

    /**
     * 注册 destroy 时的清除缓存操作
     */
    private fun ComponentActivity.registerCleaner() {
        lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_DESTROY) {
                    clearImageDir(this@registerCleaner)
                }
            }
        })
    }

    /**
     * Universal log
     *
     * @param message log message
     */
    private fun log(message: String) {
        if (isLoggable) {
            Log.e(ImagePick::class.java.simpleName, message)
        }
    }
}
