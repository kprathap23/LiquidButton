package com.gospelware.liquildbutton;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.Transformation;

/**
 * Created by ricogao on 06/05/2016.
 */
public class LiquidButton extends View {

    private Paint pourPaint, liquidPaint, tickPaint;
    private int centreX, centerY, frameTop, left, top, radius, bottom;
    private int bounceY;
    private int pourHeight;

    private static final int POUR_STROKE_WIDTH = 30;
    private static final int TICK_STROKE_WIDTH = 15;

//    private float mInterpolatedTime;

    private PointF pourTop, pourBottom, tickPoint1, tickPoint2, tickPoint3, tickControl2, tickControl3;

    private float liquidLevel;
    private Path circlePath;
    private Path wavePath, tickPath;
    private Animation liquidAnimation, bounceAnimation, tickAnimation;
    private AnimationSet set;


    private int liquidColor;

    //control shift-x on sin wave
    private int fai = 0;

    private static final int FAI_FACTOR = 5;
    private static final int AMPLITUDE = 50;
    private static final float ANGLE_VELOCITY = 0.5f;

    private final float TOUCH_BASE = 0.1f;
    private final float FINISH_POUR = 0.9f;


    public LiquidButton(Context context) {
        super(context);
    }

    public LiquidButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LiquidButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    class LiquidAnimation extends Animation {
        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);

            computeColor(interpolatedTime);
            computePourStart(interpolatedTime);
            computeLiquid(interpolatedTime);

