package com.twiceyuan.appcompatimagepick.sample

import android.graphics.Bitmap
import android.graphics.Matrix
import kotlin.math.max

/**
 * Created by twiceYuan on 2017/6/20.
 *
 * 对于要显示的图片进行大于 2048 分辨率过滤
 */
object BitmapUtil {

    private const val MAX_SIZE = 2048f

    // 保证图片能够显示需要限制分辨率不能大于 2048x2048
    fun resize(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val largeLength = max(width, height)
        return if (largeLength > MAX_SIZE) {
            val scale = MAX_SIZE / largeLength
            val scaleMatrix = Matrix()
            scaleMatrix.postScale(scale, scale)
            Bitmap.createBitmap(bitmap, 0, 0, width, height, scaleMatrix, true)
        } else {
            bitmap
        }
    }
}
