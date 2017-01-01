package com.kaltura.magikapp.magikapp.menu;

import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;

/**
 * Created by zivilan on 01/01/2017.
 */

public class SideMenuMediator implements MenuMediator, SideMenuListener{

    private static final int DRAWER_GRAVITY = GravityCompat.START;

    private SideMenuView mSideMenu;

    public SideMenuMediator(PluginProvider provider, int drawerId, int menuId) {
        super(provider, drawerId);
        initMenu(menuId);
    }

    private void initMenu(int menuId) {
        mSideMenu = (SideMenuView) mActivity.findViewById(menuId);
        mSideMenu.setUserState(TvinciSDK.getSessionManager().isLoggedIn() ? SideMenuView.USER_LOGGED_IN : SideMenuView.USER_LOGGED_OUT);
        mSideMenu.setMenuListener(this);
    }

    @Override
    public boolean openDrawer() {
        if (!mDrawerLayout.isDrawerOpen(DRAWER_GRAVITY)) {
            mDrawerLayout.openDrawer(DRAWER_GRAVITY);
            return true;
        }
        return false;
    }

    @Override
    public boolean closeDrawer() {
        if (mDrawerLayout.isDrawerOpen(DRAWER_GRAVITY)) {
            mDrawerLayout.closeDrawer(DRAWER_GRAVITY);
            return true;
        }
        return false;
    }

    @Override
    public void refreshDrawer() {
        mSideMenu.refreshSideMenu();
    }

    @Override
    public boolean onBackPressed() {
        return closeDrawer();
    }

    @Override
    public void onMenuClicked(MenuItem menuItem) {
        mSideMenu.getMenuAdapter().onMenuItemClick(menuItem, MenuHelper.getPositionByMenuType(menuItem.getStringType()));
    }

    public boolean onMenuClicked(MenuItem menuItem, Object extra) {

        switch (menuItem.getStringType()) {

        }
        return false;

    }

    @Override
    public void update(String menuType) {
        if(!TextUtils.isEmpty(menuType)) {
            mSideMenu.setMenuSelectionByType(menuType);
        }
    }

    @Override
    public void setDrawerListener(DrawerLayout.DrawerListener toggle){
        mDrawerLayout.addDrawerListener(toggle);
    }

    @Override
    public void removeDrawerListener(DrawerLayout.DrawerListener toggle){
        mDrawerLayout.removeDrawerListener(toggle);
    }

    @Override
    public void toggleDrawer() {
        if(!closeDrawer()) {
            openDrawer();
        }
    }

}
