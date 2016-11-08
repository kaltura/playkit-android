package com.kaltura.playkit.plugins.mediaprovider.mock;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.plugins.connect.ResultElement;
import com.kaltura.playkit.plugins.mediaprovider.base.ErrorElement;
import com.kaltura.playkit.plugins.mediaprovider.base.OnMediaLoadCompletion;

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
        PKMediaEntry mediaEntry;JsonParser parser = new JsonParser();

        final JsonObject data = parser.parse(new FileReader(inputFile)).getAsJsonObject();
        mediaEntry = getFromJson(data.has("entries") ? data.getAsJsonObject("entries") : data);
        return mediaEntry;
    }

    private PKMediaEntry getFromJson(JsonObject data)  throws JsonSyntaxException{
        if (data.has(id)) { // data holds entries object (multiple entries)
            return MockMediaParser.parseMedia(inputJson.getAsJsonObject(id));

        } else if (data.getAsJsonPrimitive("id").getAsString().equals(id)) { // data is the actual inputJson object
            return MockMediaParser.parseMedia(inputJson);
        }
        return null;
    }


    static class MockMediaParser {

        static PKMediaEntry parseMedia(JsonObject mediaObject) throws JsonSyntaxException {
            return new Gson().fromJson(mediaObject, PKMediaEntry.class);
        }

    }
}
