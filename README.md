# AppCompat ImagePick

> 状态：开发中

调用外部图片应用，提供对外统一的图片选择、裁剪接口。

从 4.1 到 7.1 图片选择和裁剪过程有很多需要适配的地方，网上的很多方法都有所缺漏，并且大多数没有系统遵循规范，导致 Google Photos 之类的应用不能被调用选择或者裁剪。

本库希望实现一个无依赖、界面无关、仅提供选择、裁剪接口的图片工具，来简化这部分需求时的适配工作。

## 使用

```java
// 拍照
AppCompatImagePick.pickCamera(this, imageUri -> mImgPickResult.setImageURI(imageUri));

// 选择图片
AppCompatImagePick.pickGallery(this, imageUri -> mImgPickResult.setImageURI(imageUri));

// 拍照并且裁剪
AppCompatImagePick.pickCamera(this, imageUri ->
    AppCompatImagePick.crop(this, imageUri, croppedUri ->
            mImgPickResult.setImageURI(imageUri)));
```