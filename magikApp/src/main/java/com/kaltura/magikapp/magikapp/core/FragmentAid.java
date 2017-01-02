package com.kaltura.magikapp.magikapp.core;

import android.view.View;

import com.kaltura.magikapp.magikapp.toolbar.ToolbarMediator;

/**
 * Created by tehilarozin on 06/06/2016.
 */

public interface FragmentAid {

    void setToolbarTitle(String title);

    void setToolbarHomeButton(@ToolbarMediator.ToolbarHomeButton int button);

    void setToolbarActionListener(ToolbarMediator.ToolbarActionListener listener);

    void setStatusBarColor(int color);

    boolean changeToolbarLayoutColor(boolean toBeTransparent, final View... applyTo);

    void onBackPressed();

    void setWaitProgressVisibility(int state);

}
