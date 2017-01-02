package com.kaltura.magikapp.data;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;


import java.lang.reflect.Type;

import static com.kaltura.magikapp.data.ConverterAddon.AddonType.GOOGLE_CAST;
import static com.kaltura.magikapp.data.ConverterGoogleCast.ReceiverEnvironmentType.RECEIVER_OVP_ENVIRONMENT;
import static com.kaltura.magikapp.data.ConverterGoogleCast.ReceiverEnvironmentType.RECEIVER_TVPAPI_ENVIRONMENT;


/**
 * Created by itanbarpeled on 16/12/2016.
 */

public class AddonsGsonAdapter implements JsonDeserializer<ConverterAddon> {


    private static final String ADDON_NAME = "addonName";
    private static final String PARAMS = "params";
    private static final String RECEIVER_ENVIRONMENT_TYPE = "receiverEnvironmentType";
    private static final String CAST_PACKAGE = "cast";


    private enum AddonMapping {

        googleCast("ConverterGoogleCast");

        public String name;

        AddonMapping(String name) {
            this.name = name;
        }

    }

    private enum GoogleCastMapping {

        tvpapi("ConverterTvpapiCast"),
        ovp("ConverterOvpCast");

        public String name;

        GoogleCastMapping(String name) {
            this.name = name;
        }

    }


    @Override
    public ConverterAddon deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {


        JsonObject addon;
        ConverterAddon converterAddon = null;


        if (json.isJsonObject()) {

            addon = json.getAsJsonObject();
            String addonName = null;

            if (addon.has(ADDON_NAME)) {

                addonName = addon.get(ADDON_NAME).getAsString();

                switch (AddonMapping.valueOf(addonName)) {

                    case googleCast:
                        converterAddon = getConverterGoogleCast(addon);
                        break;

                }
            }
        }

        return converterAddon;

    }



    private ConverterGoogleCast getConverterGoogleCast(JsonObject addon) {

        ConverterGoogleCast converterGoogleCast = null;

        if (addon.has(PARAMS)) {

            JsonObject addonParams = addon.getAsJsonObject(PARAMS);

            if (addonParams.has(RECEIVER_ENVIRONMENT_TYPE)) {

                String receiverEnvironmentType = addonParams.get(RECEIVER_ENVIRONMENT_TYPE).getAsString();
                GoogleCastMapping castMapping = GoogleCastMapping.valueOf(receiverEnvironmentType);
                String castClassName = getClass().getPackage().getName() + "." + CAST_PACKAGE + "." + castMapping.name;

                try {

                    Class castClass = Class.forName(castClassName);
                    converterGoogleCast = (ConverterGoogleCast) new Gson().fromJson(addonParams.get(PARAMS), castClass);

                    setCastConverterName(converterGoogleCast, castMapping);

                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }

        return converterGoogleCast;

    }


    private void setCastConverterName(ConverterGoogleCast converterGoogleCast, GoogleCastMapping castMapping) {

        converterGoogleCast.setAddonType(GOOGLE_CAST);

        ConverterGoogleCast.ReceiverEnvironmentType environmentType = null;

        switch (castMapping) {

            case ovp:
                environmentType = RECEIVER_OVP_ENVIRONMENT;
                break;

            case tvpapi:
                environmentType = RECEIVER_TVPAPI_ENVIRONMENT;
                break;
        }

        converterGoogleCast.setReceiverEnvironmentType(environmentType);

    }


}
