package com.thy.svgdoodleview;

import android.view.MotionEvent;

/**
 * Created by TangYan on 2016/2/1.
 */
public interface IGraffiti {
    // 点击屏幕开始涂鸦时
    void onTouchDown(MotionEvent event);

    // 手指滑动时
    void onTouchMove(MotionEvent event);

    // 手指抬起时
    void onTouchUp(MotionEvent event);

    // 更换颜色
    void changeColor(String colorString);

    // 更换画笔粗细
    void changeWidth(float size);

    // 撤销上一步
    void revoke();

    // 清空
    void clearAll();

    // 生成svg格式字符串
    String getPathString();
}
