package com.kaltura.magikapp.magikapp;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import com.kaltura.magikapp.R;

/**
 * Created by itanbarpeled on 21/12/2016.
 */

public class LocalPlayerPresenterView extends FrameLayout {


    public LocalPlayerPresenterView(Context context) {
        this(context, null);
    }

    public LocalPlayerPresenterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LocalPlayerPresenterView(Context context, AttributeSet attrs, int defStyleAttr) {

        super(context, attrs, defStyleAttr);

        LayoutInflater.from(context).inflate(R.layout.player_presenter, this);

    }
}
