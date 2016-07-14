package com.thy.svgdoodleview.SVGUtils;

import android.graphics.Paint;
import android.graphics.Path;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tangyan
 */
public class SVG {

    private List<Path> pathList;

    private List<Integer> colorList;

    private List<Float> strokeWidthList;

    private List<Paint> paintList;

    SVG(List<Path> pathList, List<Integer> colorList, List<Float> strokeWidthList) {
        this.pathList = pathList;
        this.colorList = colorList;
        this.strokeWidthList = strokeWidthList;
        makePaintList();
    }

    private List<Paint> makePaintList() {
        paintList = new ArrayList<>();
        if (pathList != null && colorList != null && strokeWidthList != null) {
            if (pathList.size() == colorList.size() && pathList.size() == strokeWidthList.size()) {
                for (int i = 0; i < pathList.size(); i++) {
                    Paint paint = new Paint();
                    paint.setAntiAlias(true);
                    paint.setColor(colorList.get(i));
                    paint.setStrokeWidth(strokeWidthList.get(i));
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeCap(Paint.Cap.ROUND);
                    paint.setStrokeJoin(Paint.Join.ROUND);
                    paintList.add(paint);
                }
            }
        }
        return paintList;
    }

    public List<Paint> getPaintList() {
        return paintList;
    }

    public List<Path> getPathList() {
        return pathList;
    }
}
