package com.kaltura.magikapp.magikapp;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

/**
 * Created by Gleb on 5/30/16.
 */
public class BackgroundMovementAnimation {

    public static Animator getMoveAnimator(View v, int screenWidth) {
        int dw = screenWidth / 10;
        RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(screenWidth + dw, ViewGroup.LayoutParams.MATCH_PARENT);
        v.setLayoutParams(p);
        ObjectAnimator animRight = ObjectAnimator.ofFloat(v, View.TRANSLATION_X, 0, -dw, 0);
        animRight.setDuration(60000);
        animRight.setRepeatCount(ValueAnimator.INFINITE);
        animRight.setRepeatMode(ValueAnimator.RESTART);
        return animRight;
    }
}
