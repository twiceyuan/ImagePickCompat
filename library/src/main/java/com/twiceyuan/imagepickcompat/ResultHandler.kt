package com.twiceyuan.imagepickcompat

import android.content.Intent

/**
 * Created by twiceYuan on 2017/6/16.
 *
 * 选择结果处理
 */
interface ResultHandler {
    /**
     * 处理 activity result
     *
     * @param intent activity Result
     * @return 是否处理
     */
    fun handleResult(requestCode: Int, resultCode: Int, intent: Intent?): Boolean
}
