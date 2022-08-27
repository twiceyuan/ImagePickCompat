package com.twiceyuan.imagepickcompat.result

import android.net.Uri

sealed class TakePhotoResult {

    /**
     * 拍照成功
     */
    class Success(val uri: Uri) : TakePhotoResult()

    /**
     * 拍照取消
     */
    object Cancelled : TakePhotoResult()

    /**
     * 无相机应用
     */
    object NoCamera : TakePhotoResult()

    /**
     * 未知错误
     */
    class Unknown(val throwable: Throwable) : TakePhotoResult()
}
