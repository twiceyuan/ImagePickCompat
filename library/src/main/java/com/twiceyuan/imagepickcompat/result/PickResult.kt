package com.twiceyuan.imagepickcompat.result

import android.net.Uri

/**
 * Created by twiceYuan on 2017/6/20.
 *
 * 选择图片结果
 */
sealed class PickResult {

    /**
     * 选择成功
     */
    class Success(val uri: Uri) : PickResult()

    /**
     * 选择取消
     */
    object Cancelled : PickResult()

    /**
     * 当前设备未安装相册（图片选择器）
     */
    object NoGallery : PickResult()

    /**
     * 未知错误
     */
    class Unknown(throwable: Throwable) : PickResult()
}

