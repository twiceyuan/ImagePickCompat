package com.twiceyuan.imagepickcompat;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import com.twiceyuan.imagepickcompat.callback.ImageCallback;

import java.io.File;
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
                callback.call(data.getData());
            }

            @Override
            void onCancel() {
                Toast.makeText(activity, "选择取消", Toast.LENGTH_SHORT).show();
            }
        });
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
            intent.putExtra("return-data", true);

            if (SDK_INT >= Build.VERSION_CODES.N) {
                PermissionUtil.grantUriPermission(activity, list, intent, uri);
            }

            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
//            ResolveInfo res = list.get(0);
//            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));

            int cropRequestCode = callback.hashCode();

            activity.startActivityForResult(intent, cropRequestCode);

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
