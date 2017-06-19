package com.twiceyuan.imagepickcompat;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import com.twiceyuan.imagepickcompat.callback.ImageCallback;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static android.os.Build.VERSION.SDK_INT;

/**
 * Created by twiceYuan on 2017/6/16.
 * <p>
 * Library 功能入口
 */
public class AppCompatImagePick {

    private static boolean LOG = BuildConfig.DEBUG;

    private static Map<Integer, ResultHandler> sHandlerMap = new LinkedHashMap<>();

    public static void pickGallery(final Activity activity, final ImageCallback callback) {

        //选择图库的图片
        final Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        final int pickRequestCode = callback.hashCode();

        try {
            activity.startActivityForResult(intent, pickRequestCode);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, "设备上没有找到图片浏览器应用", Toast.LENGTH_SHORT).show();
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
                    callback.call(getImageContentUri(activity, saveBitmapToFile(activity, getBitmapFromUri(activity, originUri))));
                } else {
                    callback.call(originUri);
                }
            }

            @Override
            void onCancel() {
                Toast.makeText(activity, "选择取消", Toast.LENGTH_SHORT).show();
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
        String filePath = imageFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID},
                MediaStore.Images.Media.DATA + "=? ",
                new String[]{filePath}, null);

        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor
                    .getColumnIndex(MediaStore.MediaColumns._ID));
            cursor.close();
            Uri baseUri = Uri.parse("content://media/external/images/media");
            return Uri.withAppendedPath(baseUri, "" + id);
        } else {
            if (imageFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, filePath);
                return context.getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
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
            Toast.makeText(activity, "本机没有安装相机程序", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(activity, "设备上没有找到图片浏览器应用", Toast.LENGTH_SHORT).show();
                return;
            }

            sHandlerMap.put(activity.hashCode(), new SimpleResultHandler(pickRequestCode) {
                @Override
                void handle(Intent data) {
                    callback.call(photoURI);
                }

                @Override
                void onCancel() {
                    Toast.makeText(activity, "取消拍照", Toast.LENGTH_SHORT).show();
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

    private static void log(String message) {
        if (LOG) {
            Log.e(AppCompatImagePick.class.getSimpleName(), message);
        }
    }

    public static void crop(final Activity activity, Uri uri, final ImageCallback callback) {
        final Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setType("image/*");
        List<ResolveInfo> list = activity.getPackageManager().queryIntentActivities(intent, 0);
        if (list.isEmpty()) {
            Toast.makeText(activity, "没有找到裁剪应用", Toast.LENGTH_SHORT).show();
        } else {
            intent.setData(uri);
            intent.putExtra("outputX", 300);
            intent.putExtra("outputY", 300);
            intent.putExtra("aspectX", 1);
            intent.putExtra("aspectY", 1);
            intent.putExtra("scale", true);
            intent.putExtra("return-data", false);

            if (SDK_INT >= Build.VERSION_CODES.N) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                PermissionUtil.grantUriPermission(activity, list, uri);
            }

//            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            // 没有改行会造成拍照的图片无法进行裁剪
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
//            }
//            ResolveInfo res = list.get(0);
//            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));

            int cropRequestCode = callback.hashCode();

            try {
                activity.startActivityForResult(intent, cropRequestCode);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(activity, "没有找到截图应用", Toast.LENGTH_SHORT).show();
                return;
            }

            sHandlerMap.put(activity.hashCode(), new SimpleResultHandler(cropRequestCode) {
                @Override
                void handle(Intent data) {
                    Bundle extras = intent.getExtras();
                    if (extras != null) {
                        Bitmap bitmap = extras.getParcelable("data");
                        if (bitmap != null) {
                            try {
                                File file = createCropFile(activity);
                                callback.call(Uri.fromFile(file));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return;
                        }
                    }

                    // 在不同机型上测试有时获取不到 intent 中返回的 bitmap，而是一个 uri，这是需要转换为 bitmap
                    callback.call(intent.getData());
                }
            });
        }
    }
}
