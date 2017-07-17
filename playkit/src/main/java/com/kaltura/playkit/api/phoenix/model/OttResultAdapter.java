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

package com.kaltura.playkit.api.phoenix.model;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.kaltura.netkit.connect.response.BaseResult;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.PKLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

/**
 * Enables parsing of {@link BaseResult} extending classes (such as {@link com.kaltura.playkit.api.phoenix.model.AssetResult} in a way
 * the we'll have the "result" object, and an {@link ErrorElement} object. in case of server error response - the error located
 * under {@link BaseResult#error} member, in case of success the result will be available in the specific class member.
 * (exp: {@link com.kaltura.playkit.api.phoenix.model.AssetResult#asset})
 *
 * usage: new GsonBuilder().registerTypeAdapter(AssetResult.class, new OttResultAdapter()).create().fromJson(json, AssetResult.class);
 * @hide
 */
public class OttResultAdapter implements JsonDeserializer<BaseResult> {
    @Override
    public BaseResult deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

        JsonObject result = json.getAsJsonObject();
        if(result.has("result")){
            result = result.getAsJsonObject("result");
        }

        BaseResult baseResult = null;

        if(result != null && result.has("error")){

            ErrorElement error = new Gson().fromJson(result.get("error"), ErrorElement.class);

            // trying to find constructor that excepts ErrorElement in order to return an object of type "typeOfT" even on error.
            try {
                Constructor<? extends Type> constructor = typeOfT.getClass().getConstructor(ErrorElement.class);

                if (constructor != null) {
                    baseResult = (BaseResult) constructor.newInstance(error);
                }
            } catch (NoSuchMethodException e) {
                // do nothing
            } catch (IllegalAccessException e) {
                // do nothing
            } catch (InstantiationException e) {
                // do nothing
            } catch (InvocationTargetException e) {
                // do nothing - next code section will handle this
            }

            if (baseResult == null) {
                baseResult = new BaseResult(error);
            }

        } else if(result != null && result.has("objectType")){

            String objectType=  result.getAsJsonPrimitive("objectType").getAsString();
            if(objectType.equals("KalturaAPIException")) {
                baseResult = new BaseResult(new Gson().fromJson(result, ErrorElement.class));
            } else {
                try {
                    String clzName = getClass().getPackage().getName() + "." + objectType;
                    Class clz = Class.forName(clzName);
                    baseResult = (BaseResult) new Gson().fromJson(result, clz);
                } catch (ClassNotFoundException e) {
                    PKLog.e("OttResultAdapter","can't find class "+objectType+ " in the provided package\n ");
                    e.printStackTrace();
                }
            }
        } else {
            baseResult = new Gson().fromJson(result, typeOfT);
        }

        return baseResult;
    }
}
