package com.vondear.rxtools.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;

import com.vondear.rxtools.R;
import com.vondear.rxtools.RxImageTool;
import com.vondear.rxtools.model.ModelSpider;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jiangzehui on 16/9/25.
 */
public class RxCobwebView extends View {
    private int center;//中心点
    private float one_radius; //外层菱形圆半径
    private float distance;//多边形之间的间距
    private Rect str_rect;//字体矩形
    private List<Paint> levelPaintList;//层级颜色 集合

    private int defalutSize = 300;//默认大小

    private String[] mSpiderNames;
    private float[] mSpiderLevels;  // 等级列表

    private Paint rank_Paint;//各等级进度画笔
    private Paint mSpiderNamePaint;//字体画笔
    private Paint center_paint;//中心线画笔

    private int mSpiderMaxLevel;// 最大等级
    private int mSpiderNumber;//蜘蛛数量

    private List<ModelSpider> mSpiderList = new ArrayList<>();

    private int mSpiderColor;//蛛网内部填充颜色
    private int mSpiderRadiusColor;//蛛网半径颜色

    private int mSpiderLevelColor; // 蛛网等级填充的颜色
    private int mSpiderLevelStrokeColor; // 蛛网等级描边的颜色
    private boolean mSpiderLevelStroke; // 是否使用蛛网等级的描边
    private float mSpiderLevelStrokeWidth; // 蛛网等级描边的宽度

    private int mSpiderNameSize;

    public RxCobwebView(Context context) {
        this(context, null);
    }

