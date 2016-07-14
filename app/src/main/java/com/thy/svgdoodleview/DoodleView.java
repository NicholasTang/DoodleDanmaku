package com.thy.svgdoodleview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.thy.svgdoodleview.Utils.UnitUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 涂鸦弹幕绘画层
 * Created by TangYan on 2016/2/1.
 */
public class DoodleView extends View implements IGraffiti{

    private static final String SVG_PATH = "<path d=\"";
    private static final String SVG_STYLE = "\" style=\"";
    private static final String SVG_PATH_END = "\"/>";
    private static final String SVG_END = "</svg>";

    private Context mContext;
    private Paint mPaint = new Paint();
    private Path mMainPath = new Path();
    private Path mCurrentPath;
    private StringBuilder mMainPathString;
    private StringBuilder mCurrentPathString;
    private float mX,mY;
    private String mColorString = "#FF0000"; // 画笔颜色
    private float mPaintWidth = 10; // 画笔粗细(单位：像素)
    private boolean hasDrawSomething; // 判断此次点击是否绘制了图像.
    private boolean moveOutOfCanvas; // 绘画时手指划出了画板范围

    private int mCanvasWidth, mCanvasHeight; // 画板的尺寸

    private List<Path> mPathList = new ArrayList<>();
    private List<StringBuilder> mPathStringList = new ArrayList<>();


    public DoodleView(Context context) {
        super(context);
        init(context);
    }

    public DoodleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.parseColor(mColorString));
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(UnitUtil.toDp(mContext, mPaintWidth));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawPath(mMainPath, mPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                onTouchDown(event);
                return true;
            case MotionEvent.ACTION_MOVE:
                onTouchMove(event);
                break;
            case MotionEvent.ACTION_UP:
                onTouchUp(event);
                break;
            default:
                return false;
        }
        postInvalidate();
        return false;
    }

    @Override
    public void onTouchDown(MotionEvent event) {
        mX = event.getX();
        mY = event.getY();
        startPath();
    }

    private void startPath() {
        moveOutOfCanvas = false;
        mCurrentPath = new Path();
        mPathList.add(mCurrentPath);
        mCurrentPath.moveTo(mX, mY);
        mMainPath.moveTo(mX,mY);
        mCurrentPathString = new StringBuilder();
        mPathStringList.add(mCurrentPathString);
        mCurrentPathString.append("M" + (int) mX + " " + (int) mY);
    }

    @Override
    public void onTouchMove(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        if (x < 0 || x > mCanvasWidth || y < 0 || y > mCanvasHeight) {
            moveOutOfCanvas = true;
            return;
        }
        if (moveOutOfCanvas) {
            mX = x;
            mY = y;
            startPath();
        }

        float cX = mX;
        float cY = mY;

        float dx = Math.abs(x - cX);
        float dy = Math.abs(y - cY);

        if (dx >= 3 || dy >= 3) {
            float endX = (x + cX) / 2;
            float endY = (y + cY) / 2;
            mCurrentPath.quadTo(cX, cY, endX, endY);
            mMainPath.quadTo(cX, cY, endX, endY);
            mCurrentPathString.append(" Q"+(int)cX+" "+(int)cY+" "+(int)endX+" "+(int)endY);
            mX = x;
            mY = y;
            hasDrawSomething = true;
        }
    }

    @Override
    public void onTouchUp(MotionEvent event) {
        if (!hasDrawSomething) {
            // 如果点击屏幕但未画下任何东西，移除此次点击记录
            if (mPathList.size() > 0 && mPathStringList.size() > 0) {
                mPathList.remove(mPathList.size()-1);
                mPathStringList.remove(mPathStringList.size()-1);
            }
        }
        hasDrawSomething = false;
    }

    @Override
    public void changeColor(String colorString) {
        mColorString = colorString;
        mPaint.setColor(Color.parseColor(mColorString));
    }

    @Override
    public void changeWidth(float width) {
        mPaintWidth = width;
        mPaint.setStrokeWidth(width);
    }

    @Override
    public void revoke() {
        if (mPathList.size() > 0 && mPathStringList.size() > 0) {
            mPathList.remove(mPathList.size()-1);
            mPathStringList.remove(mPathStringList.size() - 1);
            mMainPath.reset();
            for (Path path : mPathList) {
                mMainPath.addPath(path);
            }
            postInvalidate();
        }
    }

    @Override
    public void clearAll() {
        mMainPath.reset();
        if (mPathList.size() > 0) {
            mPathList.clear();
        }
        if (mPathStringList.size() > 0) {
            mPathStringList.clear();
        }
        postInvalidate();
    }

    @Override
    public String getPathString() {
        if (mPathStringList.size() > 0) {
            mMainPathString = new StringBuilder();
            mMainPathString.append("<svg width=\""+getWidth()+"\" height=\""+getHeight()+"\" version=\"1.1\" xmlns=\"http://www.w3.org/2000/svg\">");
            for (StringBuilder pathString : mPathStringList) {
                mMainPathString.append(SVG_PATH);
                mMainPathString.append(pathString);
                mMainPathString.append(SVG_STYLE)
                        .append("fill:transparent;stroke:"+mColorString+";stroke-width:"+UnitUtil.toDp(mContext, mPaintWidth))
                        .append(SVG_PATH_END);
            }
            mMainPathString.append(SVG_END);
            return mMainPathString.toString();
        }
        return "";
    }

    public void changeCanvasSize(int width, int height) {
        mCanvasWidth = width;
        mCanvasHeight = height;
    }
}
