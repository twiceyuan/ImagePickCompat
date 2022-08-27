package com.twiceyuan.imagepickcompat.options

/**
 * Created by twiceYuan on 2017/6/19.
 *
 *
 * Options about crop action.
 */
data class CropOptions(
    var outputX: Int = 300,
    var outputY: Int = 300,
    var aspectX: Int = 1,
    var aspectY: Int = 1,
    var scale: Boolean = true,
    var noCropCallback: (() -> Unit)? = null,
    var cancelCropCallback: (() -> Unit)? = null,
)
