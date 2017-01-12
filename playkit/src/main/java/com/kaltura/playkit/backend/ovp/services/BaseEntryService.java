package com.kaltura.playkit.backend.ovp.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kaltura.playkit.backend.ovp.APIDefines;
import com.kaltura.playkit.backend.ovp.OvpRequestBuilder;


/**
 * Created by tehilarozin on 13/11/2016.
 */

public class BaseEntryService extends OvpService {

    /*public static RequestBuilder entryInfo(String baseUrl, String ks, int partnerId, String entryId) {

        MultiRequestBuilder multiRequestBuilder = (MultiRequestBuilder) OvpService.getMultirequest(baseUrl, ks, partnerId)
                .tag("mediaAsset-multi-get");

        if(TextUtils.isEmpty(ks)){
            multiRequestBuilder.add(OvpSessionService.anonymousSession(baseUrl, partnerId));

            ks = "{1:result:ks}";
        }

        return multiRequestBuilder.add(list(baseUrl, ks, entryId),
                getPlaybackContext(baseUrl, ks, entryId),
                MetaDataService.list(baseUrl,ks,entryId));
    }
*/
    public static OvpRequestBuilder list(String baseUrl, String ks, String entryId) {
        return new OvpRequestBuilder()
                .service("baseEntry")
                .action("list")
                .method("POST")
                .url(baseUrl)
                .tag("baseEntry-list")
                .params(getEntryListReqParams(ks, entryId));
    }

    private static JsonObject getEntryListReqParams(String ks, String entryId) {

        BaseEntryListParams baseEntryListParams = new BaseEntryListParams(ks);
        baseEntryListParams.filter.redirectFromEntryId = entryId;
        baseEntryListParams.responseProfile.fields = "id,name,dataUrl,duration,msDuration,flavorParamsIds,mediaType,type,tags";
        baseEntryListParams.responseProfile.type = APIDefines.ResponseProfileType.IncludeFields;

        return new Gson().toJsonTree(baseEntryListParams).getAsJsonObject();
    }

    public static OvpRequestBuilder getContextData(String baseUrl, String ks, String entryId) {
        JsonObject params = new JsonObject();
        params.addProperty("entryId", entryId);
        params.addProperty("ks", ks);

        JsonObject contextDataParams = new JsonObject();
        contextDataParams.addProperty("objectType","KalturaContextDataParams");
        params.add("contextDataParams", contextDataParams);

        return new OvpRequestBuilder().service("baseEntry")
                .action("getContextData")
                .method("POST")
                .url(baseUrl)
                .tag("baseEntry-getContextData")
                .params(params);
    }

    public static OvpRequestBuilder getPlaybackContext(String baseUrl, String ks, String entryId) {
        JsonObject params = new JsonObject();
        params.addProperty("entryId", entryId);
        params.addProperty("ks", ks);
        JsonObject contextDataParams = new JsonObject();
        contextDataParams.addProperty("objectType","KalturaContextDataParams");
        params.add("contextDataParams", contextDataParams);

        return new OvpRequestBuilder().service("baseEntry")
                .action("getPlaybackContext")
                .method("POST")
                .url(baseUrl)
                .tag("baseEntry-getPlaybackContext")
                .params(params);
    }





    static class BaseEntryListParams {
        String ks;
        Filter filter;
        ResponseProfile responseProfile;

        public BaseEntryListParams(String ks) {
            this.ks = ks;
            this.filter = new Filter();
            this.responseProfile = new ResponseProfile();
        }

        class Filter {
            String redirectFromEntryId;
        }
    }

}
