package com.connect.backend.phoenix.services;

import com.google.gson.JsonObject;
import com.connect.backend.phoenix.PhoenixRequestBuilder;

/**
 * Created by zivilan on 21/11/2016.
 */

public class BookmarkService extends PhoenixService {

    public static PhoenixRequestBuilder actionAdd(String baseUrl, int partnerId, String ks, String type, String assetId, String actionType, long position, String fileId) {
        return new PhoenixRequestBuilder()
                .service("bookmark")
                .action("add")
                .method("POST")
                .url(baseUrl)
                .tag("bookmark-action-add")
                .params(addBookmarkGetReqParams(ks, assetId,  type, actionType, position, fileId));
    }

    private static JsonObject addBookmarkGetReqParams(String ks, String assetId, String type, String actionType, long position, String fileId) {
        JsonObject playerData = new JsonObject();
        playerData.addProperty("objectType", "KalturaBookmarkPlayerData");
        playerData.addProperty("action", actionType);
        playerData.addProperty("fileId", fileId);

        JsonObject bookmark = new JsonObject();
        bookmark.addProperty("objectType", "KalturaBookmark");
        bookmark.addProperty("id", assetId);
        bookmark.addProperty("type", type);
        bookmark.addProperty("position", position);
        bookmark.add("playerData", playerData);

        JsonObject getParams = new JsonObject();
        getParams.addProperty("ks", ks);
        getParams.add("bookmark", bookmark);

        return getParams;
    }
}
