package com.twiceyuan.imagepickcompat.ext

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract

fun <Input, Output> ActivityResultContract<Input, Output>.launchWithCallback(
    registry: ActivityResultRegistry,
    input: Input,
    callback: (Output) -> Unit
) {
    val tempKey = "Key#${callback.hashCode()}"
    selfReference<ActivityResultLauncher<Input>> {
        registry.register(tempKey, this@launchWithCallback) {
            callback(it)
            self.unregister()
        }.also {
            it.launch(input)
        }
    }
}

data class ActivityResult(val resultCode: Int, val result: Intent?)

class CommonContract(private val intent: Intent) : ActivityResultContract<Unit, ActivityResult>() {
    override fun createIntent(context: Context, input: Unit): Intent {
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): ActivityResult {
        return ActivityResult(resultCode, intent)
    }
}

fun Intent.startWithCallback(activity: ComponentActivity, callback: (ActivityResult) -> Unit) {
    CommonContract(this).launchWithCallback(activity.activityResultRegistry, Unit, callback)
}

/**
 * 内部获取自己的引用
 *
 * from https://stackoverflow.com/questions/33898748/how-to-reference-a-lambda-from-inside-it/36279773#36279773
 */
class SelfReference<T>(initializer: SelfReference<T>.() -> T) {
    val self: T by lazy {
        inner ?: error("SelfReference: inner is null")
    }

    private val inner = initializer()
}

inline fun <reified T> selfReference(noinline initializer: SelfReference<T>.() -> T): T {
    return SelfReference(initializer).self
}
