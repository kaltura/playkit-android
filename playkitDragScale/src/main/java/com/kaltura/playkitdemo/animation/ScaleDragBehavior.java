package com.kaltura.playkitdemo.animation;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.CoordinatorLayout.Behavior;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.support.v4.widget.ViewDragHelper.Callback;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewGroup.LayoutParams;

import com.kaltura.playkitdemo.R;

import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;

import static com.kaltura.playkitdemo.animation.ScaleDragBehavior.State.STATE_DRAGGING;
import static com.kaltura.playkitdemo.animation.ScaleDragBehavior.State.STATE_EXPANDED;
import static com.kaltura.playkitdemo.animation.ScaleDragBehavior.State.STATE_SETTLING;
import static com.kaltura.playkitdemo.animation.ScaleDragBehavior.State.STATE_SHRINK;
import static com.kaltura.playkitdemo.animation.ScaleDragBehavior.State.STATE_SHRINK_DRAGGING;
import static com.kaltura.playkitdemo.animation.ScaleDragBehavior.State.STATE_TO_LEFT;
import static com.kaltura.playkitdemo.animation.ScaleDragBehavior.State.STATE_TO_RIGHT;


public class ScaleDragBehavior extends CoordinatorLayout.Behavior<View> {

    public enum State {
        STATE_DRAGGING,
        STATE_SETTLING,
        STATE_EXPANDED,
        STATE_SHRINK,
        STATE_TO_LEFT,
        STATE_TO_RIGHT,
        STATE_HIDDEN,
        STATE_SHRINK_DRAGGING;
    }

    private static final int REMOVE_THRETHOLD=30*3;

    private ScaleDragBehavior.OnBehaviorStateListener listener;
    @Nullable
    private VelocityTracker velocityTracker;
    private WeakReference<View> viewRef;
    private ScaleDragBehavior.DragCallback dragCallback;
    private ViewDragHelper dragHelper;
    private State state;
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;
    private boolean ignoreEvents = false;
    private boolean draggable = true;
    private int initialX = 0;
    private int initialY = 0;
    private float shrinkRate;
    private int marginBottom;
    private int marginRight;
    private int parentHeight = 0;
    private int parentWidth = 0;
    private int originalWidth;
    private int originalHeight;
    private int leftMargin = 0;
    private int shrinkMarginTop = 0;

    public abstract static class OnBehaviorStateListener {
        public abstract void onBehaviorStateChanged(State state);
    }

    public class DragCallback extends Callback {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            if (ScaleDragBehavior.fromView(child) == null) {
                return false;
            }
            if (state == STATE_DRAGGING) {
                return false;
            }
            return viewRef.get() != null;
        }

        @Override
        public void onViewDragStateChanged(int state) {
            //super.onViewDragStateChanged(state);
            State currentState = ScaleDragBehavior.this.state;
            if (state == ViewDragHelper.STATE_DRAGGING) {
                if (currentState == STATE_EXPANDED || currentState == STATE_DRAGGING) {
                    setStateInternal(STATE_DRAGGING);
                } else if (currentState == STATE_SHRINK_DRAGGING || currentState == STATE_SHRINK) {
                    setStateInternal(STATE_SHRINK_DRAGGING);
                }
            }
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            //super.onViewPositionChanged(changedView, left, top, dx, dy);
            dispatchOnSlide(top);
            float rate = (shrinkRate - 1) / shrinkMarginTop * (float) top + 1;
            Log.v("scale","scaleFactor = " + rate + " originW = "+originalWidth+" originH = " +originalHeight);

            changedView.setTranslationX(leftMargin * 2f * (1f - rate));
            onScale(changedView, rate, originalWidth, originalHeight);
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            //super.onViewReleased(releasedChild, xvel, yvel);
            State targetState = state;
            int currentTop = releasedChild.getTop();
            int currentLeft = releasedChild.getLeft();
            int top = currentTop;
            int left = currentLeft;
            if (state == STATE_DRAGGING) {
                if (Math.abs(currentTop) < Math.abs(parentHeight / 2f)) {
                    top = 0;
                    targetState = STATE_EXPANDED;
                } else {
                    top = shrinkMarginTop;
                    targetState = STATE_SHRINK;
                }
            } else if (state == STATE_SHRINK_DRAGGING) {
                if (currentLeft < -REMOVE_THRETHOLD) {
                    left = -parentWidth;
                    targetState = STATE_TO_LEFT;
                } else if (REMOVE_THRETHOLD < currentLeft) {
                    left = parentWidth;
                    targetState = STATE_TO_RIGHT;
                } else {
                    left = 0;
                    targetState = STATE_SHRINK;
                }
            }

            boolean settleCaptureViewAt = dragHelper != null ? dragHelper.settleCapturedViewAt(left, top) : false;
            if (settleCaptureViewAt) {
                setStateInternal(STATE_SETTLING);
                ViewCompat.postOnAnimation(releasedChild, new SettleRunnable(releasedChild, targetState));
            } else {
                setStateInternal(targetState);
            }
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            if (state == STATE_SHRINK_DRAGGING || state == STATE_SHRINK) {
                return constrain(left, -parentWidth, parentWidth);
            } else {
                return 0;
            }
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            //return super.clampViewPositionVertical(child, top, dy);
            if (state == STATE_SHRINK_DRAGGING || state == STATE_SHRINK) {
                if (Math.abs(shrinkMarginTop - top) >= 10) {
                    setStateInternal(STATE_DRAGGING);
                    return constrain(top, 0, shrinkMarginTop);
                } else {
                    return shrinkMarginTop;
                }
            } else {
                return constrain(top, 0, shrinkMarginTop);
            }
        }

