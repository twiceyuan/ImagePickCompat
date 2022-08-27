package com.twiceyuan.imagepickcompat.utils;

import android.content.Context;
import android.net.Uri;
import androidx.core.content.FileProvider;

import com.twiceyuan.imagepickcompat.Constants;

import java.io.File;

/**
 * Created by twiceYuan on 2017/6/20.
 *
 * File Provider util
 */
public class FileProviderUtil {

    public static Uri getUriByFileProvider(Context context, File file) {
        return FileProvider.getUriForFile(context, context.getPackageName() + Constants.FILE_PROVIDER_NAME, file);
    }
}
