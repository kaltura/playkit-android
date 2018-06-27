package com.kaltura.playkit.api.phoenix.model;

public class KalturaThumbnail {

    private Integer width;
    private Integer height;
    private Integer version;

    private String id;
    private String url;
    private String ratio;

    private Boolean isDefault;

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public Integer getVersion() {
        return version;
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getRatio() {
        return ratio;
    }

    public Boolean getDefault() {
        return isDefault;
    }
}
