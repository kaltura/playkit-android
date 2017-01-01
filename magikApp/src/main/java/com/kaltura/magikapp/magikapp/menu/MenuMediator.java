package com.kaltura.magikapp.magikapp.menu;

import android.support.v4.widget.DrawerLayout;


public interface MenuMediator {

    boolean openDrawer();

    boolean closeDrawer();

    boolean onBackPressed();

    void toggleDrawer();

    void update(String menuType);

    void setDrawerListener(DrawerLayout.DrawerListener toggle);

    void removeDrawerListener(DrawerLayout.DrawerListener toggle);

    void refreshDrawer();

    void onMenuClicked(MenuItem menuItem);

}
