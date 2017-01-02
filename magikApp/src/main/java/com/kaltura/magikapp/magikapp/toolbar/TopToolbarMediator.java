package com.kaltura.magikapp.magikapp.toolbar;

import android.graphics.drawable.Drawable;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.kaltura.magikapp.R;
import com.kaltura.magikapp.magikapp.core.PluginProvider;

/**
 * Created by zivilan on 01/01/2017.
 */

public class TopToolbarMediator implements View.OnClickListener, ToolbarMediator {
    public static final int BUTTON_MENU = 1;
    public static final int BUTTON_BACK = -1;

    PluginProvider mProvider;

    private AppBarLayout mAppBar;
    protected Toolbar mToolbar; // the common top bar
    private ActionBar mToolActionbar;
    private Menu mToolbarMenu;
    private TextView mToolbarTitle;
    private ImageView mToolbarLogo;
    private ToolbarMediator.ToolbarActionListener mActionListener;
    private @ToolbarHomeButton int mToolbarHomeButton = BUTTON_MENU;


    public TopToolbarMediator(PluginProvider activity, int toolbarId) {
        this(activity, toolbarId, -1);
    }

    public TopToolbarMediator(PluginProvider activity, int toolbarId, int appBarId) {
        mProvider = activity;
        initToolbar(toolbarId, appBarId);
    }

    private void initToolbar(int toolbarId, int appBarId) {
        AppCompatActivity activity = mProvider.getActivity();

        mToolbar = (Toolbar) activity.findViewById(toolbarId);
        activity.setSupportActionBar(mToolbar);

        mToolActionbar = activity.getSupportActionBar();
        mToolActionbar.setHomeAsUpIndicator(R.mipmap.menu_icon_tablet);
        mToolActionbar.setDisplayHomeAsUpEnabled(true);
        mToolActionbar.setDisplayShowTitleEnabled(false);

//        mToolbarTitle = (TextView) activity.findViewById(R.id.toolbar_title);
//        mToolbarTitle.setOnClickListener(this);

        mToolbarLogo = (ImageView) activity.findViewById(R.id.toolbar_logo);

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
    public void setToolbarLogo(Drawable logo){
        if (mToolbarLogo != null) {
            mToolbarLogo.setImageDrawable(logo);
        }
    }

    @Override
    public void setToolbarColor(int color){
        mToolbar.setBackgroundColor(color);
    }

    @Override
    public void setHomeButton(@ToolbarHomeButton int button, Drawable[] drawables) {
        switch (button) {
            case ToolbarMediator.BUTTON_BACK:
                mToolbar.setNavigationIcon(drawables[0]);
                break;
            case ToolbarMediator.BUTTON_MENU:
                mToolbar.setNavigationIcon(drawables[1]);
                break;
        }
        mToolbarHomeButton = button;
    }

    @Override
    public ToolbarMediator.ToolbarAction getHomeButton(){
        switch (mToolbarHomeButton){
            case BUTTON_BACK:
                return ToolbarAction.Back;
            case BUTTON_MENU:
                return ToolbarAction.Menu;
            default:
                return ToolbarAction.Menu;
        }
    }

    @Override
    public void setToolbarActionListener(ToolbarActionListener listener) {
        mActionListener = listener;
    }

    public void onToolbarMenuAction(int itemId) {
        ToolbarAction action = null;
        switch (itemId) {
            case android.R.id.home:
                switch (mToolbarHomeButton) {
                    case ToolbarMediator.BUTTON_MENU:
                        action = ToolbarAction.Menu;
                        break;
                    case ToolbarMediator.BUTTON_BACK:
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
