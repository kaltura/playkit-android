package com.kaltura.playkit.offline;

import com.kaltura.playkit.PKMediaSource;

/**
 * The local media source that should be passed to the player
 * when offline(locally stored) media want to be played.
 * Created by anton.afanasiev on 18/12/2016.
 */

public class LocalMediaSource extends PKMediaSource {

    private LocalDrmStorage localDrmStorage;

    /**
     * @param localDrmStorage - the storage from where drm keySetId is stored.
     * @param localPath - the local url of the media.
     * @param assetId - the id of the media.
     */
    public LocalMediaSource(LocalDrmStorage localDrmStorage, String localPath, String assetId) {
        setId(assetId);
        setUrl(localPath);
        this.localDrmStorage = localDrmStorage;
    }

    /**
     * @return - the {@link LocalDrmStorage}
     */
    public LocalDrmStorage getStorage() {
        return localDrmStorage;
    }

}
