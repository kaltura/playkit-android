package com.kaltura.magikapp.magikapp.toolbar;

import android.support.design.widget.AppBarLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import com.kaltura.magikapp.R;

/**
 * Created by zivilan on 01/01/2017.
 */

public class TopToolbarMediator implements View.OnClickListener, ToolbarMediator {
    public static final int BUTTON_MENU = 1;
    public static final int BUTTON_BACK = -1;

    private AppBarLayout mAppBar;
    protected Toolbar mToolbar; // the common top bar
    private ActionBar mToolActionbar;
    private Menu mToolbarMenu;
    private TextView mToolbarTitle;
    private ToolbarMediator.ToolbarActionListener mActionListener;
    private @ToolbarHomeButton int mToolbarHomeButton = BUTTON_MENU;


    public TopToolbarMediator(AppCompatActivity activity, int toolbarId) {
        this(activity, toolbarId, -1);
    }

    public TopToolbarMediator(AppCompatActivity activity, int toolbarId, int appBarId) {
        initToolbar(activity, toolbarId, appBarId);
    }

    private void initToolbar(AppCompatActivity varActivity, int toolbarId, int appBarId) {
        AppCompatActivity activity = varActivity;

        mToolbar = (Toolbar) activity.findViewById(toolbarId);
        activity.setSupportActionBar(mToolbar);

        mToolActionbar = activity.getSupportActionBar();
        mToolActionbar.setHomeAsUpIndicator(R.mipmap.menu_icon_tablet);
        mToolActionbar.setDisplayHomeAsUpEnabled(true);
        mToolActionbar.setDisplayShowTitleEnabled(false);

        mToolbarTitle = (TextView) activity.findViewById(R.id.toolbar_title);
        mToolbarTitle.setOnClickListener(this);

        if (appBarId != -1) {
            mAppBar = (AppBarLayout) activity.findViewById(appBarId);
        }
    }

    @Override
    public void onClick(View v) {
        if(mActionListener != null){
            mActionListener.onToolbarAction(ToolbarAction.Title);
        }
    }

    @Override
    public void setTitle(String title) {
        if (mToolbarTitle != null) {
            mToolbarTitle.setText(title);
        }
    }


    @Override
    public void setHomeButton(@ToolbarHomeButton int button) {
        switch (button) {
            case BUTTON_BACK:
                //setSortMenuMode(false);
                mToolbar.setNavigationIcon(R.mipmap.ic_action_navigation_arrow_back);
                break;
            case BUTTON_MENU:
                //setSortMenuMode(false);
                mToolbar.setNavigationIcon(R.mipmap.menu_icon_tablet);
                break;
        }
        mToolbarHomeButton = button;
    }

    @Override
    public void setToolbarActionListener(ToolbarActionListener listener) {
        mActionListener = listener;
    }

    public void onToolbarMenuAction(int itemId) {
        ToolbarAction action = null;
        switch (itemId) {
            case android.R.id.home:
                switch (mToolbarHomeButton) { //TODO check name value
                    case BUTTON_MENU:
                        action = ToolbarAction.Menu;
                        break;
                    case BUTTON_BACK:
                        action = ToolbarAction.Back;
                        break;
                }
        }
        if (action != null && mActionListener != null){ //!! in case of more than 1 listenr - change to use event bus
            mActionListener.onToolbarAction(action);
        }
    }

    @Override
    public void setToolbarMenu(Menu menu) {
        mToolbarMenu = menu;
    }

}
