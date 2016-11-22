package com.kaltura.playkit.backend.phoenix.services;

import android.text.TextUtils;

import com.google.gson.JsonObject;
import com.kaltura.playkit.connect.MultiRequestBuilder;
import com.kaltura.playkit.connect.RequestBuilder;

/**
 * Created by zivilan on 21/11/2016.
 */

public class BookmarkService extends PhoenixService {
    public static RequestBuilder actionAdd(String baseUrl, int partnerId, String ks, String type, String assetId, String actionType, long position, String fileId) {
        if(TextUtils.isEmpty(ks)){
            return actionAdd(baseUrl, partnerId, assetId, type, actionType, position, fileId);
        }
        return new RequestBuilder()
                .service("bookmark")
                .action("add")
                .method("POST")
                .url(baseUrl)
                .tag("bookmark-action-add")
                .params(addBookmarkGetReqParams(ks, assetId,  type, actionType, position, fileId));
    }

    static RequestBuilder actionAdd(String baseUrl, int partnerId, String assetId,  String type, String actionType, long position, String fileId) {
        return new MultiRequestBuilder(OttUserService.anonymousLogin(baseUrl, partnerId),
                new RequestBuilder()
                        .params(addBookmarkGetReqParams("{1:result:ks}", assetId, type, actionType, position, fileId)) //on http://52.210.223.65:8080/v4_0/api_v3 its without the {}
                        .addParam("service","bookmark")
                        .addParam("action","add"))
                .method("POST")
                .tag("bookmark-multi-add")
                .url(baseUrl)
                .service("multirequest");
    }

    private static JsonObject addBookmarkGetReqParams(String ks, String assetId, String type, String actionType, long position, String fileId) {
        JsonObject getParams = getPhoenixParams();
        getParams.addProperty("ks", ks);
        JsonObject bookmark = new JsonObject();
        bookmark.addProperty("objectType", "KalturaBookmark");
        bookmark.addProperty("id", assetId);
        bookmark.addProperty("type", type);
        bookmark.addProperty("position", position);
        JsonObject playerData = new JsonObject();
        playerData.addProperty("objectType", "KalturaBookmarkPlayerData");
        playerData.addProperty("action", actionType);
        playerData.addProperty("fileId", fileId);
        bookmark.add("playerData", playerData);
        getParams.add("bookmark", bookmark);

        return getParams;
    }
}
