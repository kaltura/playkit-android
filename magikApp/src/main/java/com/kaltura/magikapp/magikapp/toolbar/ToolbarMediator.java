package com.kaltura.magikapp.magikapp.toolbar;

import android.support.annotation.IntDef;
import android.view.Menu;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by zivilan on 01/01/2017.
 */

public interface ToolbarMediator {
    public static final int BUTTON_MENU = 1;
    public static final int BUTTON_BACK = -1;
    @IntDef({BUTTON_MENU, BUTTON_BACK})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ToolbarHomeButton {}

    enum ToolbarAction { // actions delivered fromthe toolbar to its listener
        Edit, Save, Menu, Back, Search, Title;
    }

    interface ToolbarActionListener {
        void onToolbarAction(ToolbarAction action);
    }


    void setTitle(String title);

    void setHomeButton(@ToolbarHomeButton int homeButton);

    void setToolbarActionListener(ToolbarActionListener listener);

    void onToolbarMenuAction(int id);

    void setToolbarMenu(Menu menu);

}
