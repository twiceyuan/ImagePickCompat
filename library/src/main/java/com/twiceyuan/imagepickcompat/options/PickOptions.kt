package com.twiceyuan.imagepickcompat.options

/**
 * Created by twiceYuan on 2017/6/20.
 *
 *
 * 选择图片选项
 */
data class PickOptions(
    var pickAction: (() -> Unit)? = null,
    var cancelAction: (() -> Unit)? = null,
    var noGalleryCallback: (() -> Unit)? = null,
)
