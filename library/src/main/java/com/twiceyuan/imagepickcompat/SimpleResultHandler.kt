package com.twiceyuan.imagepickcompat

import android.app.Activity
import android.content.Intent

/**
 * Created by twiceYuan on 2017/6/16.
 *
 * 一般情况使用的 ResultHandler
 */
abstract class SimpleResultHandler(private val mHandleRequestCode: Int) : ResultHandler {
    override fun handleResult(requestCode: Int, resultCode: Int, intent: Intent?): Boolean {
        return if (requestCode == mHandleRequestCode) {
            if (resultCode == Activity.RESULT_OK) {
                handle(intent)
            } else {
                onCancel()
            }
            true
        } else {
            false
        }
    }

    abstract fun handle(data: Intent?)

    open fun onCancel() {
        // Nothing to do by default, override if need.
    }
}
