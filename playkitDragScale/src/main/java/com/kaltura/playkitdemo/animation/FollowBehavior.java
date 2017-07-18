package com.kaltura.playkitdemo.animation;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

import com.kaltura.playkitdemo.R;

import static com.kaltura.playkitdemo.animation.ScaleDragBehavior.State.STATE_EXPANDED;

/**
 * Created by glebgleb on 7/13/17.
 */

public class FollowBehavior extends CoordinatorLayout.Behavior {

    private float shrinkRate;
    private float mediaHeight;
    private int marginBottom;
    private int marginRight;

    private int shrinkContentMarginTop = 0;
    private int parentHeight = 0;

    public FollowBehavior(Context c, AttributeSet attrs) {
        super(c, attrs);
        if (attrs == null) {
            shrinkRate = 0.5f;
            mediaHeight = 600f;
            marginBottom = 0;
            marginRight = 0;
        } else {
            TypedArray youtubeBehaviorParams = c.obtainStyledAttributes(attrs, R.styleable.YoutubeLikeBehaviorParam);
            shrinkRate = youtubeBehaviorParams.getFloat(R.styleable.YoutubeLikeBehaviorParam_shrinkRate, 0.5f);
            mediaHeight = youtubeBehaviorParams.getDimension(R.styleable.YoutubeLikeBehaviorParam_mediaHeight, 600f);
            marginBottom = youtubeBehaviorParams.getDimensionPixelSize(R.styleable.YoutubeLikeBehaviorParam_ylb_marginBottom, 0);
            marginRight = youtubeBehaviorParams.getDimensionPixelSize(R.styleable.YoutubeLikeBehaviorParam_ylb_marginRight, 0);
            youtubeBehaviorParams.recycle();
        }
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        return ScaleDragBehavior.fromView(dependency) != null;
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, View child, int layoutDirection) {
        parentHeight = parent.getHeight();
        shrinkContentMarginTop = Math.min(parentHeight, (int)(parentHeight - mediaHeight + mediaHeight * shrinkRate / 2)) - marginBottom;
        parent.onLayoutChild(child, layoutDirection);
        ViewCompat.offsetTopAndBottom(child, (int)mediaHeight);
        return true;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, View child, View dependency) {
        super.onDependentViewChanged(parent, child, dependency);
        float rate = dependency.getY() / shrinkContentMarginTop;
        child.setAlpha(1f - rate);
        child.setY(dependency.getY() + mediaHeight);
        return true;
    }

    public void setShrinkRate(float shrinkRate) {
        this.shrinkRate = shrinkRate;
    }

    public void setMediaHeight(float mediaHeight) {
        this.mediaHeight = mediaHeight;
    }

    public void setMarginBottom(int marginBottom) {
        this.marginBottom = marginBottom;
    }

    public void setMarginRight(int marginRight) {
        this.marginRight = marginRight;
    }
}
