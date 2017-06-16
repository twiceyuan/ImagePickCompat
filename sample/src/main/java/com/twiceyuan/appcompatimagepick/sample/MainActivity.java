package com.twiceyuan.appcompatimagepick.sample;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;

import com.twiceyuan.appcompatimagepick.R;
import com.twiceyuan.imagepickcompat.AppCompatImagePick;

import java.util.Arrays;

public class MainActivity extends Activity {

    private ImageView mImgPickResult;
    private Button    mBtnPick;
    private Button    mBtnPickCrop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

        mBtnPick.setOnClickListener(v -> new AlertDialog.Builder(this).setAdapter(new ArrayAdapter<>(
                this,
                R.layout.simple_list_item,
                Arrays.asList("从相机选择", "从相册选择")
        ), (dialog, which) -> {
            if (which == 0) {
                AppCompatImagePick.pickCamera(this, imageUri -> mImgPickResult.setImageURI(imageUri));
            }
            if (which == 1) {
                AppCompatImagePick.pickGallery(this, imageUri -> mImgPickResult.setImageURI(imageUri));
            }
        }).show());

        mBtnPickCrop.setOnClickListener(v -> new AlertDialog.Builder(this).setAdapter(new ArrayAdapter<>(
                this,
                R.layout.simple_list_item,
                Arrays.asList("从相机选择并裁剪", "从相册选择并裁剪")
        ), (dialog, which) -> {
            if (which == 0) {
                AppCompatImagePick.pickCamera(this, imageUri ->
                        AppCompatImagePick.crop(this, imageUri, croppedUri ->
                                mImgPickResult.setImageURI(imageUri)));
            }
            if (which == 1) {
                AppCompatImagePick.pickGallery(this, imageUri ->
                        AppCompatImagePick.crop(this, imageUri, croppedUri ->
                                mImgPickResult.setImageURI(imageUri)));
            }
        }).show());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (AppCompatImagePick.handleResult(this, requestCode, resultCode, data)) {
            //noinspection UnnecessaryReturnStatement
            return;
        }

        // other activity result
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        AppCompatImagePick.clearImageDir(this);
    }

    private void initView() {
        mImgPickResult = (ImageView) findViewById(R.id.img_pick_result);
        mBtnPick = (Button) findViewById(R.id.btn_pick);
        mBtnPickCrop = (Button) findViewById(R.id.btn_pick_crop);
    }
}
