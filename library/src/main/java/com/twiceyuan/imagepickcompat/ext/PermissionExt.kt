package com.twiceyuan.imagepickcompat.ext

import android.content.Context
import android.content.pm.ResolveInfo
import android.content.Intent
import android.net.Uri

/**
 * 给 intent handler 授予内部 uri 的读写权限
 */
internal fun grantUriPermission(context: Context, resInfoList: List<ResolveInfo>, uri: Uri) {
    for (resolveInfo in resInfoList) {
        val packageName = resolveInfo.activityInfo.packageName
        context.grantUriPermission(
            packageName,
            uri,
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }
}
