package com.joe.slidemenulayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.RelativeLayout;

/**
 * Created by qiaorongzhu on 2017/1/13.
 */

public class SlideMenuLayout extends RelativeLayout {

    private int slidableDistance;
    private float canvasTranslation;
    float x1 = 0;
    float y1 = 0;
    private float xVel;
    private ValueAnimator smoothAnim;
    private VelocityTracker mVelocityTracker;
    private int mMaximumVelocity;
    private boolean slided = false;
    private boolean slideRight = false;
    private PointF actionDownPoint = new PointF();
    private View menuChildView;
    private View mainChildView;
    private onMenuClickListener onMenuClickListener;
    private OnClickListener l;
    private int touchSlop;

    public SlideMenuLayout(Context context) {
        this(context, null);
    }

    public SlideMenuLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mMaximumVelocity = ViewConfiguration.get(context)
                .getScaledMaximumFlingVelocity();
        mVelocityTracker = VelocityTracker.obtain();
        setLongClickable(true);
        setClickable(true);
        setWillNotDraw(false);
    }

    public SlideMenuLayout setOnMenuClickListener(onMenuClickListener onMenuClickListener) {
        this.onMenuClickListener = onMenuClickListener;
        return this;
    }

    public SlideMenuLayout setMenuBackgroundColor(int color) {
        menuChildView.setBackgroundColor(color);
        invalidate();
        return this;
    }

    public void setMenuClosed() {
        canvasTranslation = 0;
        invalidate();
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        this.l = l;
        super.setOnClickListener(l);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (menuChildView == null || mainChildView == null) {
            super.onDraw(canvas);
        } else {
            drawChild(canvas, menuChildView, System.currentTimeMillis());
            Matrix matrix = new Matrix();
            matrix.setTranslate(canvasTranslation, 0);
            canvas.concat(matrix);
            drawChild(canvas, mainChildView, System.currentTimeMillis());
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float deltaX;
        float x2;
        float y2;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                y1 = event.getY();
                actionDownPoint.set(x1, y1);
                return true;
            case MotionEvent.ACTION_MOVE:
                x2 = event.getX();
                y2 = event.getY();
                if (canvasTranslation == 0 && slided) {
                    x1 = x2;
                    y1 = y2;
                    slided = false;
                    return super.onTouchEvent(event);
                }

                if (canvasTranslation < 0 && x1 > (getMeasuredWidth() + canvasTranslation)) {
                    x1 = x2;
                    y1 = y2;
                    return super.onTouchEvent(event);
                }

                if (Math.abs(y2 - y1) > Math.abs(x2 - x1)) {
                    x1 = x2;
                    y1 = y2;
                    return super.onTouchEvent(event);
                }

                if (Math.abs(x2 - actionDownPoint.x) < touchSlop) {
                    x1 = x2;
                    y1 = y2;
                    return super.onTouchEvent(event);
                }

                if (smoothAnim != null && smoothAnim.isRunning()) {
                    smoothAnim.cancel();
                }

                mVelocityTracker.addMovement(event);
                mVelocityTracker.computeCurrentVelocity(1, mMaximumVelocity);
                xVel = mVelocityTracker.getXVelocity();

                deltaX = x2 - x1;
                slideRight = deltaX > 0;
                x1 = x2;
                y1 = y2;
                canvasTranslation += deltaX;
                slided = true;
                canvasTranslation = canvasTranslation > 0 ? 0 : canvasTranslation;
                canvasTranslation = canvasTranslation < -slidableDistance ? -slidableDistance : canvasTranslation;

                invalidate();
                return true;
            case MotionEvent.ACTION_UP:

                if (canvasTranslation != 0) {
                    float endValue = 0;
                    long flingDuration = 300;
                    if ((canvasTranslation < -slidableDistance / 8 && !slideRight) || canvasTranslation < -7 * slidableDistance / 8) {
                        flingDuration = (long) Math.abs((-slidableDistance - canvasTranslation) / xVel);
                        flingDuration = flingDuration > 300 ? 300 : flingDuration;
                        flingDuration = flingDuration < 200 ? 200 : flingDuration;
                        endValue = -slidableDistance;
                    } else if (canvasTranslation < 0 && canvasTranslation > -7 * slidableDistance / 8 && slideRight) {
                        endValue = 0;
                    }

                    if (actionDownPoint.equals(event.getX(), event.getY()) && canvasTranslation == -slidableDistance) {
                        if (onMenuClickListener != null && (canvasTranslation == -slidableDistance && event.getX() > getMeasuredWidth() + canvasTranslation)) {
                            onMenuClickListener.onMenuClick();
                        }
                        endValue = 0;
                        flingDuration = 300;
                    }

                    smoothToEnd(canvasTranslation, endValue, flingDuration);
                } else if (l != null && actionDownPoint.equals(event.getX(), event.getY())) {
                    l.onClick(this);
                }
                return true;

        }
        return super.onTouchEvent(event);
    }

    private void smoothToEnd(float startValue, final float endValue, long duration) {
        smoothAnim = ValueAnimator.ofFloat(startValue, endValue);
        smoothAnim.setDuration(duration);
        smoothAnim.setInterpolator(new DecelerateInterpolator());
        smoothAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                canvasTranslation = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        smoothAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (l != null) {
                    if (endValue == -slidableDistance) {
                        onMenuClickListener.onMenuOpened();
                    } else if (endValue == 0) {
                        onMenuClickListener.onMenuClosed();
                    }
                }
            }
        });
        smoothAnim.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        for (int i = 0; i < getChildCount(); i++) {
            int childWidth = getChildAt(i).getMeasuredWidth();
            if (childWidth < w && childWidth > 10) {
                slidableDistance = childWidth;
                menuChildView = getChildAt(i);
            } else {
                mainChildView = getChildAt(i);
            }
        }
    }

    public interface onMenuClickListener {
        void onMenuClick();

        void onMenuOpened();

        void onMenuClosed();
    }

}
