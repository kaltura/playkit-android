package com.kaltura.playkit.mediaproviders.base;

import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.api.phoenix.APIDefines;

/**
 * Created by tehilarozin on 02/04/2017.
 */

public enum MediaType {
    Vod(APIDefines.KalturaAssetType.Media, PKMediaEntry.MediaEntryType.Vod),
    Channel(APIDefines.KalturaAssetType.Media, PKMediaEntry.MediaEntryType.Live),
    Recording(APIDefines.KalturaAssetType.Recording,PKMediaEntry.MediaEntryType.Vod),
    EPG(APIDefines.KalturaAssetType.Epg, PKMediaEntry.MediaEntryType.Live);

    MediaType(APIDefines.KalturaAssetType assetType, PKMediaEntry.MediaEntryType mediaEntryType){
        this.assetType = assetType;
        this.mediaEntryType = mediaEntryType;
    }

    private APIDefines.KalturaAssetType assetType;
    private PKMediaEntry.MediaEntryType mediaEntryType;

    public APIDefines.KalturaAssetType getAssetType() {
        return assetType;
    }

    public PKMediaEntry.MediaEntryType getMediaEntryType() {
        return mediaEntryType;
    }
}