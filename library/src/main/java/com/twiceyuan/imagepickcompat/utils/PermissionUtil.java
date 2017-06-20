package com.twiceyuan.imagepickcompat.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import java.util.List;

/**
 * Created by twiceYuan on 2017/5/23.
 *
 * 权限工具
 */
public class PermissionUtil {

    public static void grantUriPermission(Context context, List<ResolveInfo> resInfoList, Uri uri) {
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }
}
