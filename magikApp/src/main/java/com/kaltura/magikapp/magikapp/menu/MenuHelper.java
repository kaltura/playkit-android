package com.kaltura.magikapp.magikapp.menu;

import android.text.TextUtils;

import com.connect.backend.magikapp.data.Configuration;
import com.kaltura.magikapp.MagikApplication;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zivilan on 01/01/2017.
 */

public class MenuHelper {
    private static MenuHelper sMenuHelper = null;
    private List<MenuItem> menuItems;

    private MenuHelper() {
    }

    public static MenuHelper getInstance() {
        if (sMenuHelper == null) {
            sMenuHelper = new MenuHelper();
        }
        return sMenuHelper;
    }

    public List<MenuItem> getMenuItems(){
        if(menuItems == null){
            ArrayList<Configuration.MenuItemConf> configItems = MagikApplication.get().getConfigurations().getMenu();
            menuItems = new ArrayList<>();
            if(configItems != null) {
                for (Configuration.MenuItemConf menuItemConf : configItems) {
                    menuItems.add(new MenuItem(menuItemConf));
                }
            }
        }
        return menuItems;
    }

    public static int getMenuItemPosition(String id) {
        if(!TextUtils.isEmpty(id)) {
            List<MenuItem> menuItems = getInstance().menuItems;
            for (int i = 0; i < menuItems.size(); i++) {
                if (getInstance().menuItems.get(i).getId().equals(id)) {
                    return i;
                }
            }
        }
        return -1;
    }


}
