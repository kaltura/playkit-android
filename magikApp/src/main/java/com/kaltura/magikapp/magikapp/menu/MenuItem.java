package com.kaltura.magikapp.magikapp.menu;

/**
 * Created by zivilan on 01/01/2017.
 */

public class MenuItem {
    public static final int TYPE_TITLE = -2;
    public static final int TYPE_MENU_ITEM = -3;

    private String mMenuName;
    private int mMenuType;
    private int resIcon;
    private boolean itemSelected;
    private int mMenuId;

    public MenuItem(int type, String title) {
        this.mMenuType = type;
        this.mMenuName = title;
    }
    public MenuItem(int type, String title, int resIcon) {
        this.mMenuType = type;
        this.mMenuName = title;
        this.resIcon = resIcon;
    }

    public String getMenuName() {
        return mMenuName;
    }

    public void setName(String name) {
        mMenuName = name;
    }

    public int getMenuType() {
        return mMenuType;
    }

    public void setType(int type) {
        mMenuType = type;
    }

    public boolean isItemSelected() {
        return itemSelected;
    }

    public void setItemSelected(boolean itemSelected) {
        this.itemSelected = itemSelected;
    }

    public void setId(int id) {
        mMenuId = id;
    }

    public int getId() {
        return mMenuId;
    }

    public int getResIcon() {
        return resIcon;
    }

    public void setResIcon(int resIcon) {
        this.resIcon = resIcon;
    }


}
