package com.kaltura.playkit.backend.ovp.data;


import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by tehilarozin on 01/12/2016.
 */


public class FlavorAssetsFilter /*extends ArrayList<KalturaFlavorAsset>*/ {

    public interface Filterable{
        <V> V getMemberValue(String name);
    }

    public static <T extends Filterable> List<T> filter(List<T> source, String filterBy, String... filterValues) {
        return filter(source, filterBy, Arrays.asList(filterValues));
    }

    @NonNull
    public static <T extends Filterable> List<T> filter(List<T> source, String filterBy, List<String> filterValues) {
        List<T> filtered = new ArrayList<>();

        if (source.size() == 0 || filterValues.size() == 0) {
            return filtered;
        }

        for (int idx = 0; idx < source.size(); idx++) {
            for (String value : filterValues)
                if (source.get(idx).getMemberValue(filterBy).equals(value)) {
                    filtered.add(source.get(idx));
                }
        }

        return filtered;
    }

    public static class ArrayAdapter<T> extends TypeAdapter<List<T>> {
        private Class<T> adapterclass;

        public ArrayAdapter(Class<T> adapterclass) {

            this.adapterclass = adapterclass;
        }

        public List<T> read(JsonReader reader) throws IOException {


            List<T> list = new ArrayList<T>();

            Gson gson = new Gson();

            if (reader.peek() == JsonToken.BEGIN_OBJECT) {

                T inning = (T) gson.fromJson(reader, adapterclass);
                list.add(inning);

            } else if (reader.peek() == JsonToken.BEGIN_ARRAY) {

                reader.beginArray();
                while (reader.hasNext()) {
                    T inning = (T) gson.fromJson(reader, adapterclass);
                    list.add(inning);
                }
                reader.endArray();

            } else {
                reader.skipValue();
            }

            return list;
        }

        public void write(JsonWriter writer, List<T> value) throws IOException {

        }

    }

    public static class ArrayAdapterFactory implements TypeAdapterFactory {

        @SuppressWarnings({"unchecked"})
        @Override
        public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {

            ArrayAdapter typeAdapter = null;
            try {
                if (type.getRawType() == List.class) {

                    typeAdapter = new ArrayAdapter(
                            (Class) ((ParameterizedType) type.getType())
                                    .getActualTypeArguments()[0]);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return typeAdapter;
        }
    }
}