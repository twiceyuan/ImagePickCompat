package com.twiceyuan.imagepickcompat.options

/**
 * Created by twiceYuan on 2017/6/20.
 *
 * 拍照配置选项
 */
data class TakeCameraOptions(
    var takePhotoAction: (() -> Unit)? = null,
    var cancelAction: (() -> Unit)? = null,
    var noCameraCallback: (() -> Unit)? = null,
)
