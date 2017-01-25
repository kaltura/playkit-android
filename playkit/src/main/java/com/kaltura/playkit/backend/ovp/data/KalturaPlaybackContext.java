package com.kaltura.playkit.backend.ovp.data;

import com.google.gson.annotations.SerializedName;
import com.kaltura.playkit.backend.BaseResult;

import java.util.ArrayList;

/**
 * @hide
 */

public class KalturaPlaybackContext extends KalturaEntryContextDataResult {

    ArrayList<KalturaPlaybackSource> sources;
    ArrayList<KalturaRuleAction> actions;
    ArrayList<KalturaAccessControlMessage> messages;


    public KalturaPlaybackContext() {
    }

    public KalturaPlaybackContext(KalturaEntryContextDataResult contextDataResult) {
        flavorAssets = contextDataResult.flavorAssets;
    }

    public ArrayList<KalturaPlaybackSource> getSources() {
        return sources;
    }

    public ArrayList<KalturaAccessControlMessage> getMessages() {
        return messages;
    }

    public ArrayList<KalturaRuleAction> getActions() {
        return actions;
    }

    public static class KalturaRuleAction extends BaseResult{
        String objectType;
        KalturaRuleActionType type;

        public KalturaRuleAction() {
        }


        public enum KalturaRuleActionType{
            DRM_POLICY("DRM_POLICY"),
            @SerializedName("1")
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

            public String type;

            KalturaRuleActionType(String type) {
                this.type = type;
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

    public class KalturaAccessControlMessage {
        String message;
        String code;

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}
