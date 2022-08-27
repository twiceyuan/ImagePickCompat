package com.twiceyuan.appcompatimagepick.sample

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import com.twiceyuan.appcompatimagepick.R
import com.twiceyuan.imagepickcompat.ImagePick.clearImageDir
import com.twiceyuan.imagepickcompat.ImagePick.crop
import com.twiceyuan.imagepickcompat.ImagePick.handleResult
import com.twiceyuan.imagepickcompat.ImagePick.pickGallery
import com.twiceyuan.imagepickcompat.ImagePick.takePhoto
import java.io.IOException

class MainActivity : Activity() {

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
            AlertDialog.Builder(this).setAdapter(
                ArrayAdapter(
                    this,
                    R.layout.simple_list_item,
                    listOf("从相机选择", "从相册选择")
                )
            ) { _: DialogInterface?, which: Int ->
                if (which == 0) {
                    takePhoto(this) { imageUri: Uri -> setImage(imageUri) }
                }
                if (which == 1) {
                    pickGallery(this) { setImage(it) }
                }
            }.show()
        }
        mBtnPickCrop.setOnClickListener {
            AlertDialog.Builder(this).setAdapter(
                ArrayAdapter(
                    this,
                    R.layout.simple_list_item,
                    listOf("从相机选择并裁剪", "从相册选择并裁剪")
                )
            ) { _: DialogInterface?, which: Int ->
                if (which == 0) {
                    takePhoto(this) { imageUri: Uri? ->
                        crop(this, imageUri) { setImage(it) }
                    }
                }
                if (which == 1) {
                    pickGallery(this) { imageUri: Uri? ->
                        crop(this, imageUri) { setImage(it) }
                    }
                }
            }.show()
        }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (handleResult(this, requestCode, resultCode, data)) {
            return
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        clearImageDir(this)
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