    public RxCobwebView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RxCobwebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.RxCobwebView);//获得这个控件对应的属性。

        mSpiderColor = a.getColor(R.styleable.RxCobwebView_spiderColor, getResources().getColor(R.color.teal));//蛛网内部颜色
        mSpiderRadiusColor = a.getColor(R.styleable.RxCobwebView_spiderRadiusColor, Color.WHITE);//蛛网半径颜色
        mSpiderLevelStrokeColor = a.getColor(R.styleable.RxCobwebView_spiderLevelColor, getResources().getColor(R.color.custom_progress_orange_progress));//蛛网半径颜色
        mSpiderLevelColor = RxImageTool.changeColorAlpha(mSpiderLevelStrokeColor, (255 / 2));
        mSpiderLevelStroke = a.getBoolean(R.styleable.RxCobwebView_spiderLevelStroke, true);
        mSpiderLevelStrokeWidth = a.getFloat(R.styleable.RxCobwebView_spiderLevelStrokeWidth, 3f);
        mSpiderMaxLevel = a.getInteger(R.styleable.RxCobwebView_spiderMaxLevel, 4);
        mSpiderNameSize = a.getDimensionPixelSize(R.styleable.RxCobwebView_spiderNameSize, dp_px(16));//标题字体大小

        defalutSize = dp_px(defalutSize);

        mSpiderNames = new String[]{"金钱", "能力", "美貌", "智慧", "交际", "口才"};
        mSpiderLevels = new float[]{1, 1, 1, 1, 1, 1};
        mSpiderList.clear();
        for (int position = 0; position < mSpiderNames.length; position++) {
            mSpiderList.add(new ModelSpider(mSpiderNames[position], mSpiderLevels[position]));
        }
        mSpiderNumber = mSpiderList.size();

        //初始化字体画笔
        mSpiderNamePaint = new Paint();
        mSpiderNamePaint.setAntiAlias(true);
        mSpiderNamePaint.setColor(Color.BLACK);
        mSpiderNamePaint.setTextSize(mSpiderNameSize);
        str_rect = new Rect();
        mSpiderNamePaint.getTextBounds(mSpiderList.get(0).getSpiderName(), 0, mSpiderList.get(0).getSpiderName().length(), str_rect);

        //初始化各等级进度画笔
        rank_Paint = new Paint();
        rank_Paint.setAntiAlias(true);
        rank_Paint.setColor(Color.RED);
        rank_Paint.setStrokeWidth(8);
        rank_Paint.setStyle(Paint.Style.STROKE);//设置空心

        initLevelPoints();

        //初始化 蛛网半径画笔
        center_paint = new Paint();
        center_paint.setAntiAlias(true);
        center_paint.setColor(mSpiderRadiusColor);
    }

    private void initLevelPoints() {
        levelPaintList = new ArrayList<>();

        //初始化 N 层多边形画笔
        for (int i = mSpiderMaxLevel; i > 0; i--) {
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            int scale = mSpiderMaxLevel * 10 / 11;
            if (scale < 1) {
                scale = 1;
            }
            paint.setColor(RxImageTool.changeColorAlpha(mSpiderColor, (255 / (mSpiderMaxLevel + 1) * (mSpiderMaxLevel - i - 1) + 255 / scale) % 255));
            paint.setStyle(Paint.Style.FILL);//设置实心
            levelPaintList.add(paint);
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        drawSpiderName(canvas);
        for (int position = 0; position < mSpiderMaxLevel; position++) {
            drawCobweb(canvas, position);
        }
        drawSpiderRadiusLine(canvas);
        drawSpiderLevel(canvas);
    }


    /**
     * 绘制等级进度
     */
    private void drawSpiderLevel(Canvas canvas) {
        Path path = new Path();

        float nextAngle;
        float nextRadians;
        float nextPointX;
        float nextPointY;
        float currentRadius;
        float averageAngle = 360 / mSpiderNumber;
        float offsetAngle = averageAngle > 0 && mSpiderNumber % 2 == 0 ? averageAngle / 2 : 0;
        for (int position = 0; position < mSpiderNumber; position++) {
            float scale = (mSpiderList.get(position).getSpiderLevel() / mSpiderMaxLevel);
            if (scale >= 1) {
                scale = 1;
            }
            currentRadius = scale * one_radius;
            nextAngle = offsetAngle + (position * averageAngle);
            nextRadians = (float) Math.toRadians(nextAngle);
            nextPointX = (float) (center + Math.sin(nextRadians) * currentRadius);
            nextPointY = (float) (center - Math.cos(nextRadians) * currentRadius);

            if (position == 0) {
                path.moveTo(nextPointX, nextPointY);
            } else {
                path.lineTo(nextPointX, nextPointY);
            }
        }

        Paint scorePaint = new Paint();
        scorePaint.setColor(mSpiderLevelColor);
        scorePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        scorePaint.setAntiAlias(true);

        path.close();
        canvas.drawPath(path, scorePaint);

        Paint scoreStrokePaint = null;

        // 绘制描边
        if (mSpiderLevelStroke) {
            if (scoreStrokePaint == null) {
                scoreStrokePaint = new Paint();
                scoreStrokePaint.setColor(mSpiderLevelStrokeColor);
                scoreStrokePaint.setStyle(Paint.Style.STROKE);
                scoreStrokePaint.setAntiAlias(true);
                if (mSpiderLevelStrokeWidth > 0) {
                    scoreStrokePaint.setStrokeWidth(mSpiderLevelStrokeWidth);
                }
            }
            canvas.drawPath(path, scoreStrokePaint);
        }
    }


    /**
     * 绘制字体
     *
     * @param canvas
     */
    private void drawSpiderName(Canvas canvas) {
        float nextAngle;
        float nextRadians;
        float nextPointX;
        float nextPointY;
        float currentRadius;
        float averageAngle = 360 / mSpiderNumber;
        float offsetAngle = averageAngle > 0 && mSpiderNumber % 2 == 0 ? averageAngle / 2 : 0;
        for (int position = 0; position < mSpiderNumber; position++) {
            currentRadius = (float) (getPaddingTop() + str_rect.height()) + one_radius;
            nextAngle = offsetAngle + (position * averageAngle);
            nextRadians = (float) Math.toRadians(nextAngle);
            nextPointX = (float) (center + Math.sin(nextRadians) * currentRadius - str_rect.width() / 2);
            nextPointY = (float) (center - Math.cos(nextRadians) * currentRadius + str_rect.height() / 2);

            canvas.drawText(mSpiderList.get(position).getSpiderName(),
                    nextPointX,
                    nextPointY,
                    mSpiderNamePaint);
        }
    }

    /**
     * //绘制层级蛛网
     *
     * @param canvas
     */
    private void drawCobweb(Canvas canvas, int index) {
        Path path = new Path();

        float nextAngle;
        float nextRadians;
        float nextPointX;
        float nextPointY;
        float currentRadius;
        float averageAngle = 360 / mSpiderNumber;
        float offsetAngle = averageAngle > 0 && mSpiderNumber % 2 == 0 ? averageAngle / 2 : 0;
        for (int position = 0; position < mSpiderNumber; position++) {
            currentRadius = (index + 1) * one_radius / mSpiderMaxLevel;
            nextAngle = offsetAngle + (position * averageAngle);
            nextRadians = (float) Math.toRadians(nextAngle);
            nextPointX = (float) (center + Math.sin(nextRadians) * currentRadius);
            nextPointY = (float) (center - Math.cos(nextRadians) * currentRadius);

            if (position == 0) {
                path.moveTo(nextPointX, nextPointY);
            } else {
                path.lineTo(nextPointX, nextPointY);
            }
        }

        path.close();
        canvas.drawPath(path, levelPaintList.get(mSpiderMaxLevel - index - 1));

        Paint scoreStrokePaint = null;

        // 绘制描边
        if (mSpiderLevelStroke) {
            if (scoreStrokePaint == null) {
                scoreStrokePaint = new Paint();
                scoreStrokePaint.setColor(RxImageTool.changeColorAlpha(levelPaintList.get(mSpiderMaxLevel - 1).getColor(), 50));
                scoreStrokePaint.setStyle(Paint.Style.STROKE);
                scoreStrokePaint.setAntiAlias(true);
                if (mSpiderLevelStrokeWidth > 0) {
                    scoreStrokePaint.setStrokeWidth(mSpiderLevelStrokeWidth);
                }
            }
            canvas.drawPath(path, scoreStrokePaint);
        }
    }

    /**
     * 绘制连接中心的线
     *
     * @param canvas Canvas
     */
    private void drawSpiderRadiusLine(Canvas canvas) {
        float nextAngle;
        float nextRadians;
        float nextPointX;
        float nextPointY;
        float averageAngle = 360 / mSpiderNumber;
        float offsetAngle = averageAngle > 0 && mSpiderNumber % 2 == 0 ? averageAngle / 2 : 0;
        for (int position = 0; position < mSpiderNumber; position++) {
            nextAngle = offsetAngle + (position * averageAngle);
            nextRadians = (float) Math.toRadians(nextAngle);
            nextPointX = (float) (center + Math.sin(nextRadians) * one_radius);
            nextPointY = (float) (center - Math.cos(nextRadians) * one_radius);

            canvas.drawLine(center, center, nextPointX, nextPointY, center_paint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int wMode = MeasureSpec.getMode(widthMeasureSpec);
        int wSize = MeasureSpec.getSize(widthMeasureSpec);
        int hMode = MeasureSpec.getMode(heightMeasureSpec);
        int hSize = MeasureSpec.getSize(heightMeasureSpec);
        int width, height;
        if (wMode == MeasureSpec.EXACTLY) {
            width = wSize;
        } else {
            width = Math.min(wSize, defalutSize);
        }

        if (hMode == MeasureSpec.EXACTLY) {
            height = hSize;
        } else {
            height = Math.min(hSize, defalutSize);
        }
        center = width / 2;//中心点
        one_radius = center - getPaddingTop() - 2 * str_rect.height();
        setMeasuredDimension(width, height);


    }

    public int getSpiderMaxLevel() {
        return mSpiderMaxLevel;
    }

    public void setSpiderMaxLevel(int spiderMaxLevel) {
        mSpiderMaxLevel = spiderMaxLevel;
        initLevelPoints();
        invalidate();
    }

    public List<ModelSpider> getSpiderList() {
        return mSpiderList;
    }

    public void setSpiderList(@NonNull List<ModelSpider> spiderList) {
        mSpiderList = spiderList;
        mSpiderNumber = mSpiderList.size();
        invalidate();
    }

    public int getSpiderColor() {
        return mSpiderColor;
    }

    public void setSpiderColor(int spiderColor) {
        mSpiderColor = spiderColor;
        invalidate();
    }

    public int getSpiderRadiusColor() {
        return mSpiderRadiusColor;
    }

    public void setSpiderRadiusColor(int spiderRadiusColor) {
        mSpiderRadiusColor = spiderRadiusColor;
        invalidate();
    }

    public int getSpiderLevelColor() {
        return mSpiderLevelColor;
    }

    public void setSpiderLevelColor(int spiderLevelColor) {
        mSpiderLevelColor = spiderLevelColor;
        invalidate();
    }

    public boolean isSpiderLevelStroke() {
        return mSpiderLevelStroke;
    }

    public void setSpiderLevelStroke(boolean spiderLevelStroke) {
        mSpiderLevelStroke = spiderLevelStroke;
        invalidate();
    }

    public float getSpiderLevelStrokeWidth() {
        return mSpiderLevelStrokeWidth;
    }

    public void setSpiderLevelStrokeWidth(float spiderLevelStrokeWidth) {
        mSpiderLevelStrokeWidth = spiderLevelStrokeWidth;
        invalidate();
    }

    public int getSpiderNameSize() {
        return mSpiderNameSize;
    }

    public void setSpiderNameSize(int spiderNameSize) {
        mSpiderNameSize = spiderNameSize;
        invalidate();
    }

    /**
     * dp转px
     *
     * @param values
     * @return
     */
    public int dp_px(int values) {

        float density = getResources().getDisplayMetrics().density;
        return (int) (values * density + 0.5f);
    }

}
