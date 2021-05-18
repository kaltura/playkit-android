package com.kaltura.playkit.player.thumbnail;

import com.kaltura.playkit.player.DashImageTrack;
import com.kaltura.playkit.player.ImageTrack;

import java.util.LinkedHashMap;
import java.util.Map;


class ThumbnailVodInfo {

    Map<ImageRangeInfo, ThumbnailInfo> imageRangeThumbnailtMap;

    public Map<ImageRangeInfo, ThumbnailInfo> getImageRangeThumbnailMap() {
        return imageRangeThumbnailtMap;
    }

    public ThumbnailVodInfo(Map<ImageRangeInfo,ThumbnailInfo> imageRangeThumbnailMap) {
        this.imageRangeThumbnailtMap = imageRangeThumbnailMap;
    }

    public ThumbnailVodInfo(long imageUrlIndex, ImageTrack imageTrack, long mediaDurationMS, long startNumber, boolean isCatchup) {

        long imageMultiplier = imageUrlIndex <= 0 ? 0 : imageUrlIndex - 1;
        long imageRealUrlTime = (imageMultiplier * imageTrack.getDuration());

        if (isCatchup) {
            imageMultiplier = ((DashImageTrack)imageTrack).getStartNumber() - imageUrlIndex <= 0 ? 0 : startNumber - imageUrlIndex - 1;
            imageRealUrlTime = startNumber + (imageMultiplier * imageTrack.getDuration());
        }
        String realImageUrl = imageTrack.getUrl().replace("$Number$", String.valueOf(imageUrlIndex)).replace("$Time$", String.valueOf(imageRealUrlTime));

        long singleImageDuration = (long) Math.ceil(imageTrack.getDuration() / (imageTrack.getCols() * imageTrack.getRows()));


        imageRangeThumbnailtMap = new LinkedHashMap<>();
        long rangeStart = startNumber == 1 ? 0 : startNumber;
        long rangeEnd = (((imageMultiplier * imageTrack.getDuration()) + singleImageDuration) - 1);
        long diff;
        if (rangeStart > rangeEnd) {
            rangeEnd = rangeStart + rangeEnd;
        }
        diff = rangeEnd - rangeStart;
        float widthPerTile = imageTrack.getWidth() / imageTrack.getCols();
        float heightPerTile = imageTrack.getHeight() / imageTrack.getRows();
        for (int rowIndex = 0; rowIndex < imageTrack.getRows(); rowIndex++) {
            for (int colIndex = 0; colIndex < imageTrack.getCols(); colIndex++) {
                ImageRangeInfo imageRangeInfo = new ImageRangeInfo(rangeStart, rangeEnd);
                ThumbnailInfo thumbnailInfo = new ThumbnailInfo(realImageUrl, colIndex * widthPerTile, rowIndex * heightPerTile, widthPerTile, heightPerTile);

                if (rangeEnd - diff > mediaDurationMS + ((DashImageTrack)imageTrack).getStartNumber()) {
                    continue;
                }
                imageRangeThumbnailtMap.put(imageRangeInfo, thumbnailInfo);
                rangeStart += singleImageDuration;
                rangeEnd = rangeStart + diff;
            }
        }
    }
}
