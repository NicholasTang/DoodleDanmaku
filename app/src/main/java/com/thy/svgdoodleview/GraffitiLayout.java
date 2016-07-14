package com.thy.svgdoodleview;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.thy.svgdoodleview.Utils.LogHelper;

import java.util.HashMap;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * 涂鸦弹幕绘画主框体
 * Created by TangYan on 2016/2/1.
 */
public class GraffitiLayout extends FrameLayout {

    private static final int MESSAGE_SET_SCREEN_SHOT = 0x0001;

    private Animation animIn;
    private Animation animOut;
    private View showingView;
    private View hidingView;
    private boolean toolsBarIsShowing = true;

    @InjectView(R.id.graffiti_tools_layout)
    RelativeLayout graffitiToolsBar;

    @InjectView(R.id.graffiti_buttom_layout)
    RelativeLayout graffitiButtomBar;

    @InjectView(R.id.doodle_view)
    DoodleView doodleView;

    @InjectView(R.id.screen_shot_view)
    ImageView screenShotView;


    //***********************  onClicks *************************

    @OnClick({R.id.graffiti_tools_up_left, R.id.graffiti_tools_up_right})
    public void onGraffitiUpClick(View v) {
        // TODO 显示工具栏按钮点击事件
        showToolsBar();
    }

    @OnClick({R.id.graffiti_tools_down_left, R.id.graffiti_tools_down_right})
    public void onGraffitiDownClick(View v) {
        // TODO 隐藏工具栏按钮点击事件
        hideToolsBar();
    }

    @OnClick(R.id.graffiti_back_image)
    public void onBackImageClick(View v) {
        // TODO 返回按钮点击事件
    }

    @OnClick(R.id.graffiti_revote_btn)
    public void onRevoteClick(View v) {
        // TODO 撤销按钮点击事件
        doodleView.revoke();
    }

    @OnClick(R.id.graffiti_paint_size_btn)
    public void onChangeSizeClick(View v) {
        // TODO 改变尺寸按钮点击事件
    }

    @OnClick(R.id.graffiti_paint_color_btn)
    public void onChangeColorClick(View v) {
        // TODO 改变颜色按钮点击事件
    }

    @OnClick(R.id.graffiti_clear_btn)
    public void onClearClick(View v) {
        // TODO 清除按钮点击事件
        doodleView.clearAll();
    }

    @OnClick(R.id.graffiti_duration_btn)
    public void onDurationClick(View v) {
        // TODO 设置时长按钮点击事件
    }

    @OnClick(R.id.graffiti_preview_btn)
    public void onPreviewClick(View v) {
        // TODO 预览按钮点击事件
    }

    @OnClick(R.id.graffiti_send_btn)
    public void onSendClick(View v) {
        // TODO 发布按钮点击事件
        LogHelper.warn("xxxxx", "SVG格式测试：" + doodleView.getPathString());
    }


    //***********************  init  *************************

    public GraffitiLayout(Context context) {
        super(context);
        init(context);
    }

    public GraffitiLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        View rootView = LayoutInflater.from(context)
                .inflate(R.layout.widget_player_graffiti_layout, this, true);
        ButterKnife.inject(rootView);
        animIn = AnimationUtils.loadAnimation(context,R.anim.fade_in_up);
        animIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                if (showingView != null && hidingView != null) {
                    showingView.setVisibility(VISIBLE);
                    hidingView.startAnimation(animOut);
                }
            }
            @Override
            public void onAnimationEnd(Animation animation) {}
            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        animOut = AnimationUtils.loadAnimation(context,R.anim.fade_out_down);
        animOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}
            @Override
            public void onAnimationEnd(Animation animation) {
                if (hidingView != null) {
                    hidingView.setVisibility(GONE);
                }
            }
            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        // TODO 测试用
        changeCanvasSize(500, 500);
    }

    private Handler mHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_SET_SCREEN_SHOT:
                    Bitmap screenShot = null;
                    if (msg.obj instanceof Bitmap) {
                        screenShot = (Bitmap) msg.obj;
                    }
                    if (screenShot != null) {
                        setScreenShot(screenShot);
                    }
                    break;
            }
        }
    };

    //************************  methods  ************************

    private void showToolsBar() {
        if (toolsBarIsShowing) return;
        toolsBarIsShowing = true;
        showingView = graffitiToolsBar;
        hidingView = graffitiButtomBar;
        doToggleAnim();
    }

    private void hideToolsBar() {
        if (!toolsBarIsShowing) return;
        toolsBarIsShowing = false;
        showingView = graffitiButtomBar;
        hidingView = graffitiToolsBar;
        doToggleAnim();
    }

    private void doToggleAnim() {
        if (showingView != null) {
            showingView.startAnimation(animIn);
        }
    }

    public void changeCanvasSize(int width, int height) {
        ViewGroup.LayoutParams lp = doodleView.getLayoutParams();
        lp.width = width;
        lp.height = height;
        doodleView.setLayoutParams(lp);
        doodleView.changeCanvasSize(width, height);
        screenShotView.setLayoutParams(lp);
    }

    public void takeScreenShot(final String uri, final long time) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap frameAtTime = null;
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                try {
                    retriever.setDataSource(uri,new HashMap<String, String>());
                    frameAtTime = retriever.getFrameAtTime(time, MediaMetadataRetriever.OPTION_CLOSEST);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                LogHelper.warn("xxxxx", "截图文件：" + frameAtTime);
                if (frameAtTime != null) {
                    Message msg = new Message();
                    msg.what = MESSAGE_SET_SCREEN_SHOT;
                    msg.obj = frameAtTime;
                    mHandler.sendMessage(msg);
                }
            }
        }).start();
    }

    public void setScreenShot(Bitmap screenShot) {
        screenShotView.setImageBitmap(screenShot);
    }

}
