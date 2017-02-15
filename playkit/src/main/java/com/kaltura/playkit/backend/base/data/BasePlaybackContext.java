package com.kaltura.playkit.backend.base.data;

import com.google.gson.annotations.SerializedName;
import com.kaltura.playkit.backend.BaseResult;
import com.kaltura.playkit.backend.ovp.data.KalturaFlavorAsset;
import com.kaltura.playkit.backend.phoenix.PhoenixErrorHelper;
import com.kaltura.playkit.connect.ErrorElement;

import java.util.ArrayList;

/**
 * Created by tehilarozin on 02/11/2016.
 */

public class BasePlaybackContext extends BaseResult{

    ArrayList<KalturaRuleAction> actions;
    ArrayList<KalturaAccessControlMessage> messages;
    ArrayList<KalturaFlavorAsset> flavorAssets;

    public BasePlaybackContext() {
    }



    public ArrayList<KalturaAccessControlMessage> getMessages() {
        return messages;
    }

    public ArrayList<KalturaRuleAction> getActions() {
        return actions;
    }

    public ArrayList<KalturaFlavorAsset> getFlavorAssets() {
        return flavorAssets;
    }



    public ErrorElement hasError() {
        ErrorElement error = null;
        // in case we'll want to gather errors or priorities message, loop over messages. Currently returns the first error
        if(messages != null) {
            for (BasePlaybackContext.KalturaAccessControlMessage message : messages) {
                error = message.getErrorElement();
                if (error != null) {
                    break;
                }
            }
        }

        return error;
    }



    public static class KalturaRuleAction extends BaseResult{
        String objectType;
        KalturaRuleActionType type;

        public KalturaRuleAction() {
        }


        public enum KalturaRuleActionType{
            DRM_POLICY("drm.DRM_POLICY"),
            @SerializedName(value = "1", alternate = {"BLOCK"})
            BLOCK("1"),
            @SerializedName("2")
            PREVIEW("2"),
            @SerializedName("3")
            LIMIT_FLAVORS("3"),
            @SerializedName("4")
            ADD_TO_STORAGE("4"),
            @SerializedName("5")
            LIMIT_DELIVERY_PROFILES("5"),
            @SerializedName("6")
            SERVE_FROM_REMOTE_SERVER("6"),
            @SerializedName("7")
            REQUEST_HOST_REGEX("7"),
            @SerializedName("8")
            LIMIT_THUMBNAIL_CAPTURE("8");

            public String value;

            KalturaRuleActionType(String value) {
                this.value = value;
            }
        }
    }

    public static class KalturaAccessControlDrmPolicyAction extends KalturaRuleAction{
        int policyId;

        public KalturaAccessControlDrmPolicyAction() {
            super();
            objectType = "KalturaAccessControlDrmPolicyAction";
        }
    }
    public static class KalturaAccessControlLimitDeliveryProfilesAction extends KalturaRuleAction{
        String deliveryProfileIds;
        boolean isBlockedList;

        public KalturaAccessControlLimitDeliveryProfilesAction() {
            super();
            objectType = "KalturaAccessControlLimitDeliveryProfilesAction";
        }
    }

    public static class KalturaAccessControlMessage {
        String message;
        String code;

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        /**
         * if message is error, returns matching {@link ErrorElement}
         * currently all messages indicates error
         * @return
         */
        public ErrorElement getErrorElement(){
            switch (code){
                case "OK":
                    return null;
                default:
                    return PhoenixErrorHelper.getErrorElement(code, message);
            }
        }
    }
}
