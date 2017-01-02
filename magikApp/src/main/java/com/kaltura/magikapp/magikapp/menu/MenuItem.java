package com.kaltura.magikapp.magikapp.menu;

import com.connect.backend.magikapp.data.Configuration;

/**
 * Created by zivilan on 01/01/2017.
 */

public class MenuItem {
    public static final int TYPE_TITLE = -2;
    public static final int TYPE_MENU_ITEM = -3;

    private String id;
    private String title;
    private int resIcon;
    private boolean isSelected;


    public MenuItem(Configuration.MenuItemConf menuItemConf) {
        this.id = menuItemConf.name;
        this.title = menuItemConf.title;
    }

    public String getTitle() {
        return title;
    }

    public String getId() {
        return id;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
