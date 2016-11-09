package com.kaltura.playkit.mediaproviders.mock;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.connect.ResultElement;
import com.kaltura.playkit.mediaproviders.base.ErrorElement;
import com.kaltura.playkit.mediaproviders.base.OnMediaLoadCompletion;

import java.io.FileNotFoundException;
import java.io.FileReader;

/**
 * Created by tehilarozin on 06/11/2016.
 */

public class MockMediaProvider implements MediaEntryProvider {

    private JsonObject inputJson;
    private String inputFile;
    private String id;

    private MockMediaProvider(JsonObject inputJson, String id) {
        this.inputJson = inputJson;
        this.id = id;
    }

    public MockMediaProvider(String inputFile, String id) {
        this.inputFile = inputFile;
        this.id = id;
    }

    public MockMediaProvider id(String id) {
        this.id = id;
        return this;
    }

    @Override
    public void load(final OnMediaLoadCompletion completion) {

        ErrorElement error = null;
        PKMediaEntry mediaEntry = null;

        //parse Json input to MediaEntry
        try {
            mediaEntry = inputJson != null ? getFromJson(inputJson) : getFromFile();

        } catch (FileNotFoundException | JsonSyntaxException ex) {
            error = ErrorElement.LoadError;
            mediaEntry = null;
        }

        if (mediaEntry == null) {
            error = ErrorElement.MediaNotFound;
        }

        if (completion != null) {
            final ErrorElement finalError = error;
            final PKMediaEntry finalEntry = mediaEntry;
            completion.onComplete(new ResultElement<PKMediaEntry>() {

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

    }

    private PKMediaEntry getFromFile() throws FileNotFoundException {
        JsonParser parser = new JsonParser();

        final JsonObject data = parser.parse(new FileReader(inputFile)).getAsJsonObject();
        return getFromJson(data);
    }

    private PKMediaEntry getFromJson(JsonObject data)  throws JsonSyntaxException{
        if (data.has(id)) { // data holds multiple entry objects
            return MockMediaParser.parseMedia(data.getAsJsonObject(id));

        } else if (data.has("id") && data.getAsJsonPrimitive("id").getAsString().equals(id)) { // data is the actual inputJson object
            return MockMediaParser.parseMedia(data);
        }
        return null;
    }


    static class MockMediaParser {

        static PKMediaEntry parseMedia(JsonObject mediaObject) throws JsonSyntaxException {
            return new Gson().fromJson(mediaObject, PKMediaEntry.class);
        }

    }
}
