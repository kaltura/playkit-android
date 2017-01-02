package com.kaltura.magikapp.magikapp.asset_page;

/**
 * Created by vladir on 28/11/2016.
 */

public class SecondaryButtonsInitiatorHelper {

    public static void init(SecondaryButtons secondaryButtonsView, AssetInfo asset) {
        IconTextRoundButton likeButton = (IconTextRoundButton) secondaryButtonsView.getButton(SecondaryButtons.SecondaryButtonType.Like);

        RoundButton favoriteButton = (RoundButton) secondaryButtonsView.getButton(SecondaryButtons.SecondaryButtonType.Favorites);
    }


}
