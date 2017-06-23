package com.twiceyuan.appcompatimagepick.sample;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;

import com.twiceyuan.appcompatimagepick.R;
import com.twiceyuan.imagepickcompat.ImagePick;

import java.io.IOException;
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
                ImagePick.takePhoto(this, this::setImage);
            }
            if (which == 1) {
                ImagePick.pickGallery(this, this::setImage);
            }
        }).show());

        mBtnPickCrop.setOnClickListener(v -> new AlertDialog.Builder(this).setAdapter(new ArrayAdapter<>(
                this,
                R.layout.simple_list_item,
                Arrays.asList("从相机选择并裁剪", "从相册选择并裁剪")
        ), (dialog, which) -> {
            if (which == 0) {
                ImagePick.takePhoto(this, imageUri ->
                        ImagePick.crop(this, imageUri, this::setImage));
            }
            if (which == 1) {
                ImagePick.pickGallery(this, imageUri ->
                        ImagePick.crop(this, imageUri, this::setImage));
            }
        }).show());
    }

    private void setImage(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            bitmap = BitmapUtil.resize(bitmap);
            mImgPickResult.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (ImagePick.handleResult(this, requestCode, resultCode, data)) {
            //noinspection UnnecessaryReturnStatement
            return;
        }

        // other activity result
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        ImagePick.clearImageDir(this);
    }

    private void initView() {
        mImgPickResult = (ImageView) findViewById(R.id.img_pick_result);
        mBtnPick = (Button) findViewById(R.id.btn_pick);
        mBtnPickCrop = (Button) findViewById(R.id.btn_pick_crop);
    }
}
