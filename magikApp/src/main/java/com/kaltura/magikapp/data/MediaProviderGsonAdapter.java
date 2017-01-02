package com.kaltura.magikapp.data;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * Created by itanbarpeled on 27/11/2016.
 */

public class MediaProviderGsonAdapter implements JsonDeserializer <ConverterMediaProvider> {


    private final static String PARAMS = "params";
    private final static String MEDIA_PROVIDER_TYPE = "mediaProviderType";
    private final static String MEDIA_ID = "mediaId";
    private final static String ENTRIES = "entries";


    @Override
    public ConverterMediaProvider deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {


        JsonObject mediaProvider;
        ConverterMediaProvider converterMediaProvider = null;

        if (json.isJsonObject()) {

            mediaProvider = json.getAsJsonObject();


            String mediaProviderType = null;

            if (mediaProvider.has(MEDIA_PROVIDER_TYPE)) {

                mediaProviderType = mediaProvider.get(MEDIA_PROVIDER_TYPE).getAsString();
                mediaProviderType = getClass().getPackage().getName() + "." + MediaProviderMapping.valueOf(mediaProviderType).name;

                try {

                    Class mediaProviderClass = Class.forName(mediaProviderType);
                    converterMediaProvider = (ConverterMediaProvider) new Gson().fromJson(mediaProvider.get(PARAMS), mediaProviderClass);

                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

            }
        }

        return converterMediaProvider;
    }



    private enum MediaProviderMapping {

        phoenixMediaProvider("ConverterPhoenixMediaProvider"),
        kalturaOvpMediaProvider("ConverterKalturaOvpMediaProvider"),
        mockMediaProvider("ConverterMockMediaProvider");

        public String name;

        MediaProviderMapping(String name) {
            this.name = name;
        }

    }

}
