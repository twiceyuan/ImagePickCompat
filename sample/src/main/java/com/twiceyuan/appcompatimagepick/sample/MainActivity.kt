package com.twiceyuan.appcompatimagepick.sample

import android.app.AlertDialog
import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.twiceyuan.appcompatimagepick.R
import com.twiceyuan.imagepickcompat.ImagePick
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var mImgPickResult: ImageView
    private lateinit var mBtnPick: Button
    private lateinit var mBtnPickCrop: Button

    private fun initView() {
        mImgPickResult = findViewById(R.id.img_pick_result)
        mBtnPick = findViewById(R.id.btn_pick)
        mBtnPickCrop = findViewById(R.id.btn_pick_crop)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
        mBtnPick.setOnClickListener {
            showActionsMenu(mapOf(
                "从相机选择" to { ImagePick.takePhoto(this) { setImage(it) } },
                "从相册选择" to { ImagePick.pickGallery(this) { setImage(it) } }
            ))
        }
        mBtnPickCrop.setOnClickListener {
            fun cropAndShowImage(uri: Uri) {
                ImagePick.crop(this, uri) { setImage(it) }
            }
            showActionsMenu(
                mapOf(
                    "从相机选择并裁剪" to { ImagePick.takePhoto(this) { cropAndShowImage(it) } },
                    "从相册选择并裁剪" to { ImagePick.pickGallery(this) { cropAndShowImage(it) } },
                )
            )
        }
    }

    private fun showActionsMenu(actions: Map<String, () -> Unit>) {
        AlertDialog.Builder(this).setItems(
            actions.keys.toTypedArray()
        ) { _: DialogInterface?, which: Int ->
            actions.values.toList()[which]()
        }.show()
    }

    private fun setImage(imageUri: Uri) {
        try {
            @Suppress("DEPRECATION")
            var bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
            bitmap = BitmapUtil.resize(bitmap)
            mImgPickResult.setImageBitmap(bitmap)
        } catch (e: IOException) {
            Log.e(TAG, "setImage", e)
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
