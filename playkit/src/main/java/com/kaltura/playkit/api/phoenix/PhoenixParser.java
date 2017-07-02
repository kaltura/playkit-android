/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 * 
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 * 
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit.api.phoenix;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.kaltura.netkit.connect.response.BaseResult;
import com.kaltura.netkit.utils.GsonParser;
import com.kaltura.playkit.api.phoenix.model.OttResultAdapter;

/**
 * @hide
 */

public class PhoenixParser {

    public static <T> T parseObject(String json, Class type) throws JsonSyntaxException {
        return (T) new GsonBuilder().registerTypeHierarchyAdapter(type, new OttResultAdapter()).create().fromJson(json, type);
    }

    public static Object parse(String response, Class...types) throws JsonSyntaxException {
        JsonParser parser = new JsonParser();
        JsonElement resultElement = parser.parse(response);

        if(resultElement.isJsonObject() && resultElement.getAsJsonObject().has("result")) {
            resultElement = resultElement.getAsJsonObject().get("result");
        }
        return GsonParser.parse(resultElement, new GsonBuilder().registerTypeHierarchyAdapter(BaseResult.class, new OttResultAdapter()).create(), types);
    }

    public static <T> T parse(JsonReader reader) throws JsonSyntaxException {
        JsonParser parser = new JsonParser();
        return parse(parser.parse(reader));
    }

    public static <T> T parse(String response) throws JsonSyntaxException {
        JsonParser parser = new JsonParser();
        return parse(parser.parse(response));
    }

    public static <T> T parse(JsonElement resultElement) throws JsonSyntaxException {

        if(resultElement.isJsonObject() && resultElement.getAsJsonObject().has("result")){
            resultElement = resultElement.getAsJsonObject().get("result");
        }

        if(resultElement.isJsonObject()){
            return (T) GsonParser.parseObject(resultElement, BaseResult.class, new GsonBuilder().registerTypeHierarchyAdapter(BaseResult.class, new OttResultAdapter()).create());
        } else if (resultElement.isJsonArray()){
            return (T) GsonParser.parseArray(resultElement, new GsonBuilder().registerTypeHierarchyAdapter(BaseResult.class, new OttResultAdapter()).create(), BaseResult.class);
        } else if (resultElement.isJsonPrimitive()){
            return (T) resultElement.getAsJsonPrimitive().getAsString();
        }
        return null;
    }

}
