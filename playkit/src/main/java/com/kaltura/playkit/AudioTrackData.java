package com.kaltura.playkit;

/**
 * Created by anton.afanasiev on 17/11/2016.
 */
public class AudioTrackData {

    private String language;
    private String mimeType;
    private int channelCount;
    private String id;
    private int sampleRate;

    public AudioTrackData(String language, String mimeType, int channelCount, String id, int sampleRate) {
        this.language = language;
        this.mimeType = mimeType;
        this.channelCount = channelCount;
        this.id = id;
        this.sampleRate = sampleRate;
    }

    public String getLanguage() {
        return language;
    }

    public String getMimeType() {
        return mimeType;
    }

    public int getChannelCount() {
        return channelCount;
    }

    public String getId() {
        return id;
    }

    public int getSampleRate() {
        return sampleRate;
    }
}
