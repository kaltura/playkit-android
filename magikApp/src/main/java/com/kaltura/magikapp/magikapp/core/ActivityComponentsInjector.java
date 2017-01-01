package com.kaltura.magikapp.magikapp.core;

import android.support.v7.app.AppCompatActivity;

import com.kaltura.magikapp.magikapp.menu.MenuMediator;
import com.kaltura.magikapp.magikapp.toolbar.ToolbarMediator;

/**
 * Created by tehilarozin on 26/06/2016.
 */
public interface ActivityComponentsInjector {

    ToolbarMediator getToolbar(AppCompatActivity provider);

    MenuMediator getMenu(AppCompatActivity provider);

}
