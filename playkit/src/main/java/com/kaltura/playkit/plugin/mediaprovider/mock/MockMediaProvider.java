package com.kaltura.playkit.plugin.mediaprovider.mock;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.plugin.connect.OnCompletion;
import com.kaltura.playkit.plugin.connect.ResultElement;
import com.kaltura.playkit.plugin.mediaprovider.base.BaseMediaProvider;
import com.kaltura.playkit.plugin.mediaprovider.base.ErrorElement;
import com.kaltura.playkit.plugin.mediaprovider.base.ProviderBuilder;

import java.io.FileNotFoundException;
import java.io.FileReader;

/**
 * Created by tehilarozin on 06/11/2016.
 */

public class MockMediaProvider extends BaseMediaProvider {

    private JsonObject entry;
    private String inputFile;
    private String id;

    private MockMediaProvider(JsonObject entry) {
        this.entry = entry;
    }

    public MockMediaProvider(String inputFile, String id) {
        this.inputFile = inputFile;
        this.id = id;
    }

    @Override
    public PKMediaEntry getMediaEntry() {
        return mediaEntry;
    }

    @Override
    public void load(final OnCompletion callback) {

        ErrorElement error = null;
        PKMediaEntry mediaEntry = null;

        //parse entry to MediaEntry
        if (entry != null) {
            mediaEntry = MockMediaParser.parseMedia(entry);

        } else {
            JsonParser parser = new JsonParser();
            try {
                final JsonElement element = parser.parse(new FileReader(inputFile));
                JsonObject entryObject;
                if (element.getAsJsonObject().has("entries")) {
                    entryObject = element.getAsJsonObject().getAsJsonObject("entries").getAsJsonObject(id);
                } else {
                    entryObject = element.getAsJsonObject();
                }

                if(entryObject != null) {
                    mediaEntry = MockMediaParser.parseMedia(entryObject);
                } else {
                    error = ErrorElement.MediaNotFound;
                }

            } catch (FileNotFoundException ex) {
                error = ErrorElement.LoadError;
            }
        }

        if(callback!=null){
            final ErrorElement finalError = error;
            final PKMediaEntry finalEntry = mediaEntry;
            callback.onComplete(new ResultElement<PKMediaEntry>() {

                @Override
                public PKMediaEntry getResponse() {
                    return finalEntry;
                }

                @Override
                public boolean isSuccess() {
                    return finalEntry != null;
                }

                @Override
                public ErrorElement getError() {
                    return finalError;
                }
            });
        }

        /*requestHandler.getMockMedia(id, new OnCompletion<ResponseElement>() {
            @Override
            public void onComplete(ResponseElement response) {
                if(response.isSuccess()){
                    MediaEntry mediaEntry = MockMediaParser.parseMedia(response.getResponse());
                    if(callback!=null){
                        callback.onComplete(mediaEntry);
                    }
                } else
            }
        });*/
    }


    public static class Builder implements ProviderBuilder {

        JsonObject data;
        private String id;
        private String file;

        public Builder setFile(String file) {
            this.file = file;
            return this;
        }

        public Builder setData(JsonObject data) {
            this.data = data;
            return this;
        }

        public Builder setId(String id) {
            this.id = id;
            return this;
        }


        @Override
        public MediaEntryProvider build() {
            /*data or file*/
            if (data != null) {
                return new MockMediaProvider(data.get(id).getAsJsonObject());
            } else {
                return new MockMediaProvider(file, id);
            }
        }
    }


    static class MockMediaParser {

        static PKMediaEntry parseMedia(JsonObject mediaObject) {
            PKMediaEntry mediaEntry = new Gson().fromJson(mediaObject, PKMediaEntry.class);
            return mediaEntry;
        }


    }
}
