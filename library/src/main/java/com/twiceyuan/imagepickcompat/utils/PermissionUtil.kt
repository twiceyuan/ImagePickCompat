package com.twiceyuan.imagepickcompat.utils

import android.content.Context
import android.content.pm.ResolveInfo
import android.content.Intent
import android.net.Uri

/**
 * Created by twiceYuan on 2017/5/23.
 *
 * 权限工具
 */
object PermissionUtil {
    @JvmStatic
    fun grantUriPermission(context: Context, resInfoList: List<ResolveInfo>, uri: Uri?) {
        for (resolveInfo in resInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            context.grantUriPermission(
                packageName,
                uri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }
}
