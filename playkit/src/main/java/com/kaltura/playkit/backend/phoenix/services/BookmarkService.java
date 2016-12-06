package com.kaltura.playkit.backend.phoenix.services;

import com.google.gson.JsonObject;
import com.kaltura.playkit.connect.RequestBuilder;

/**
 * Created by zivilan on 21/11/2016.
 */

public class BookmarkService extends PhoenixService {
    public static RequestBuilder actionAdd(String baseUrl, int partnerId, String ks, String type, String assetId, String actionType, long position, String fileId) {
        return new RequestBuilder()
                .service("bookmark")
                .action("add")
                .method("POST")
                .url(baseUrl)
                .tag("bookmark-action-add")
                .params(addBookmarkGetReqParams(ks, assetId,  type, actionType, position, fileId));
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
