package com.thy.svgdoodleview.bean;


import com.alibaba.fastjson.annotation.JSONField;

import java.io.Serializable;

/**
 * Created by TangYan on 2016/2/15.
 */
public class GraffitiDanmaku implements Serializable{
    @JSONField(name = "text")
    private String svgString;
    @JSONField(name = "duration")
    private int duration;
    @JSONField(serialize = false)
    private int startTime;
    @JSONField(serialize = false)
    private int endTime;
    @JSONField(serialize = false)
    private boolean isShowing;

    public GraffitiDanmaku() {
    }

    public GraffitiDanmaku(String svgString, int startTime, int duration) {
        this.svgString = svgString;
        this.startTime = startTime;
        this.duration = duration;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public int getDuration() {
        return duration;
    }

    public String getSvgString() {
        return svgString;
    }

    public int getEndTime() {
        endTime = startTime+duration;
        return endTime;
    }

    public int getStartTime() {
        return startTime;
    }

    public void setShowing(boolean isShowing) {
        this.isShowing = isShowing;
    }

    public boolean isShowing() {
        return isShowing;
    }

    @Override
    public String toString() {
        return "GraffitiDanmaku{" +
                "svgString='" + svgString + '\'' +
                "\nstartTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}
