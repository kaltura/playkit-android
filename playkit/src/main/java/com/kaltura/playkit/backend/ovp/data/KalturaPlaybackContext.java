package com.kaltura.playkit.backend.ovp.data;

import com.kaltura.playkit.backend.BaseResult;

import java.util.ArrayList;

/**
 * Created by tehilarozin on 02/11/2016.
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
        String type;

        public KalturaRuleAction() {
            //objectType = "KalturaRuleAction";
        }
    }

    public static class KalturaAccessControlDrmPolicyAction extends KalturaRuleAction{
        int policyId;

        public KalturaAccessControlDrmPolicyAction() {
            super();
            objectType = "KalturaAccessControlDrmPolicyAction";

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
