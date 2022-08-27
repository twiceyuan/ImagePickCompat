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
object FileProviderUtil {
    @JvmStatic
    fun getUriByFileProvider(context: Context, file: File?): Uri {
        return FileProvider.getUriForFile(
            context,
            context.packageName + Constants.FILE_PROVIDER_NAME,
            file!!
        )
    }
}
