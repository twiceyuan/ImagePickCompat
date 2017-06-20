package com.twiceyuan.imagepickcompat.options;

/**
 * Created by twiceYuan on 2017/6/19.
 * <p>
 * Options about crop action.
 */
public class CropOptions {

    public int     outputX;
    public int     outputY;
    public int     aspectX;
    public int     aspectY;
    public boolean scale;

    public CropOptions() {
        outputX = 300;
        outputY = 300;
        aspectX = 1;
        aspectY = 1;
        scale = true;
    }
}
