package com.twiceyuan.imagepickcompat.ext

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.twiceyuan.imagepickcompat.Constants
import java.io.File

internal fun getProviderAuthority(context: Context) =
    context.packageName + Constants.FILE_PROVIDER_NAME

/**
 * 获取内部文件的 Uri
 */
internal fun File.getUriByFileProvider(context: Context): Uri {
    val authority = getProviderAuthority(context)
    return FileProvider.getUriForFile(context, authority, this)
}
