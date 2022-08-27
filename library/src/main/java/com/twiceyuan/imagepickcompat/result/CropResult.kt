package com.twiceyuan.imagepickcompat.result

import android.net.Uri

/**
 * 裁剪结果
 */
sealed class CropResult {
    /**
     * 裁剪成功
     */
    class Success(val uri: Uri) : CropResult()

    /**
     * 裁剪取消
     */
    object Cancelled : CropResult()

    /**
     * 设备未安装支持裁剪的 App
     */
    object NoCropHandler : CropResult()

    /**
     * 未知错误
     */
    class Unknown(val throwable: Throwable): CropResult()
}

