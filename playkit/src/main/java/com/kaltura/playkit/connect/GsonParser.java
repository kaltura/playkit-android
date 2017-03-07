package com.kaltura.playkit.connect;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.$Gson$Types;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

/**
 * @hide
 */

public class GsonParser {

    public static Object parse(JsonElement jsonElement, Gson gson, @NonNull Class... types) throws JsonSyntaxException {
        return types.length == 0 ? null :
                jsonElement.isJsonArray() ? parseArray(jsonElement, gson, types) :
                        gson.fromJson(jsonElement, types[0]);
    }

    @NonNull
    public static List<Object> parseArray(JsonElement jsonElement, Gson gson, Class... types) throws JsonSyntaxException {
        final ArrayList<Object> responsesObjects = new ArrayList<>();
        JsonArray responses = jsonElement.getAsJsonArray();

        Class claz;
        for (int i = 0, tIdx = 0; i < responses.size(); i++) {
            claz = types[tIdx];
            responsesObjects.add(parse(responses.get(i), gson, claz));
            if (tIdx < types.length - 1) { // in case types size is smaller than parsable objects, parse rest with last type
                tIdx++;
            }
        }
        return responsesObjects;
    }


    public static <T> T parseObject(JsonElement jsonElement, Class<T> clz, @Nullable Gson gson) throws JsonSyntaxException {
        if (jsonElement != null) {
            if (gson == null) {
                gson = new Gson();
            }
            return gson.fromJson(jsonElement, clz);
        }
        return null;
    }

    public static <T> ArrayList<T> parseArray(JsonElement jsonElement, Class clz, Gson gson) throws JsonSyntaxException {

        if (jsonElement.isJsonArray()) {
            if (gson == null) {
                gson = new Gson();
            }

            ParameterizedType type = $Gson$Types.newParameterizedTypeWithOwner(null, ArrayList.class, clz);
//                Type type = ParameterizedTypeImpl.make(ArrayList.class, new Type[]{clz}, null);
            return gson.fromJson(jsonElement, type);
        }

        return new ArrayList<T>();
    }

    public static JsonElement toJson(String jsonString) throws JsonSyntaxException{
        JsonParser parser = new JsonParser();
        return parser.parse(jsonString);
    }
}
