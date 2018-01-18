package com.kaltura.playkit.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GsonReader {

    private static final JsonParser sharedParser = new JsonParser();
    private final JsonObject jsonObject;

    public GsonReader(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }
    
    public JsonArray getArray(String key) {
        return getArray(jsonObject, key);
    }

    public JsonObject getObject(String key) {
        return getObject(jsonObject, key);
    }

    public String getString(String key) {
        return getString(jsonObject, key);
    }

    public Boolean getBoolean(String key) {
        return getBoolean(jsonObject, key);
    }

    public Integer getInteger(String key) {
        return getInteger(jsonObject, key);
    }

    public GsonReader getReader(String key) {
        return withObject(getObject(key));
    }
    
    public static JsonObject parseObject(String json) {
        JsonElement element = sharedParser.parse(json);
        return element.isJsonObject() ? element.getAsJsonObject() : null;
    }
    
    public static GsonReader withObject(JsonObject object) {
        return object != null ? new GsonReader(object) : null;
    }
    
    public static GsonReader withString(String json) {
        JsonObject jsonObject = parseObject(json);
        return jsonObject != null ? new GsonReader(jsonObject) : null;
    }
    
    public static GsonReader getReader(JsonObject json, String key) {
        JsonObject object = getObject(json, key);
        return withObject(object);
    }

    public static JsonArray getArray(JsonObject json, String key) {
        final JsonElement jsonElement = json.get(key);
        if (jsonElement != null && jsonElement.isJsonArray()) {
            return jsonElement.getAsJsonArray();
        }
        return null;
    }
    
    public static JsonObject getObject(JsonObject json, String key) {
        final JsonElement jsonElement = json.get(key);
        if (jsonElement != null && jsonElement.isJsonObject()) {
            return jsonElement.getAsJsonObject();
        }
        return null;
    }

    public static String getString(JsonObject json, String key) {
        final JsonElement jsonElement = json.get(key);
        if (jsonElement != null && jsonElement.isJsonPrimitive()) {
            return jsonElement.getAsString();
        }
        return null;
    }

    public static Boolean getBoolean(JsonObject json, String key) {
        final JsonElement jsonElement = json.get(key);
        if (jsonElement != null && jsonElement.isJsonPrimitive()) {
            return jsonElement.getAsBoolean();
        }
        return null;
    }

    public static Integer getInteger(JsonObject json, String key) {
        final JsonElement jsonElement = json.get(key);
        if (jsonElement != null && jsonElement.isJsonPrimitive()) {
            return jsonElement.getAsInt();
        }
        return null;
    }
}
