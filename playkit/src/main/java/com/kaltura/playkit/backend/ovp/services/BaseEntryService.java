package com.kaltura.playkit.backend.ovp.services;

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
        /*if(TextUtils.isEmpty(ks)){
            multiRequestBuilder.add(SessionService.widgetSession(baseUrl, partnerId));
            ks = "{1:result:ks}";
        }*/

        return multiRequestBuilder.add(list(baseUrl, ks, entryId), contextData(baseUrl, ks, entryId));
    }


    public static RequestBuilder list(String baseUrl, String ks, String entryId){
        return new RequestBuilder()
                .service("baseEntry")
                .action("list")
                .method("POST")
                .url(baseUrl)
                .tag("baseEntry-list")
                .params(getEntryListReqParams(ks, entryId));
    }

    private static JsonObject getEntryListReqParams(String ks,  String entryId){
        JsonObject params = OvpService.getOvpParams();
        params.addProperty("filter.redirectFromEntryId", entryId);
        params.addProperty("responseProfile.type", APIDefines.ResponseProfileType.IncludeFields); // in order to define which of the properties we want to "include"(1) in the response
        params.addProperty("responseProfile.fields", "id,name,duration,msDuration,flavorParamsIds");
        params.addProperty("ks", ks);
        return params;
    }

    public static RequestBuilder contextData(String baseUrl, String ks, String entryId){
        return new RequestBuilder().service("baseEntry")
                .action("getContextData")
                .method("POST")
                .url(baseUrl)
                .tag("baseEntry-getContextData")
                .params(getContextDataReqParams(ks, entryId));
    }


    private static JsonObject getContextDataReqParams(String ks, String entryId){
        JsonObject params = OvpService.getOvpParams();
        params.addProperty("entryId", entryId);
        params.addProperty("ks", ks);
        params.addProperty("contextDataParams.objectType", "KalturaEntryContextDataParams");
        return params;
    }
}
