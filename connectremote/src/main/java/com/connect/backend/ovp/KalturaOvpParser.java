package com.connect.backend.ovp;

import android.support.annotation.NonNull;

import com.connect.backend.BaseResult;
import com.connect.backend.ovp.data.OvpResultAdapter;
import com.connect.utils.GsonParser;
import com.connect.utils.RuntimeTypeAdapterFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;

/**
 * Created by tehilarozin on 24/11/2016.
 */

public class KalturaOvpParser {

    public static Object parse(String response, Class... types) throws JsonSyntaxException {
        JsonParser parser = new JsonParser();
        JsonElement resultElement = parser.parse(response);
        return GsonParser.parse(resultElement, new GsonBuilder().registerTypeHierarchyAdapter(BaseResult.class, new OvpResultAdapter()).create(), types);
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

        if (resultElement.isJsonObject()) {
            return (T) GsonParser.parseObject(resultElement, BaseResult.class, getGson());
        } else if (resultElement.isJsonArray()) {
            return (T) GsonParser.parseArray(resultElement, getGson(), BaseResult.class);
        } else if (resultElement.isJsonPrimitive()) {
            return (T) resultElement.getAsJsonPrimitive().getAsString();
        }
        return null;
    }

    @NonNull
    public static Gson getGson() {
        return new GsonBuilder().registerTypeHierarchyAdapter(BaseResult.class, new OvpResultAdapter()).create();
    }

    public static Gson getRuntimeGson(Class clz) {
        RuntimeTypeAdapterFactory adapterFactory = null;
        switch (clz.getSimpleName()){
            case "KalturaPlaybackContext":
                /*!! for now - since we are not using the parsed action objects, there's no need to use this runtime factory
                  !! all items in the "actions" list will be of the base class type "KalturaRuleAction"
                  !! once needed - set adapterFactory to the commented code and make sure to register each of the subclasses that may
                  !! return in the response */
                adapterFactory = null;
                        /*RuntimeTypeAdapterFactory
                        .of(KalturaPlaybackContext.KalturaRuleAction.class, "objectType")
                        .registerSubtype(KalturaPlaybackContext.KalturaAccessControlDrmPolicyAction.class, "KalturaAccessControlDrmPolicyAction")
                        .registerSubtype(KalturaPlaybackContext.KalturaAccessControlDrmPolicyAction.class, "KalturaAccessControlLimitDeliveryProfilesAction");*/
        }

        GsonBuilder gsonBuilder = new GsonBuilder();
        if(adapterFactory != null){
            gsonBuilder.registerTypeAdapterFactory(adapterFactory);
        }
        return gsonBuilder.create();
    }

    //public static void registerRuntimeAdapter()
}
