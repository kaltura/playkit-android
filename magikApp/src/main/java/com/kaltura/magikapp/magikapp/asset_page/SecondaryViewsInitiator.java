package com.kaltura.magikapp.magikapp.asset_page;

import com.kaltura.mediago.datatypes.sdk.SocialHelper;
import com.tvinci.sdk.catalog.AssetInfo;
import com.tvinci.sdk.utils.StepListener;
import com.tvinci.sdk.utils.TvinciSDKUtils;

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

    public static void initLike(AssetInfo asset, final SecondaryView view){

        SocialHelper.getInstance().isMediaLiked(asset, new StepListener<Boolean>() {
            @Override
            public void onStepEnded(Boolean isSuccess) {
                view.setSelected(isSuccess);
            }
        });

        SocialHelper.getInstance().getLikeCount(asset, new StepListener<Integer>() {
            @Override
            public void onStepEnded(Integer data) {
                if (data > 0) {
                    view.setText(TvinciSDKUtils.safeString(data));
                }
            }
        });
    }

    public static void initFavorites(AssetInfo asset, final SecondaryView view){

        SocialHelper.getInstance().isMediaFavorite(asset, new StepListener<Boolean>() {
            @Override
            public void onStepEnded(Boolean isSuccess) {
                view.setSelected(isSuccess);
            }
        });
    }

}
