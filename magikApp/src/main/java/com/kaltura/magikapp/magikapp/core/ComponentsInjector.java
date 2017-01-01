package com.kaltura.magikapp.magikapp.core;

import android.support.v7.app.AppCompatActivity;

import com.kaltura.magikapp.R;
import com.kaltura.magikapp.magikapp.menu.MenuMediator;
import com.kaltura.magikapp.magikapp.menu.SideMenuMediator;
import com.kaltura.magikapp.magikapp.toolbar.ToolbarMediator;
import com.kaltura.magikapp.magikapp.toolbar.TopToolbarMediator;

/**
 * Created by zivilan on 01/01/2017.
 */

public class ComponentsInjector implements ActivityComponentsInjector {

    @Override
    public ToolbarMediator getToolbar(AppCompatActivity provider) {
        return new TopToolbarMediator(provider, R.id.toolbar, R.id.app_bar);
    }

    @Override
    public MenuMediator getMenu(AppCompatActivity provider) {
        return new SideMenuMediator(provider, R.id.drawer_layout, R.id.sideMenu);
    }
}
