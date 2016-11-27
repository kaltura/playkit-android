package com.kaltura.playkit.backend.phoenix.data;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.kaltura.playkit.backend.BaseResult;
import com.kaltura.playkit.connect.GsonParser;

import java.util.List;

/**
 * Created by tehilarozin on 15/11/2016.
 */

public class PhoenixParser {

    public static <T> T parseResult(String json, Class type) throws JsonSyntaxException {
        return (T) new GsonBuilder().registerTypeHierarchyAdapter(type, new OttResultAdapter()).create().fromJson(json, type);
    }

    /*public static List<Object> parseMultiresponse(String response, int parseFromIdx, @NonNull Class... types) throws JsonSyntaxException {

        JsonParser parser = new JsonParser();
        JsonElement resultElement = parser.parse(response).getAsJsonObject().get("result");
        ArrayList<Object> responsesObjects = new ArrayList<>();

        if (resultElement.isJsonArray()) {
            *//*JsonArray responses = resultElement.getAsJsonArray();
            Gson gson = new GsonBuilder().registerTypeAdapter(BaseResult.class, new OttResultAdapter()).create();

            Class claz;
            for (int i = parseFromIdx, tIdx = 0; i < responses.size(); i++) {
                claz = types[tIdx];
                responsesObjects.add(gson.fromJson(responses.get(i), claz));
                if (tIdx < types.length - 1) { // in case types size is smaller than parsable objects, parse rest with last type
                    tIdx++;
                }
            }*//*
            return GsonParser.parseArray(resultElement, BaseResult.class, new OttResultAdapter());
        }
        return responsesObjects;
    }*/

    public static Object parse(String response, Class...types) throws JsonSyntaxException {
        JsonParser parser = new JsonParser();
        JsonElement resultElement = parser.parse(response).getAsJsonObject().get("result");
        return GsonParser.parse(resultElement, new GsonBuilder().registerTypeHierarchyAdapter(BaseResult.class, new OttResultAdapter()).create(), types);
    }

    public static <T extends BaseResult> T parseObject(String response) throws JsonSyntaxException {
        JsonParser parser = new JsonParser();
        JsonElement resultElement = parser.parse(response).getAsJsonObject().get("result");
        if(resultElement.isJsonObject()){
            return (T) GsonParser.parseObject(resultElement, BaseResult.class, new GsonBuilder().registerTypeHierarchyAdapter(BaseResult.class, new OttResultAdapter()).create());
        }
        return null;
    }

    public static <T extends BaseResult> List<T> parseArray(String response) throws JsonSyntaxException {

        JsonParser parser = new JsonParser();
        JsonElement resultElement = parser.parse(response);//.get("result");

        if (resultElement.isJsonArray()) {
            return GsonParser.parseArray(resultElement, BaseResult.class, new GsonBuilder().registerTypeAdapter(BaseResult.class, new OttResultAdapter()).create());
        }
        return null;
    }

}
