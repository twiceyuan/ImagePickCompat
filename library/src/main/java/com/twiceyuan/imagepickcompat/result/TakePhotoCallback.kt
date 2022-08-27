package com.twiceyuan.imagepickcompat.result

import android.net.Uri

/**
 * 拍照回调
 */
fun interface TakePhotoCallback {

    fun onTake(result: TakePhotoResult)
}

fun TakePhotoCallback.onTakeSuccess(uri: Uri) = onTake(TakePhotoResult.Success(uri))

fun TakePhotoCallback.onTakeCancel() = onTake(TakePhotoResult.Cancelled)
