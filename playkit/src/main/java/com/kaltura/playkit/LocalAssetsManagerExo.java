package com.kaltura.playkit;

import android.content.Context;
import androidx.annotation.NonNull;
import com.kaltura.android.exoplayer2.source.MediaSource;
import com.kaltura.playkit.drm.WidevineModularAdapter;

public class LocalAssetsManagerExo {

    private final LocalAssetsManagerHelper helper;

    public LocalAssetsManagerExo(Context context) {
        this.helper = new LocalAssetsManagerHelper(context);
    }

    public void registerWidevineDashAsset(String assetId, String licenseUri, byte[] drmInitData) throws LocalAssetsManagerImp.RegisterException {
        final WidevineModularAdapter widevine = new WidevineModularAdapter(helper.context, helper.localDataStore);
        widevine.registerAsset(drmInitData, "video/mp4", licenseUri, helper.licenseRequestParamAdapter);
        helper.saveMediaFormat(assetId, PKMediaFormat.dash, PKDrmParams.Scheme.WidevineCENC);
    }

    /**
     * @param assetId        - the id of the asset.
    //     * @param exoMediaSource - the actual url of the video that should be played.
     * @return - the {@link PKMediaSource} that should be passed to the player.
     */
    public PKMediaSource getLocalMediaSource(@NonNull final String assetId, @NonNull final MediaSource exoMediaSource) {
        return new LocalExoMediaSource(helper.localDataStore, exoMediaSource, assetId, helper.getLocalAssetScheme(assetId));
    }

    public static class LocalExoMediaSource extends LocalAssetsManagerImp.LocalMediaSourceImp {
        private MediaSource exoMediaSource;

        /**
         * @param localDataStore - the storage from where drm keySetId is stored.
         * @param assetId        - the id of the media.
         * @param scheme
         */
        LocalExoMediaSource(LocalDataStore localDataStore, @NonNull MediaSource exoMediaSource, String assetId, PKDrmParams.Scheme scheme) {
            super(localDataStore, null, assetId, scheme);

            this.exoMediaSource = exoMediaSource;
        }

        public MediaSource getExoMediaSource() {
            return exoMediaSource;
        }
    }
}
