package com.kaltura.magikapp.magikapp.toolbar;

import android.graphics.drawable.Drawable;
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

    void setToolbarLogo(Drawable logo);

    void setToolbarActionListener(ToolbarActionListener listener);

    void setToolbarColor(int color);

    ToolbarMediator.ToolbarAction getHomeButton();

    void setHomeButton(@ToolbarHomeButton int button, Drawable[] drawables);

    void onToolbarMenuAction(int id);

    void setToolbarMenu(Menu menu);

}
