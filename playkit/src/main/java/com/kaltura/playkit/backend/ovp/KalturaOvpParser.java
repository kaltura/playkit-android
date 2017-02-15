package com.kaltura.playkit.backend.ovp;

import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.kaltura.playkit.backend.BaseResult;
import com.kaltura.playkit.backend.ovp.data.OvpResultAdapter;
import com.kaltura.playkit.connect.GsonParser;
import com.kaltura.playkit.utils.RuntimeTypeAdapterFactory;

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
            case "BasePlaybackContext":
                /*!! for now - since we are not using the parsed action objects, there's no need to use this runtime factory
                  !! all items in the "actions" list will be of the base class type "KalturaRuleAction"
                  !! once needed - set adapterFactory to the commented code and make sure to register each of the subclasses that may
                  !! return in the response */
                adapterFactory = null;
                        /*RuntimeTypeAdapterFactory
                        .of(BasePlaybackContext.KalturaRuleAction.class, "objectType")
                        .registerSubtype(BasePlaybackContext.KalturaAccessControlDrmPolicyAction.class, "KalturaAccessControlDrmPolicyAction")
                        .registerSubtype(BasePlaybackContext.KalturaAccessControlDrmPolicyAction.class, "KalturaAccessControlLimitDeliveryProfilesAction");*/
        }

        GsonBuilder gsonBuilder = new GsonBuilder();
        if(adapterFactory != null){
            gsonBuilder.registerTypeAdapterFactory(adapterFactory);
        }
        return gsonBuilder.create();
    }

    //public static void registerRuntimeAdapter()
}
