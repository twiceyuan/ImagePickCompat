package com.twiceyuan.imagepickcompat;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import com.twiceyuan.imagepickcompat.callback.Constants;
import com.twiceyuan.imagepickcompat.callback.ImageCallback;
import com.twiceyuan.imagepickcompat.options.CropOptions;
import com.twiceyuan.imagepickcompat.options.PickOptions;
import com.twiceyuan.imagepickcompat.options.TakeCameraOptions;
import com.twiceyuan.imagepickcompat.utils.FileProviderUtil;
import com.twiceyuan.imagepickcompat.utils.PermissionUtil;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by twiceYuan on 2017/6/16.
 * <p>
 * AppCompatImagePick - Provide universal api to take / pick photo or crop a image.
 * <p>
 * Compatible for Google Photos and Android File Provider.
 */
public class ImagePick {

    private static boolean LOG = BuildConfig.DEBUG;

    private static Map<Integer, ResultHandler> sHandlerMap = new LinkedHashMap<>();

    public static void pickGallery(final Activity activity, final ImageCallback callback) {
        pickGallery(activity, new PickOptions(), callback);
    }

    public static void pickGallery(final Activity activity, @NonNull final PickOptions options, final ImageCallback callback) {

        if (options.pickAction != null) {
            options.pickAction.call();
        }

        //选择图库的图片
        final Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        final int pickRequestCode = callback.hashCode();

        try {
            activity.startActivityForResult(intent, pickRequestCode);
        } catch (ActivityNotFoundException e) {
            if (options.noGalleryCallback != null) {
                options.noGalleryCallback.call();
            } else {
                Toast.makeText(activity, R.string.no_gallery_app, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        sHandlerMap.put(activity.hashCode(), new SimpleResultHandler(pickRequestCode) {
            @Override
            void handle(Intent data) {
                Uri originUri = data.getData();
                if (originUri == null) {
                    callback.call(null);
                    return;
                }

                if (isRemoteUri(originUri) || isGooglePhotosUri(originUri)) {
                    // 网络图片或者 Google Photos 的图片没有权限进行进一步操作，所以缓存保存到本地
                    Bitmap bitmapFromUri = getBitmapFromUri(activity, originUri);
                    File bitmapToFile = saveBitmapToFile(activity, bitmapFromUri);
                    callback.call(getImageContentUri(activity, bitmapToFile));
                } else {
                    callback.call(originUri);
                }
            }

            @Override
            void onCancel() {
                if (options.cancelAction != null) {
                    options.cancelAction.call();
                } else {
                    Toast.makeText(activity, R.string.cancel_choose, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Is a google photos uri?
     */
    private static boolean isGooglePhotosUri(Uri uri) {
        return uri.getHost().equals(Constants.PACKAGE_NAME_GOOGLE_PHOTOS);
    }

    /**
     * handle pick or crop result data. intercept when it return false.
     *
     * @return Return true if data was handled, otherwise return false.
     */
    public static boolean handleResult(Activity activity, int requestCode, int resultCode, Intent intent) {
        ResultHandler handler = sHandlerMap.get(activity.hashCode());

        if (handler == null) {
            return false;
        }

        //noinspection RedundantIfStatement
        if (handler.handleResult(requestCode, resultCode, intent)) {
            return true;
        }

        return false;
    }

    private static boolean isRemoteUri(Uri uri) {
        String lastPathSegment = uri.getLastPathSegment();
        return lastPathSegment.startsWith("http");
    }

    /**
     * Fetch a content uri with the file provider api.
     * <p>
     * Don’t use Uri.fromFile(). It forces receiving apps to have the READ_EXTERNAL_STORAGE permission, won’t work at
     * all if you are trying to share across users, and in versions of Android lower than 4.4 (API level 19), would
     * require your app to have WRITE_EXTERNAL_STORAGE. And really important share targets, such as the Gmail app,
     * don't have the READ_EXTERNAL_STORAGE, causing this call to fail. Instead, you can use URI permissions to grant
     * other apps access to specific URIs. While URI permissions don’t work on file:// URIs as is generated by
     * Uri.fromFile(), they do work on Uris associated with Content Providers. Rather than implement your own just for
     * this, you can and should use FileProvider as explained in File Sharing.
     */
    private static Uri getImageContentUri(Context context, File imageFile) {
        return FileProvider.getUriForFile(context, context.getPackageName() + ".image_provider", imageFile);
    }

    private static File saveBitmapToFile(Context context, Bitmap bitmap) {
        FileOutputStream stream;
        File file;
        try {
            file = createPhotoFile(context);
            try {
                stream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                return file;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Bitmap getBitmapFromUri(Context context, Uri uri) {
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor = context.getContentResolver().openFileDescriptor(uri, "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        FileDescriptor fileDescriptor;
        if (parcelFileDescriptor != null) {
            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            try {
                parcelFileDescriptor.close();
                return image;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    public static void setLogEnable(boolean LOG) {
        ImagePick.LOG = LOG;
    }

    public static void takePhoto(final Activity activity, final ImageCallback callback) {
        takePhoto(activity, new TakeCameraOptions(), callback);
    }

    public static void takePhoto(final Activity activity, @NonNull final TakeCameraOptions options, final ImageCallback callback) {

        if (options.takePhotoAction != null) {
            options.takePhotoAction.call();
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (intent.resolveActivity(activity.getPackageManager()) == null) {
            if (options.noCameraCallback != null) {
                options.noCameraCallback.call();
            } else {
                Toast.makeText(activity, R.string.no_crop_app, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        File photoFile = null;
        try {
            photoFile = createPhotoFile(activity);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (photoFile != null) {
            final Uri photoURI = FileProvider.getUriForFile(activity,
                    activity.getPackageName() + activity.getString(R.string.provider_name),
                    photoFile);

            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

            final int pickRequestCode = callback.hashCode();

            try {
                activity.startActivityForResult(intent, pickRequestCode);
            } catch (ActivityNotFoundException e) {
                if (options.noCameraCallback != null) {
                    options.noCameraCallback.call();
                } else {
                    Toast.makeText(activity, R.string.no_camera_app, Toast.LENGTH_SHORT).show();
                }
                return;
            }

            sHandlerMap.put(activity.hashCode(), new SimpleResultHandler(pickRequestCode) {
                @Override
                void handle(Intent data) {
                    callback.call(photoURI);
                }

                @Override
                void onCancel() {
                    if (options.cancelAction != null) {
                        options.cancelAction.call();
                    } else {
                        Toast.makeText(activity, R.string.cancel_take_photo, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private static File createPhotoFile(Context context) throws IOException {
        String imageFileName = "IMAGE_PICK_" + System.currentTimeMillis();
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private static File createCropFile(Context context) throws IOException {
        String imageFileName = "CROP_" + System.currentTimeMillis();
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    /**
     * 可以在选择图片的 onDestroy 中调用，清楚拍照的缓存
     */
    public static void clearImageDir(Context context) {
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir != null) {
            File[] files = storageDir.listFiles();
            for (File file : files) {
                if (!file.delete()) {
                    log("文件删除失败: " + String.valueOf(file));
                }
            }
        }
    }

    /**
     * Universal log
     *
     * @param message log message
     */
    private static void log(String message) {
        if (LOG) {
            Log.e(ImagePick.class.getSimpleName(), message);
        }
    }

    /**
     * Crop use default options
     * <p>
     * other see {@link ImagePick#crop(Activity, Uri, CropOptions, ImageCallback)}
     */
    public static void crop(final Activity activity, Uri uri, final ImageCallback callback) {
        crop(activity, uri, new CropOptions(), callback);
    }

    /**
     * Crop a image from a uri
     *
     * @param activity Context
     * @param uri      Image Uri
     * @param options  Crop options
     * @param callback Crop result callback
     */
    @SuppressWarnings("WeakerAccess")
    public static void crop(final Activity activity, Uri uri, @NonNull final CropOptions options, final ImageCallback callback) {
        final Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setType("image/*");
        List<ResolveInfo> list = activity.getPackageManager().queryIntentActivities(intent, 0);
        if (list.isEmpty()) {

            if (options.noCropCallback != null) {
                options.noCropCallback.call();
            } else {
                Toast.makeText(activity, R.string.no_crop_app, Toast.LENGTH_SHORT).show();
            }
        } else {
            intent.setData(uri);

            intent.putExtra("outputX", options.outputX);
            intent.putExtra("outputY", options.outputY);
            intent.putExtra("aspectX", options.aspectX);
            intent.putExtra("aspectY", options.aspectY);
            intent.putExtra("scale", options.scale);
            intent.putExtra("return-data", false);

            File cropFile;
            try {
                cropFile = createCropFile(activity);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(activity, R.string.create_cache_fail, Toast.LENGTH_SHORT).show();
                return;
            }

            final Uri outputUri = FileProviderUtil.getUriByFileProvider(activity, cropFile);

            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            PermissionUtil.grantUriPermission(activity, list, outputUri);

            // 设置裁剪结果输出 uri
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);

            int cropRequestCode = callback.hashCode();

            try {
                activity.startActivityForResult(intent, cropRequestCode);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(activity, R.string.no_crop_app, Toast.LENGTH_SHORT).show();
                return;
            }

            sHandlerMap.put(activity.hashCode(), new SimpleResultHandler(cropRequestCode) {
                @Override
                void handle(Intent data) {
                    callback.call(outputUri);
                }

                @Override
                void onCancel() {
                    if (options.cancelCropCallback != null) {
                        options.cancelCropCallback.call();
                    }
                }
            });
        }
    }
}
