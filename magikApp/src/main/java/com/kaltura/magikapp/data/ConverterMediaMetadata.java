package com.kaltura.magikapp.data;

import java.util.List;

/**
 * Created by itanbarpeled on 16/12/2016.
 */

public class ConverterMediaMetadata {


    String title;
    String subtitle;
    List<ConverterImageUrl> imageUrls;


    public List<ConverterImageUrl> getImageUrls() {
        return imageUrls;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getTitle() {
        return title;
    }
}