        private void dispatchOnSlide(int offset) {
            // TODO: notify position to listener
        }

        private int constrain(int amount, int low, int high) {
            if (amount < low) {
                return low;
            } else if (amount > high) {
                return high;
            } else {
                return amount;
            }
        }
    }

    public class SettleRunnable implements Runnable {
        private View view;
        private State mState;

        public SettleRunnable(View v, State state){
            view = v;
            mState = state;
        }

        @Override
        public void run() {
            if (dragHelper != null && dragHelper.continueSettling(true)) {
                ViewCompat.postOnAnimation(view, this);
            } else {
                setStateInternal(mState);
            }
        }
    }

    public static ScaleDragBehavior fromView(View v) {
        if (v!= null && v.getLayoutParams() != null) {
            if (v.getLayoutParams() instanceof CoordinatorLayout.LayoutParams) {
                Behavior behavior = ((CoordinatorLayout.LayoutParams)v.getLayoutParams()).getBehavior();
                if (behavior != null && behavior instanceof ScaleDragBehavior) {
                    return (ScaleDragBehavior)behavior;
                }
            }
        }
        return null;
    }

    public ScaleDragBehavior(Context c, AttributeSet attrs) {
        super(c, attrs);
        if (attrs==null) {
            shrinkRate = 0.5f;
            marginBottom = 0;
            marginRight = 0;
            state = STATE_EXPANDED;
        }
        else {
            TypedArray params = c.obtainStyledAttributes(attrs, R.styleable.YoutubeLikeBehaviorParam);
            shrinkRate = params.getFloat(R.styleable.YoutubeLikeBehaviorParam_shrinkRate, 0.5f);
            marginBottom = params.getDimensionPixelSize(R.styleable.YoutubeLikeBehaviorParam_ylb_marginBottom, 0);
            marginRight = params.getDimensionPixelSize(R.styleable.YoutubeLikeBehaviorParam_ylb_marginRight, 0);
            state = STATE_EXPANDED;//State.values()[params.getInt(R.styleable.YoutubeLikeBehaviorParam_start_state, STATE_EXPANDED.ordinal())];
            params.recycle();
        }
        dragCallback = new DragCallback();
    }

    public void updateState(State value) {
        if (this.state == value) {
            return;
        }
        this.state = value;
        final View sheet = viewRef.get();
        ViewParent parent = sheet != null ? sheet.getParent() : null;
        if (parent != null) {
            if (parent.isLayoutRequested() && ViewCompat.isAttachedToWindow(sheet)) {
                sheet.post(new Runnable() {
                    @Override
                    public void run() {
                        startSettlingAnimation(sheet, state);
                    }
                });
            }
            else {
                startSettlingAnimation(sheet, state);
            }
        }
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, View child, int layoutDirection) {
        if (state != STATE_DRAGGING && state != STATE_SETTLING) {
            if (ViewCompat.getFitsSystemWindows(parent) && !ViewCompat.getFitsSystemWindows(child)) {
                ViewCompat.setFitsSystemWindows(child, true);
            }
            parent.onLayoutChild(child, layoutDirection);
        }
        parentHeight = parent.getHeight();
        parentWidth = parent.getWidth();

        float halfChildHeight = child.getHeight() / 2f;
        shrinkMarginTop = Math.min(parentHeight, (int)(parentHeight - (halfChildHeight * (1 + shrinkRate)))) - marginBottom;
        // current(2017/01/11) support media width is "screen width" only
        leftMargin = Math.min(parentWidth, (child.getWidth() / 4)) - marginRight;

        switch (state) {
            case STATE_EXPANDED: {
                ViewCompat.offsetTopAndBottom(child, 0);
                break;
            }
            case STATE_SHRINK: {
                //onScale(child, shrinkRate, originalWidth, originalHeight);
                child.setTranslationX(leftMargin * 2f * (1f - shrinkRate));
                ViewCompat.offsetTopAndBottom(child, shrinkMarginTop);
                break;
            }
            case STATE_TO_LEFT: {
                //onScale(child, shrinkRate, originalWidth, originalHeight);
                ViewCompat.offsetTopAndBottom(child, shrinkMarginTop);
                break;
            }
            case STATE_TO_RIGHT: {
                //onScale(child, shrinkRate, originalWidth, originalHeight);
                ViewCompat.offsetTopAndBottom(child, shrinkMarginTop);
                break;
            }
        }

        if (dragHelper == null) {
            dragHelper = ViewDragHelper.create(parent, dragCallback);
        }
        if (viewRef == null) {
            originalWidth = child.getWidth();
            originalHeight = child.getHeight();
        }
        viewRef = new WeakReference(child);
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, View child, MotionEvent ev) {
        if (!draggable) {
            return false;
        }
        if (!child.isShown()) {
            return false;
        }
        int action = MotionEventCompat.getActionMasked(ev);
        if (action == MotionEvent.ACTION_DOWN) {
            reset();
        }
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }

        velocityTracker.addMovement(ev);
        switch (action) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                if (ignoreEvents) {
                    ignoreEvents = false;
                    return false;
                }
                break;
            }
            case MotionEvent.ACTION_DOWN: {
                initialX = (int)ev.getX();
                initialY = (int)ev.getY();
                ignoreEvents = activePointerId == MotionEvent.INVALID_POINTER_ID
                        && !parent.isPointInChildBounds(child, initialX, initialY);
                break;
            }
        }

        if (!ignoreEvents && dragHelper != null && dragHelper.shouldInterceptTouchEvent(ev)) {
            return true;
        }

        int touchSlop = 0;
        if (dragHelper != null) {
            touchSlop = dragHelper.getTouchSlop();
        }

        return action == MotionEvent.ACTION_MOVE
                && !ignoreEvents
                && state != STATE_DRAGGING
                && (Math.abs(initialX - ev.getX()) > touchSlop || Math.abs(initialY - ev.getY()) > touchSlop);
    }

    @Override
    public boolean onTouchEvent(CoordinatorLayout parent, View child, MotionEvent ev) {
        if (!draggable) {
            return false;
        }
        if (!child.isShown()) {
            return false;
        }
        int action = MotionEventCompat.getActionMasked(ev);
        if (state == STATE_DRAGGING && action == MotionEvent.ACTION_DOWN) {
            return true;
        }

        if (dragHelper != null) {
            dragHelper.processTouchEvent(ev);
        }
        if (action == MotionEvent.ACTION_DOWN) {
            reset();
        }
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(ev);

        if (action == MotionEvent.ACTION_MOVE && !ignoreEvents) {
            int touchSlop = 0;
            if (dragHelper != null) {
                touchSlop = dragHelper.getTouchSlop();
            }
            if (Math.abs(initialX - ev.getX()) > touchSlop
                    || Math.abs(initialY - ev.getY()) > touchSlop) {
                if (dragHelper != null) {
                    dragHelper.captureChildView(child, ev.getPointerId(ev.getActionIndex()));
                }
            }
        }
        return !ignoreEvents;
    }

    private void setStateInternal(State state) {
        if (this.state == state) {
            return;
        }
        this.state = state;
        if (!(this.state == STATE_DRAGGING || this.state == STATE_SETTLING)) {
            if (listener != null) {
                this.listener.onBehaviorStateChanged(state);
            }
        }
    }

    private void reset() {
        activePointerId = ViewDragHelper.INVALID_POINTER;
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    private void startSettlingAnimation(View child, State state) {
        int top;
        int left;
        if (state == STATE_EXPANDED) {
            top = 0;
            left = 0;
        } else if (state == STATE_SHRINK) {
            top = shrinkMarginTop;
            left = 0;
        } else {
            throw new IllegalArgumentException("Illegal state argument: " + state);
        }
        setStateInternal(STATE_SETTLING);
        if (dragHelper != null && dragHelper.smoothSlideViewTo(child, left, top)) {
            ViewCompat.postOnAnimation(child, new SettleRunnable(child, state));
        }
    }

    private void onScale(View v, float scaleFactor, int originalWidth, int originalHeight) {
        //ViewCompat.setScaleX(v, scaleFactor);
        //ViewCompat.setScaleY(v, scaleFactor);

        final int w = (int)((float)originalWidth * scaleFactor);
        final int h = (int)((float)originalHeight * scaleFactor);
        Log.v("change view size","w = "+w+" h = "+h);
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams)v.getLayoutParams();
        params.width = w;
        params.height = h;
        v.requestLayout();

        //params.r
        //v.setLayoutParams(params);
        //v.requestLayout();
        //v.postInvalidate();
    }

    public void setState(State state) {
        this.state = state;
    }

    public void setShrinkRate(float shrinkRate) {
        this.shrinkRate = shrinkRate;
    }

    public void setMarginBottom(int marginBottom) {
        this.marginBottom = marginBottom;
    }

    public void setMarginRight(int marginRight) {
        this.marginRight = marginRight;
    }

    public void setListener(OnBehaviorStateListener listener) {
        this.listener = listener;
    }
}

