package com.kaltura.playkit.player.thumbnail;

import android.graphics.Rect;
import android.util.Log;

import com.kaltura.playkit.player.ImageTrack;

import java.util.LinkedHashMap;
import java.util.Map;


public class ThumbnailVodInfo {

    Map<ImageRangeInfo, Rect> imageRangeRectMap;

    public Map<ImageRangeInfo, Rect> getImageRangeRectMap() {
        return imageRangeRectMap;
    }

    public ThumbnailVodInfo(long imageUrlIndex, ImageTrack imageTrack, long mediaDurationMS, long startNumber, boolean isCatchup) {

        long imageMultiplier = imageUrlIndex <= 0 ? 0 : imageUrlIndex - 1;
        long imageRealUrlTime = (imageMultiplier * imageTrack.getSegmentDuration());

        if (isCatchup) {
            imageMultiplier = imageTrack.getStartNumber() - imageUrlIndex <= 0 ? 0 : startNumber - imageUrlIndex - 1;
            imageRealUrlTime = startNumber + (imageMultiplier * imageTrack.getSegmentDuration());
        }
        String realImageUrl = imageTrack.getImageTemplateUrl().replace("$Number$", String.valueOf(imageUrlIndex)).replace("$Time$", String.valueOf(imageRealUrlTime));

        long singleImageDuration = (long) Math.ceil(imageTrack.getSegmentDuration() / (imageTrack.getTilesHorizontal() * imageTrack.getTilesVertical()));


        imageRangeRectMap = new LinkedHashMap<>();
        long rangeStart = startNumber == 1 ? 0 : startNumber;
        long rangeEnd = (((imageMultiplier * imageTrack.getSegmentDuration()) + singleImageDuration) - 1);
        long diff = 0;
        if (rangeStart > rangeEnd) {
            rangeEnd = rangeStart + rangeEnd;
        }
        diff = rangeEnd - rangeStart;
        int widthPerTile = imageTrack.getWidth() / imageTrack.getTilesHorizontal();
        int heightPerTile = imageTrack.getHeight() / imageTrack.getTilesVertical();
        for (int rowIndex = 0; rowIndex < imageTrack.getTilesVertical(); rowIndex++) {
            for (int colIndex = 0; colIndex < imageTrack.getTilesHorizontal(); colIndex++) {
                //Log.d("GILAD BY POSITION THUMB", "[" + rowIndex + "," + colIndex + "] = [" + rangeStart + "," + rangeEnd + "]");

                ImageRangeInfo imageRangeInfo = new ImageRangeInfo(realImageUrl, rangeStart, rangeEnd);
                Rect rect =
                        new Rect((colIndex * widthPerTile),
                                rowIndex * heightPerTile,
                                (colIndex * widthPerTile + widthPerTile),
                                rowIndex * heightPerTile + heightPerTile);

                if (rangeEnd - diff > mediaDurationMS + imageTrack.getStartNumber()) {
                    continue;
                }
                imageRangeRectMap.put(imageRangeInfo, rect);
                rangeStart += singleImageDuration;
                rangeEnd = rangeStart + diff;
            }
        }
        Log.d("GILAD THUMB", "--------------------------");
    }
}