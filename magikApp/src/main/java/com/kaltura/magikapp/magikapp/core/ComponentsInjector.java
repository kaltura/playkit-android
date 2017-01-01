package com.kaltura.magikapp.magikapp.core;

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
    public ToolbarMediator getToolbar(PluginProvider provider) {
        return new TopToolbarMediator(provider, R.id.toolbar, R.id.app_bar);
    }

    @Override
    public MenuMediator getMenu(PluginProvider provider) {
        return new SideMenuMediator(provider, R.id.drawer_layout, R.id.sideMenu);
    }
}
