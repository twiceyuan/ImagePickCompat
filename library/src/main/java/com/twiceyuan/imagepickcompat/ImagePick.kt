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
import androidx.core.content.FileProvider
import com.twiceyuan.imagepickcompat.options.CropOptions
import com.twiceyuan.imagepickcompat.result.CropCallback
import com.twiceyuan.imagepickcompat.result.CropResult
import com.twiceyuan.imagepickcompat.result.PickCallback
import com.twiceyuan.imagepickcompat.result.PickResult
import com.twiceyuan.imagepickcompat.result.TakePhotoCallback
import com.twiceyuan.imagepickcompat.result.TakePhotoResult
import com.twiceyuan.imagepickcompat.result.onTakeCancel
import com.twiceyuan.imagepickcompat.result.onTakeSuccess
import com.twiceyuan.imagepickcompat.utils.FileProviderUtil.getUriByFileProvider
import com.twiceyuan.imagepickcompat.utils.PermissionUtil.grantUriPermission
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
    private val sHandlerMap: MutableMap<Int, ResultHandler> = LinkedHashMap()

    fun pickGallery(activity: Activity, callback: (Uri) -> Unit) {
        pickGallery(activity, PickCallback {
            if (it is PickResult.Success) {
                callback(it.uri)
            }
        })
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun pickGallery(activity: Activity, callback: PickCallback) {
        //选择图库的图片
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        val pickRequestCode = callback.hashCode()
        activity.startActivityForResult(intent, pickRequestCode)
        sHandlerMap[activity.hashCode()] = object : SimpleResultHandler(pickRequestCode) {
            override fun handle(data: Intent?) {
                val originUri = data?.data
                if (originUri == null) {
                    Log.e(TAG, "handle, originUri is null")
                    return
                }
                if (isRemoteUri(originUri) || isGooglePhotosUri(originUri)) {
                    // 网络图片或者 Google Photos 的图片没有权限进行进一步操作，所以缓存保存到本地
                    val bitmapFromUri = getBitmapFromUri(activity, originUri)
                    if (bitmapFromUri != null) {
                        val bitmapToFile = saveBitmapToFile(activity, bitmapFromUri)
                        callback.onPick(
                            PickResult.Success(
                                getImageContentUri(
                                    activity,
                                    bitmapToFile
                                )
                            )
                        )
                    } else {
                        callback.onPick(PickResult.Success(originUri))
                    }
                } else {
                    callback.onPick(PickResult.Success(originUri))
                }
            }

            override fun onCancel() {
                callback.onPick(PickResult.Cancelled)
            }
        }
    }

    fun takePhoto(activity: Activity, callback: (Uri) -> Unit) {
        takePhoto(activity, TakePhotoCallback {
            if (it is TakePhotoResult.Success) {
                callback(it.uri)
            }
        })
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun takePhoto(activity: Activity, callback: TakePhotoCallback) {
        kotlin.runCatching {
            takePhotoInternal(activity, callback)
        }.onFailure {
            callback.onTake(TakePhotoResult.Unknown(it))
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun takePhotoInternal(activity: Activity, callback: TakePhotoCallback) {
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
        val pickRequestCode = callback.hashCode()
        activity.startActivityForResult(intent, pickRequestCode)
        sHandlerMap[activity.hashCode()] = object : SimpleResultHandler(pickRequestCode) {
            override fun handle(data: Intent?) {
                callback.onTakeSuccess(photoURI)
            }

            override fun onCancel() {
                callback.onTakeCancel()
            }
        }
    }

    /**
     * Crop use default options
     *
     *
     * other see [ImagePick.crop]
     */
    fun crop(activity: Activity, uri: Uri, callback: (Uri) -> Unit) {
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
    fun crop(activity: Activity, options: CropOptions, uri: Uri, callback: (Uri) -> Unit) {
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
    fun crop(activity: Activity, uri: Uri, options: CropOptions, callback: CropCallback) {
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
        val cropRequestCode = callback.hashCode()
        activity.startActivityForResult(intent, cropRequestCode)
        sHandlerMap[activity.hashCode()] = object : SimpleResultHandler(cropRequestCode) {
            override fun handle(data: Intent?) {
                callback.onCrop(CropResult.Success(outputUri))
            }

            override fun onCancel() {
                callback.onCrop(CropResult.Cancelled)
            }
        }
    }

    /**
     * Is a google photos uri?
     */
    private fun isGooglePhotosUri(uri: Uri): Boolean {
        return uri.host == Constants.PACKAGE_NAME_GOOGLE_PHOTOS
    }

    /**
     * handle pick or crop result data. intercept when it return false.
     *
     * @return Return true if data was handled, otherwise return false.
     */
    @JvmStatic
    fun handleResult(
        activity: Activity,
        requestCode: Int,
        resultCode: Int,
        intent: Intent?
    ): Boolean {
        val handler = sHandlerMap[activity.hashCode()] ?: return false
        return handler.handleResult(requestCode, resultCode, intent)
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

    /**
     * 可以在选择图片的 onDestroy 中调用，清楚拍照的缓存
     */
    @JvmStatic
    fun clearImageDir(context: Context) {
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
