package com.twiceyuan.imagepickcompat;

import android.Manifest;
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
import android.support.annotation.RequiresPermission;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import com.twiceyuan.imagepickcompat.callback.ImageCallback;
import com.twiceyuan.imagepickcompat.options.CropOptions;

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
public class AppCompatImagePick {

    private static boolean LOG = BuildConfig.DEBUG;

    private static Map<Integer, ResultHandler> sHandlerMap = new LinkedHashMap<>();

    @RequiresPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    public static void pickGallery(final Activity activity, final ImageCallback callback) {

        //选择图库的图片
        final Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        final int pickRequestCode = callback.hashCode();

        try {
            activity.startActivityForResult(intent, pickRequestCode);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.no_gallery_app, Toast.LENGTH_SHORT).show();
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
                Toast.makeText(activity, R.string.cancel_choose, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static boolean isGooglePhotosUri(Uri uri) {
        return uri.getHost().equals("com.google.android.apps.photos.contentprovider");
    }

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
        AppCompatImagePick.LOG = LOG;
    }

    public static void pickCamera(final Activity activity, final ImageCallback callback) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (intent.resolveActivity(activity.getPackageManager()) == null) {
            Toast.makeText(activity, R.string.no_crop_app, Toast.LENGTH_SHORT).show();
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
                Toast.makeText(activity, R.string.no_camera_app, Toast.LENGTH_SHORT).show();
                return;
            }

            sHandlerMap.put(activity.hashCode(), new SimpleResultHandler(pickRequestCode) {
                @Override
                void handle(Intent data) {
                    callback.call(photoURI);
                }

                @Override
                void onCancel() {
                    Toast.makeText(activity, R.string.cancel_take_photo, Toast.LENGTH_SHORT).show();
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
            Log.e(AppCompatImagePick.class.getSimpleName(), message);
        }
    }

    /**
     * Crop use default options
     * <p>
     * other see {@link AppCompatImagePick#crop(Activity, Uri, CropOptions, ImageCallback)}
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
    public static void crop(final Activity activity, Uri uri, @NonNull CropOptions options, final ImageCallback callback) {
        final Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setType("image/*");
        List<ResolveInfo> list = activity.getPackageManager().queryIntentActivities(intent, 0);
        if (list.isEmpty()) {
            Toast.makeText(activity, R.string.no_crop_app, Toast.LENGTH_SHORT).show();
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
                Toast.makeText(activity, "无法截取图片，请检查设备存储", Toast.LENGTH_SHORT).show();
                return;
            }

            final Uri outputUri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".image_provider", cropFile);

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
            });
        }
    }
}
