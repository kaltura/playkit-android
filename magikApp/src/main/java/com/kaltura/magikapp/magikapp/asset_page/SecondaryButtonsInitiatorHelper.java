package com.kaltura.magikapp.magikapp.asset_page;

import com.kaltura.mediago.fragments.helen.views.roundbutton.IconTextRoundButton;
import com.kaltura.mediago.fragments.helen.views.roundbutton.RoundButton;
import com.tvinci.sdk.catalog.AssetInfo;

/**
 * Created by vladir on 28/11/2016.
 */

public class SecondaryButtonsInitiatorHelper {

    public static void init(SecondaryButtons secondaryButtonsView, AssetInfo asset) {
        IconTextRoundButton likeButton = (IconTextRoundButton) secondaryButtonsView.getButton(SecondaryButtons.SecondaryButtonType.Like);
        SecondaryViewsInitiator.initLike(asset, likeButton);

        RoundButton favoriteButton = (RoundButton) secondaryButtonsView.getButton(SecondaryButtons.SecondaryButtonType.Favorites);
        SecondaryViewsInitiator.initFavorites(asset, favoriteButton);
    }


}
