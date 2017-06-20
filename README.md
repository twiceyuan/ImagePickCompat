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

## 兼容

### Nexus 6P（7.1.2）

#### 相册应用

* Google Photos
* 快图

#### 测试用例

- [x] 拍照获取图片
- [x] 选择图片
  - [x] 使用快图选择
  - [x] 使用 Google Photos 选择
- [x] 使用相机拍照后截取图片
  - [x] 使用快图截取
  - [x] 使用 Google Photos 截取
- [ ] 选择图片后截取图片
  - [ ] 使用快图选择
    - [ ] 【崩溃】然后用 Google Photos 截取 
    - [x] 然后用快图截取
  - [x] 使用 Google Photos 选择
    - [x] 然后使用快图截取
    - [x] 然后使用 Google Photos 截取

### Nexus 7 2013 (6.0.1)

#### 相册应用

- Google Photos
- 快图

#### 测试用例

- [x] 拍照获取图片
- [x] 选择图片
  - [x] 使用快图选择
  - [x] 使用 Google Photos 选择
- [x] 使用相机拍照后截取图片
  - [x] 使用快图截取
  - [x] 使用 Google Photos 截取
- [ ] 选择图片后截取图片
  - [ ] 使用快图选择
    - [ ] 【崩溃】然后用 Google Photos 截取 
    - [x] 然后用快图截取
  - [x] 使用 Google Photos 选择
    - [x] 然后使用快图截取
    - [x] 然后使用 Google Photos 截取

### 魅蓝 Note 2 (5.1)

#### 相册应用

- 自带相册

#### 测试用例

- [x] 拍照获取图片
- [x] 选择图片
- [x] 使用相机拍照后截取图片
- [x] 选择图片后截取图片




## 非功能性问题

* [ ] 截图图片之后会出现截取结果在相册中（重现方式：先用快图选择、然后用 Google Photos 截取，之后有很大几率发生崩溃，崩溃后相册会有截取后的结果图片）