package com.kaltura.playkit.backend.ovp;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.kaltura.playkit.backend.ovp.data.OvpResultAdapter;
import com.kaltura.playkit.backend.BaseResult;
import com.kaltura.playkit.connect.GsonParser;

import java.util.List;

/**
 * Created by tehilarozin on 24/11/2016.
 */

public class KalturaOvpParser {

    public static Object parse(String response, Class...types) throws JsonSyntaxException {
        JsonParser parser = new JsonParser();
        JsonElement resultElement = parser.parse(response);
        return GsonParser.parse(resultElement, new GsonBuilder().registerTypeHierarchyAdapter(BaseResult.class, new OvpResultAdapter()).create(), types);
    }

    public static BaseResult parseObject(String response) throws JsonSyntaxException {
        JsonParser parser = new JsonParser();
        JsonElement resultElement = parser.parse(response);//.get("result");
        if(resultElement.isJsonObject()){
            return GsonParser.parseObject(resultElement, BaseResult.class, new GsonBuilder().registerTypeHierarchyAdapter(BaseResult.class, new OvpResultAdapter()).create());
        }
        return null;
    }

    public static <T extends BaseResult> List<T> parseArray(String response) throws JsonSyntaxException {

        JsonParser parser = new JsonParser();
        JsonElement resultElement = parser.parse(response);//.get("result");

        if (resultElement.isJsonArray()) {
            return GsonParser.parseArray(resultElement, BaseResult.class, new GsonBuilder().registerTypeHierarchyAdapter(BaseResult.class, new OvpResultAdapter()).create());
        }
        return null;
    }

}
