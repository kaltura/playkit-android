package com.kaltura.playkit.backend.ovp.data;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.kaltura.playkit.backend.BaseResult;
import com.kaltura.playkit.backend.PrimitiveResult;
import com.kaltura.playkit.backend.ovp.KalturaOvpParser;
import com.kaltura.playkit.connect.ErrorElement;

import java.lang.reflect.Type;

/**
 * Enables parsing of {@link BaseResult} extending classes (such as {@link com.kaltura.playkit.backend.phoenix.data.AssetResult} in a way
 * the we'll have the "result" object, and an {@link ErrorElement} object. in case of server error response - the error located
 * under {@link BaseResult#error} member, in case of success the result will be available in the specific class member.
 * (exp: {@link com.kaltura.playkit.backend.phoenix.data.AssetResult#asset})
 *
 * usage: new GsonBuilder().registerTypeAdapter(AssetResult.class, new OttResultAdapter()).create().fromJson(json, AssetResult.class);
 * @hide
 */
public class OvpResultAdapter implements JsonDeserializer<BaseResult> {
    @Override
    public BaseResult deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

        if(json.isJsonPrimitive()){
            return new PrimitiveResult(json.getAsString());
        }


        JsonObject result = json.getAsJsonObject();
        BaseResult baseResult = null/*= new Gson().fromJson(json, typeOfT)*/;

        if(result != null && result.has("objectType")){
            String objectType=  result.getAsJsonPrimitive("objectType").getAsString();
            if(objectType.equals("KalturaAPIException")) {
                baseResult = new BaseResult(new Gson().fromJson(result, ErrorElement.class));
            } else {
                try {
                    String clzName  = getClass().getPackage().getName()+"."+objectType;
                    Class clz = Class.forName(clzName);

                    baseResult = (BaseResult) KalturaOvpParser.getRuntimeGson(clz).fromJson(json, clz);

                } catch (ClassNotFoundException | JsonSyntaxException e) {
                    e.printStackTrace();
                    throw new JsonParseException("Adaptor failed to parse result, "+e.getMessage());
                }
            }
        } else {
            baseResult = KalturaOvpParser.getRuntimeGson(typeOfT.getClass()).fromJson(json, typeOfT);;
        }
        return baseResult;
    }
}
