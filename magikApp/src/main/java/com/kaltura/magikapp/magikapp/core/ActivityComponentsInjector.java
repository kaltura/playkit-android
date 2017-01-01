package com.kaltura.magikapp.magikapp.core;

import com.kaltura.magikapp.magikapp.menu.MenuMediator;
import com.kaltura.magikapp.magikapp.toolbar.ToolbarMediator;

/**
 * Created by tehilarozin on 26/06/2016.
 */
public interface ActivityComponentsInjector {

    ToolbarMediator getToolbar(PluginProvider provider);

    MenuMediator getMenu(PluginProvider provider);

}
