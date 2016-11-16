package com.kaltura.playkit.backend.mock;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.backend.base.OnMediaLoadCompletion;
import com.kaltura.playkit.connect.ErrorElement;
import com.kaltura.playkit.connect.ResultElement;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by tehilarozin on 06/11/2016.
 */

public class MockMediaProvider implements MediaEntryProvider {

    private JsonObject inputJson;
    private String inputFile;
    private String id;
    private Context context;

    public MockMediaProvider(JsonObject inputJson, String id) {
        this.inputJson = inputJson;
        this.id = id;
    }

    public MockMediaProvider(String inputFile, Context context, String id) {
        this.inputFile = inputFile;
        this.id = id;
        this.context = context;
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

        } catch (IOException | JsonSyntaxException ex) {
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

    /**
     * reads entries Json input from a file. the data is "saved" in the inputJson member.
     * next load on the same {@link MockMediaProvider} object will not read the file again, but
     * use the inputJson member, and search for the "id" entry.
     *
     * @return - The Media entry identified by "id"
     * @throws IOException
     */
    private PKMediaEntry getFromFile() throws IOException {
        JsonParser parser = new JsonParser();

        if(context != null){
            JsonReader jsonReader = new JsonReader(new InputStreamReader(context.getAssets().open(inputFile)));
            inputJson = parser.parse(jsonReader).getAsJsonObject();
        } else {
            inputJson = parser.parse(new FileReader(inputFile)).getAsJsonObject();
        }

        return getFromJson(inputJson);
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
