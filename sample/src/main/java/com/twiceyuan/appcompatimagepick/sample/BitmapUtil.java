package com.twiceyuan.appcompatimagepick.sample;

import android.graphics.Bitmap;
import android.graphics.Matrix;

/**
 * Created by twiceYuan on 2017/6/20.
 *
 * 对于要显示的图片进行大于 2048 分辨率过滤
 */
public class BitmapUtil {

    // 保证图片能够显示需要限制分辨率不能大于 2048x2048
    static Bitmap resize(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int largeLength = width > height ? width : height;
        if (largeLength > 2048) {
            float scale = 2048f / largeLength;
            Matrix scaleMatrix = new Matrix();
            scaleMatrix.postScale(scale, scale);
            return Bitmap.createBitmap(bitmap, 0, 0, width, height, scaleMatrix, true);
        } else {
            return bitmap;
        }
    }
}
