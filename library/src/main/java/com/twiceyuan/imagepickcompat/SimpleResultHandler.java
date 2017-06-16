package com.twiceyuan.imagepickcompat;

import android.app.Activity;
import android.content.Intent;

/**
 * Created by twiceYuan on 2017/6/16.
 *
 * 一般情况使用的 ResultHandler
 */
public abstract class SimpleResultHandler implements ResultHandler {

    private final int mHandleRequestCode;

    public SimpleResultHandler(int handleRequestCode) {
        this.mHandleRequestCode = handleRequestCode;
    }

    @Override
    public boolean handleResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == mHandleRequestCode) {
            if (resultCode == Activity.RESULT_OK) {
                handle(intent);
            } else {
                onCancel();
            }
            return true;
        } else {
            return false;
        }
    }

    abstract void handle(Intent data);

    void onCancel() {
        // Nothing to do by default, override if need.
    }
}
