package com.kaltura.magikapp.magikapp.menu;

import android.app.Activity;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;

/**
 * Created by zivilan on 01/01/2017.
 */

public class SideMenuMediator implements MenuMediator, SideMenuListener{

    private static final int DRAWER_GRAVITY = GravityCompat.START;
    public Activity mActivity;
    public DrawerLayout mDrawerLayout;


    private SideMenuView mSideMenu;

    public SideMenuMediator(Activity activity, int drawerId, int menuId) {
        mActivity = activity;
        initMenu(menuId, drawerId);
    }

    private void initMenu(int menuId, int drawerId) {
        mDrawerLayout = (DrawerLayout)mActivity.findViewById(drawerId);
        mSideMenu = (SideMenuView) mActivity.findViewById(menuId);
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

    }

    @Override
    public boolean onBackPressed() {
        return closeDrawer();
    }

    @Override
    public void onMenuClick(MenuItem menuItem) {
        mSideMenu.getMenuAdapter().onMenuItemClick(menuItem, MenuHelper.getPositionByMenuType(menuItem.getStringType()));
    }

    @Override
    public boolean onMenuClicked(MenuItem menuItem) {

        switch (menuItem.getStringType()) {

            default:
                closeDrawer();
                break;
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
