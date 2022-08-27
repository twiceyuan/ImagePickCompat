package com.twiceyuan.imagepickcompat.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.twiceyuan.imagepickcompat.Constants
import java.io.File

/**
 * Created by twiceYuan on 2017/6/20.
 *
 * File Provider util
 */
internal object FileProviderUtil {

    fun getUriByFileProvider(context: Context, file: File): Uri {
        val authority = context.packageName + Constants.FILE_PROVIDER_NAME
        return FileProvider.getUriForFile(context, authority, file)
    }
}
