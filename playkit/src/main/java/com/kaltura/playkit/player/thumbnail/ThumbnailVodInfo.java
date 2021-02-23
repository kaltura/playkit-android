package com.kaltura.playkit.player.thumbnail;

import com.kaltura.playkit.player.ImageTrack;

import java.util.LinkedHashMap;
import java.util.Map;


public class ThumbnailVodInfo {

    Map<ImageRangeInfo, ThumbnailInfo> imageRangeThumbnailtMap;

    public Map<ImageRangeInfo, ThumbnailInfo> getImageRangeThumbnailMap() {
        return imageRangeThumbnailtMap;
    }

    public ThumbnailVodInfo(Map<ImageRangeInfo,ThumbnailInfo> imageRangeThumbnailMap) {
        this.imageRangeThumbnailtMap = imageRangeThumbnailMap;
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


        imageRangeThumbnailtMap = new LinkedHashMap<>();
        long rangeStart = startNumber == 1 ? 0 : startNumber;
        long rangeEnd = (((imageMultiplier * imageTrack.getSegmentDuration()) + singleImageDuration) - 1);
        long diff = 0;
        if (rangeStart > rangeEnd) {
            rangeEnd = rangeStart + rangeEnd;
        }
        diff = rangeEnd - rangeStart;
        float widthPerTile = imageTrack.getWidth() / imageTrack.getTilesHorizontal();
        float heightPerTile = imageTrack.getHeight() / imageTrack.getTilesVertical();
        for (int rowIndex = 0; rowIndex < imageTrack.getTilesVertical(); rowIndex++) {
            for (int colIndex = 0; colIndex < imageTrack.getTilesHorizontal(); colIndex++) {
                ImageRangeInfo imageRangeInfo = new ImageRangeInfo(rangeStart, rangeEnd);
                ThumbnailInfo thumbnailInfo = new ThumbnailInfo(realImageUrl, colIndex * widthPerTile, rowIndex * heightPerTile, widthPerTile, heightPerTile);
//                Rect rect =
//                        new Rect((colIndex * widthPerTile),
//                                rowIndex * heightPerTile,
//                                (colIndex * widthPerTile + widthPerTile),
//                                rowIndex * heightPerTile + heightPerTile);

                if (rangeEnd - diff > mediaDurationMS + imageTrack.getStartNumber()) {
                    continue;
                }
                imageRangeThumbnailtMap.put(imageRangeInfo, thumbnailInfo);
                rangeStart += singleImageDuration;
                rangeEnd = rangeStart + diff;
            }
        }
    }
}