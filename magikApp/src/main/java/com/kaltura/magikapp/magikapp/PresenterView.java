package com.kaltura.magikapp.magikapp;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import com.kaltura.magikapp.R;

/**
 * Created by itanbarpeled on 18/12/2016.
 */

public class PresenterView extends ConstraintLayout {


    public PresenterView(Context context) {
        this(context, null);
    }

    public PresenterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PresenterView(Context context, AttributeSet attrs, int defStyleAttr) {

        super(context, attrs, defStyleAttr);

        LayoutInflater.from(context).inflate(R.layout.presenter_container, this);

        initPresentationViews();

    }


    private void initPresentationViews() {

        //mProgressBar = (ProgressBar) findViewById(R.id.icon_progress_bar);

    }


}
