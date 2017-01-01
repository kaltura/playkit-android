package com.kaltura.magikapp.magikapp;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.kaltura.magikapp.R;
import com.kaltura.magikapp.magikapp.core.ActivityComponentsInjector;
import com.kaltura.magikapp.magikapp.core.ComponentsInjector;
import com.kaltura.magikapp.magikapp.menu.MenuMediator;
import com.kaltura.magikapp.magikapp.toolbar.ToolbarMediator;


public class ScrollingActivity extends AppCompatActivity implements  ToolbarMediator.ToolbarActionListener{

    public MenuMediator mMenuMediator;
    private ToolbarMediator mToolbarMediator;
    protected CoordinatorLayout mCoordMainContainer;
    protected CollapsingToolbarLayout mCollapsingToolbar;
    protected FragmentManager mFragmentManager;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.base_drawer);

        initComponents();
        initOthers();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    protected void initComponents() {
        ActivityComponentsInjector injector = getComponentsInjector();// create function for the injector and change it in users activity

        mMenuMediator = injector.getMenu(this);// new SlideMenuMediator(this, R.id.drawer_layout, R.id.sideMenu);

        mToolbarMediator = injector.getToolbar(this);// new TopToolbarMediator(this, R.id.toolbar, this);
        mToolbarMediator.setToolbarActionListener(this);

        mCoordMainContainer = (CoordinatorLayout) findViewById(R.id.activity_scrolling);
        mCollapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
    }

    @NonNull
    protected ActivityComponentsInjector getComponentsInjector() {
        return new ComponentsInjector();
    }

    protected void initOthers() {
        mFragmentManager = getSupportFragmentManager();
    }

    @Override
    public void onToolbarAction(ToolbarMediator.ToolbarAction action) {
        switch (action) {
            case Menu:
                mMenuMediator.toggleDrawer();
                break;

            case Back:
                onBackPressed();
                break;
        }
    }

    protected boolean closeMenu() {
        if (mMenuMediator.closeDrawer()) {
            mToolbarMediator.setHomeButton(ToolbarMediator.BUTTON_MENU);
            return true;
        }
        return false;
    }

    protected boolean backOnStack() {
        boolean handled = false;
        if (mFragmentManager != null && mFragmentManager.getBackStackEntryCount() >= 1) {
            handled = mFragmentManager.getBackStackEntryCount() > 1;
            mFragmentManager.popBackStackImmediate();
        }
        return handled;
    }

    protected int getFragmentsContainerId(){
        return R.id.frags_container;
    }

}