            invalidate();
        }
    }


    class BounceAnimation extends Animation {
        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
            computePourFinish(interpolatedTime);
            computeBounceBall(interpolatedTime);
            invalidate();
        }
    }

    class TickAnimation extends Animation {
        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
            computeTick(interpolatedTime);
            invalidate();
        }
    }


    protected void init() {
        pourPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pourPaint.setDither(true);
        pourPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        pourPaint.setStrokeWidth(POUR_STROKE_WIDTH);

        liquidPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        liquidPaint.setDither(true);
        liquidPaint.setStyle(Paint.Style.FILL);

        tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickPaint.setDither(true);
        tickPaint.setColor(Color.WHITE);
        tickPaint.setStrokeCap(Paint.Cap.ROUND);
        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeWidth(TICK_STROKE_WIDTH);

        pourTop = new PointF();
        pourBottom = new PointF();

        tickPoint1 = new PointF();
        tickPoint2 = new PointF();
        tickPoint3 = new PointF();

        circlePath = new Path();
        wavePath = new Path();
        tickPath = new Path();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
//
//        if (!liquidAnimation.hasEnded()) {
//            drawLiquid(canvas);
//            drawPour(canvas);
//        } else {
//            drawBounceBall(canvas);
//            if (!bounceAnimation.hasEnded()) {
//                drawPour(canvas);
//            } else {
//                drawTick(canvas);
//            }
//        }

        if (set == null) {
            return;
        }

        drawPour(canvas);

        if (liquidAnimation.hasEnded()) {
            drawBounceBall(canvas);
            if (bounceAnimation.hasEnded()) {
                drawTick(canvas);
            }
        } else {
            drawLiquid(canvas);
        }


    }

    protected void computeColor(float interpolatedTime) {
        int blue = 24;
        int red = (interpolatedTime <= FINISH_POUR) ? 255 : Math.round(255 * (1 - (interpolatedTime - FINISH_POUR) / TOUCH_BASE));
        int green = (interpolatedTime >= FINISH_POUR) ? 255 : Math.round(255 * interpolatedTime / FINISH_POUR);
        liquidColor = Color.rgb(red, green, blue);
    }

    protected void computePourStart(float interpolatedTime) {
        //0.0~0.1 drop to bottom, 0.9~1.0 on top
        pourBottom.y = (interpolatedTime < TOUCH_BASE) ? interpolatedTime / TOUCH_BASE * pourHeight + frameTop : bottom;

    }

    protected void computePourFinish(float interpolatedTime) {
        pourTop.y = frameTop + (2 * radius * interpolatedTime);
    }

    protected void drawPour(Canvas canvas) {
        pourPaint.setColor(liquidColor);
        canvas.drawLine(centreX, pourTop.y, centreX, pourBottom.y, pourPaint);
    }

    protected void computeLiquid(float interpolatedTime) {

        liquidLevel = (interpolatedTime < TOUCH_BASE) ? bottom : bottom - (2 * radius * (interpolatedTime - TOUCH_BASE) / FINISH_POUR);

        // scroll x by the fai factor
        if (interpolatedTime >= TOUCH_BASE) {
            //slowly reduce the wave frequency
            fai += FAI_FACTOR * (1.4f - interpolatedTime);
            if (fai == 360) {
                fai = 0;
            }
        }
        //clear the path for next render
        wavePath.reset();
        //slowly reduce the amplitude when filling comes to end
        float a = (interpolatedTime <= FINISH_POUR) ? AMPLITUDE : AMPLITUDE * (1.4f - interpolatedTime);

        for (int i = 0; i < 2 * radius; i++) {
            int dx = left + i;

            // y = a * sin( w * x + fai ) + h
            int dy = (int) (a * Math.sin((i * ANGLE_VELOCITY + fai) * Math.PI / 180) + liquidLevel);

            if (i == 0) {
                wavePath.moveTo(dx, dy);
            }

            wavePath.quadTo(dx, dy, dx + 1, dy);
        }

        wavePath.lineTo(centreX + radius, bottom);
        wavePath.lineTo(left, bottom);

        wavePath.close();
    }


    protected void drawLiquid(Canvas canvas) {

        //save the canvas status
        canvas.save();
        //clip the canvas to circle
        liquidPaint.setColor(liquidColor);
        canvas.clipPath(circlePath);
        canvas.drawPath(wavePath, liquidPaint);
        //restore the canvas status~
        canvas.restore();

    }


    protected void computeBounceBall(float interpolatedTime) {
        bounceY = (interpolatedTime < 1f) ? centerY : Math.round((interpolatedTime - 1) * radius) + centerY;
    }

    protected void drawBounceBall(Canvas canvas) {
        canvas.drawCircle(centreX, bounceY, radius, liquidPaint);
    }


    protected void computeTick(float interpolatedTime) {
        if (interpolatedTime <= 0.5f) {

            float dt = interpolatedTime / 0.5f;
            float dx = (tickPoint2.x - tickPoint1.x) * dt;
            float dy = (tickPoint2.y - tickPoint1.y) * dt;

            if (tickControl2 == null) {
                tickControl2 = new PointF();
            }

            tickControl2.x = tickPoint1.x + dx;
            tickControl2.y = tickPoint1.y + dy;
        } else {

            float dt = (interpolatedTime - 0.5f) / 0.5f;
            float dx = (tickPoint3.x - tickPoint2.x) * dt;
            float dy = (tickPoint3.y - tickPoint2.y) * dt;

            if (tickControl3 == null) {
                tickControl3 = new PointF();
            }

            tickControl3.x = tickPoint2.x + dx;
            tickControl3.y = tickPoint2.y + dy;
        }
    }

    protected void drawTick(Canvas canvas) {
        tickPath.reset();

        tickPath.moveTo(tickPoint1.x, tickPoint1.y);

        if (tickControl2 != null) {
            tickPath.lineTo(tickControl2.x, tickControl2.y);
        }

        if (tickControl3 != null) {
            tickPath.lineTo(tickControl3.x, tickControl3.y);
        }

        canvas.drawPath(tickPath, tickPaint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int width = getWidth();
        int height = getHeight();

        centreX = width / 2;
        centerY = height / 2;
        bounceY = centerY;

        radius = width / 8;

        frameTop = centerY - 3 * radius;
        left = centreX - radius;
        top = centerY - radius;
        bottom = centerY + radius;

        pourHeight = 4 * radius;

        tickPoint1.x = left + (0.29f * 2 * radius);
        tickPoint1.y = top + (0.525f * 2 * radius);

        tickPoint2.x = left + (0.445f * 2 * radius);
        tickPoint2.y = top + (0.675f * 2 * radius);

        tickPoint3.x = left + (0.74f * 2 * radius);
        tickPoint3.y = top + (0.45f * 2 * radius);

        circlePath.addCircle(centreX, centerY, radius, Path.Direction.CW);
    }

    public void startPour() {

        fai = 0;
        tickControl2 = null;
        tickControl3 = null;


        if (set == null) {
            set = new AnimationSet(false);
            liquidAnimation = new LiquidAnimation();
            liquidAnimation.setDuration(5000);
            liquidAnimation.setInterpolator(new DecelerateInterpolator(0.8f));

            bounceAnimation = new BounceAnimation();
            bounceAnimation.setDuration(500);
            bounceAnimation.setInterpolator(new OvershootInterpolator(2.5f));
            bounceAnimation.setStartOffset(5000);

            tickAnimation = new TickAnimation();
            tickAnimation.setDuration(800);
            tickAnimation.setInterpolator(new OvershootInterpolator(2.0f));
            tickAnimation.setStartOffset(5500);

            set.addAnimation(liquidAnimation);
            set.addAnimation(bounceAnimation);
            set.addAnimation(tickAnimation);
        }

        startAnimation(set);



    }
}
