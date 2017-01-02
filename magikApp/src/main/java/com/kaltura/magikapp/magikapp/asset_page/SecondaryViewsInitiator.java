package com.kaltura.magikapp.magikapp.asset_page;

/**
 * Created by vladir on 29/11/2016.
 *
 * Used to initiate all social view in the app
 * just implement SocialView interface
 */

public class SecondaryViewsInitiator {

    public interface SecondaryView {
        void setSelected(boolean isSelected);
        void setText(String text);
    }

}
