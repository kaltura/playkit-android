package com.kaltura.playkit.backend.ovp.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kaltura.playkit.backend.ovp.APIDefines;
import com.kaltura.playkit.connect.MultiRequestBuilder;
import com.kaltura.playkit.connect.RequestBuilder;


/**
 * Created by tehilarozin on 13/11/2016.
 */

public class BaseEntryService extends OvpService {

    public static RequestBuilder entryInfo(String baseUrl, String ks, String entryId) {

        MultiRequestBuilder multiRequestBuilder = (MultiRequestBuilder) OvpService.getMultirequest(baseUrl, ks).tag("mediaAsset-multi-get");
        return multiRequestBuilder.add(list(baseUrl, ks, entryId), contextData(baseUrl, ks, entryId));
    }

    public static RequestBuilder list(String baseUrl, String ks, String entryId) {
        return new RequestBuilder()
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
        baseEntryListParams.responseProfile.fields = "id,name,dataUrl,duration,msDuration,flavorParamsIds,mediaType";
        baseEntryListParams.responseProfile.type = APIDefines.ResponseProfileType.IncludeFields;

        return new Gson().toJsonTree(baseEntryListParams).getAsJsonObject();
    }

    public static RequestBuilder contextData(String baseUrl, String ks, String entryId) {
        return new RequestBuilder().service("baseEntry")
                .action("getContextData")
                .method("POST")
                .url(baseUrl)
                .tag("baseEntry-getContextData")
                .params(getContextDataReqParams(ks, entryId));
    }


    private static JsonObject getContextDataReqParams(String ks, String entryId) {
        JsonObject params = OvpService.getOvpParams();
        params.addProperty("entryId", entryId);
        params.addProperty("ks", ks);
        params.add("contextDataParams", new JsonObject());
        return params;
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
