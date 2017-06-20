package com.twiceyuan.appcompatimagepick.sample;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.twiceyuan.appcompatimagepick.R;
import com.twiceyuan.imagepickcompat.ImagePick;

import java.io.IOException;
import java.util.Arrays;

public class MainActivity extends Activity {

    private static final int REQUEST_PERMISSION_READ_STORAGE = 1002;

    private ImageView mImgPickResult;
    private Button    mBtnPick;
    private Button    mBtnPickCrop;

    private Callback mPermissionGrantedCallback = null;

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
                ImagePick.pickCamera(this, this::setImage);
            }
            if (which == 1) {
                //noinspection MissingPermission
                requestReadStoragePermission(() -> ImagePick.pickGallery(this, this::setImage));
            }
        }).show());

        mBtnPickCrop.setOnClickListener(v -> new AlertDialog.Builder(this).setAdapter(new ArrayAdapter<>(
                this,
                R.layout.simple_list_item,
                Arrays.asList("从相机选择并裁剪", "从相册选择并裁剪")
        ), (dialog, which) -> {
            if (which == 0) {
                ImagePick.pickCamera(this, imageUri ->
                        ImagePick.crop(this, imageUri, this::setImage));
            }
            if (which == 1) {
                //noinspection MissingPermission
                requestReadStoragePermission(() -> ImagePick.pickGallery(this, imageUri ->
                        ImagePick.crop(this, imageUri, this::setImage)));
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

    public void requestReadStoragePermission(Callback callback) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            callback.call();
        } else {
            Toast.makeText(this, "该功能需要权限", Toast.LENGTH_SHORT).show();
            mPermissionGrantedCallback = callback;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION_READ_STORAGE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_READ_STORAGE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mPermissionGrantedCallback != null) {
                    mPermissionGrantedCallback.call();
                    mPermissionGrantedCallback = null;
                }
            }
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

    private interface Callback {
        void call();
    }
}
