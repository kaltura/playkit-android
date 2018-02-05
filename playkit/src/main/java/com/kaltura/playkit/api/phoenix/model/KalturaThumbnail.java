package com.kaltura.playkit.api.phoenix.model;

public class KalturaThumbnail {

    private String ratio;
    private Integer width;
    private Integer height;
    private String url;
    private Integer version;
    private  String id;
    private Boolean isDefault;

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
