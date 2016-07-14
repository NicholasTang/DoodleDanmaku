package com.thy.svgdoodleview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import com.thy.svgdoodleview.SVGUtils.SVG;
import com.thy.svgdoodleview.SVGUtils.SVGBuilder;
import com.thy.svgdoodleview.Utils.LogHelper;
import com.thy.svgdoodleview.bean.GraffitiDanmaku;

import java.util.ArrayList;
import java.util.List;

/**
 * 涂鸦弹幕显示层
 * Created by TangYan on 2016/2/2.
 */
public class GraffitiDanmakuView extends View {

    private ArrayList<GraffitiDanmaku> graffitiDanmakus;
    private long videoDuration;
    private SVGBuilder builder;

    private ArrayList<GraffitiDanmaku> showingGraffitis;


    public GraffitiDanmakuView(Context context) {
        super(context);
        init(context);
    }

    public GraffitiDanmakuView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        graffitiDanmakus = new ArrayList<>();
        showingGraffitis = new ArrayList<>();
        builder = new SVGBuilder();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (showingGraffitis != null && showingGraffitis.size() > 0) {
            LogHelper.warn("xxxxx", "prepare to draw graffiti");
            for (GraffitiDanmaku graffitiDanmaku : showingGraffitis) {
                LogHelper.warn("xxxxx", "draw graffiti:" + graffitiDanmaku);
                SVG svg = builder.readFromString(graffitiDanmaku.getSvgString()).build(getWidth(),getHeight());
                List<Path> pathList = svg.getPathList();
                List<Paint> paintList = svg.getPaintList();
                if (pathList != null && paintList != null && paintList.size() > 0) {
                    for (int i = 0; i < pathList.size(); i++) {
                        canvas.drawPath(pathList.get(i),paintList.get(i));
                    }
                }
            }
        } else {
            LogHelper.warn("xxxxx", "clear picture");
            canvas.drawColor(Color.TRANSPARENT);
        }
    }

    public void setVideoDuration(long videoDuration) {
        this.videoDuration = videoDuration;
    }

    public void checkGraffitiDanmaku(long videoCurrentPosition) {
        if (videoDuration <= 0) {
            LogHelper.warn("xxxxx", "graffiti duration is zero");
            return;
        }
        for (GraffitiDanmaku gd : graffitiDanmakus) {
            long startTime = gd.getStartTime();
            long endTime = gd.getEndTime();
            if (endTime > videoDuration) {
                endTime = videoDuration;
            }
            if (videoCurrentPosition >= startTime && videoCurrentPosition < endTime) {
                showGraffiti(gd);
            } else {
                hideGraffiti(gd);
            }
        }
    }

    public void addGraffitiDanmaku(GraffitiDanmaku graffitiDanmaku) {
        if (graffitiDanmakus != null && graffitiDanmaku != null) {
            graffitiDanmakus.add(graffitiDanmaku);
        }
    }

    public void removeGraffitiDanmaku(GraffitiDanmaku graffitiDanmaku) {
        if (graffitiDanmakus != null && graffitiDanmaku != null) {
            graffitiDanmakus.remove(graffitiDanmaku);
        }
    }

    private void showGraffiti(GraffitiDanmaku graffitiDanmaku) {
        if (graffitiDanmaku == null) return;
        if (graffitiDanmaku.isShowing()) {
            LogHelper.warn("xxxxx", "graffiti is showing");
            return;
        }
        LogHelper.warn("xxxxx", "prepare to show graffiti:add svg to list");
        showingGraffitis.add(graffitiDanmaku);
        graffitiDanmaku.setShowing(true);
        postInvalidate();
    }

    private void hideGraffiti(GraffitiDanmaku graffitiDanmaku) {
        if (graffitiDanmaku != null && graffitiDanmaku.isShowing()) {
            showingGraffitis.remove(graffitiDanmaku);
            graffitiDanmaku.setShowing(false);
            postInvalidate();
        }
    }
}
