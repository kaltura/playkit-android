package com.kaltura.playkit.api.phoenix.model;

/**
 * Created by gilad.nadav on 1/30/18.
 */

public class KalturaThumbnail {

     String ratio;
     Integer width;
     Integer height;
     String url;
     Integer version;
     String id;
     Boolean isDefault;

    public String getRatio() {
        return ratio;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public String getUrl() {
        return url;
    }

    public Integer getVersion() {
        return version;
    }

    public String getId() {
        return id;
    }

    public Boolean getDefault() {
        return isDefault;
    }
}
