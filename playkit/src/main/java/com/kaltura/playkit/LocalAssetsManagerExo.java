package com.kaltura.playkit;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kaltura.androidx.media3.common.MediaItem;
import com.kaltura.androidx.media3.exoplayer.source.MediaSource;
import com.kaltura.playkit.drm.WidevineModularAdapter;

import java.util.Map;

import static com.kaltura.playkit.Utils.toBase64;

public class LocalAssetsManagerExo {

    private static final PKLog log = PKLog.get("LocalAssetsManagerExo");

    private final LocalAssetsManagerHelper helper;

    public LocalAssetsManagerExo(Context context) {
        this.helper = new LocalAssetsManagerHelper(context);
    }

    public void setLicenseRequestAdapter(PKRequestParams.Adapter licenseRequestAdapter) {
        helper.setLicenseRequestAdapter(licenseRequestAdapter);
    }

    private static LocalAssetsManager.AssetStatus assetStatusFromWidevineMap(Map<String, String> map) {
        long licenseDurationRemaining;
        long playbackDurationRemaining;
        try {
            final String licenseDurationRemainingString = map.get("LicenseDurationRemaining");
            final String playbackDurationRemainingString = map.get("PlaybackDurationRemaining");
            if (playbackDurationRemainingString == null || licenseDurationRemainingString == null) {
                log.e("Missing keys in KeyStatus: " + map);
                return LocalAssetsManager.AssetStatus.withDrm(false, -1, -1);
            }

            licenseDurationRemaining = Long.parseLong(licenseDurationRemainingString);
            playbackDurationRemaining = Long.parseLong(playbackDurationRemainingString);

            return LocalAssetsManager.AssetStatus.withDrm(true, licenseDurationRemaining, playbackDurationRemaining);

        } catch (NumberFormatException e) {
            log.e("Invalid integers in KeyStatus: " + map);
            return LocalAssetsManager.AssetStatus.withDrm(false, -1, -1);
        }
    }

    public void registerWidevineAsset(String assetId, PKMediaFormat pkMediaFormat, String licenseUri, byte[] drmInitData, boolean forceWidevineL3Playback) throws LocalAssetsManager.RegisterException {
        final WidevineModularAdapter widevine = new WidevineModularAdapter(helper.context, helper.localDataStore);
        widevine.registerAsset(drmInitData, "video/mp4", licenseUri, forceWidevineL3Playback, helper.licenseRequestParamAdapter);
        helper.saveMediaFormat(assetId, pkMediaFormat, PKDrmParams.Scheme.WidevineCENC);
    }

    public void unregisterAsset(String assetId, @Nullable byte[] drmInitData) {
        helper.removeAssetKey(assetId);
        if (drmInitData != null) {
            helper.localDataStore.remove(toBase64(drmInitData));
        }
    }

    /**
     * @param assetId        - the id of the asset.
     * @param exoMediaItem - the actual url of the video that should be played.
     * @return - the {@link PKMediaSource} that should be passed to the player.
     */
    public PKMediaSource getLocalMediaSource(@NonNull final String assetId, @NonNull final MediaItem exoMediaItem) {
        return new LocalExoMediaItem(helper.localDataStore, exoMediaItem, assetId, helper.getLocalAssetScheme(assetId));
    }

    /**
     * @param assetId        - the id of the asset.
     * @param exoMediaSource - the actual url of the video that should be played.
     * @return - the {@link PKMediaSource} that should be passed to the player.
     */
    public PKMediaSource getLocalMediaSource(@NonNull final String assetId, @NonNull final MediaSource exoMediaSource) {
        return new LocalExoMediaSource(helper.localDataStore, exoMediaSource, assetId, helper.getLocalAssetScheme(assetId));
    }

    public PKMediaSource getLocalMediaSource(@NonNull final String assetId, @NonNull final String localAssetPath) {
        return new LocalAssetsManager.LocalMediaSource(helper.localDataStore, localAssetPath, assetId, helper.getLocalAssetScheme(assetId));
    }

    public PKMediaSource getLocalMediaItem(@NonNull final String assetId, @NonNull final String localAssetPath) {
        return new LocalAssetsManager.LocalMediaSource(helper.localDataStore, localAssetPath, assetId, helper.getLocalAssetScheme(assetId));
    }

    public LocalAssetsManager.AssetStatus getDrmStatus(String assetId, byte[] drmInitData, boolean forceWidevineL3Playback) {
        final PKDrmParams.Scheme scheme = helper.getLocalAssetScheme(assetId);
        if (scheme != PKDrmParams.Scheme.WidevineCENC) {
            return LocalAssetsManager.AssetStatus.invalid;
        }

        final WidevineModularAdapter adapter = new WidevineModularAdapter(helper.context, helper.localDataStore);
        try {
            Map<String, String> map = adapter.checkAssetStatus(drmInitData, forceWidevineL3Playback);
            return assetStatusFromWidevineMap(map);
        } catch (LocalAssetsManager.RegisterException e) {
            return LocalAssetsManager.AssetStatus.invalid;
        }
    }

    public static class LocalExoMediaItem extends LocalAssetsManager.LocalMediaSource {
        private MediaItem exoMediaItem;
        PKDrmParams.Scheme scheme;

        /**
         * @param localDataStore - the storage from where drm keySetId is stored.
         * @param assetId        - the id of the media.
         * @param scheme
         */
        LocalExoMediaItem(LocalDataStore localDataStore, @NonNull MediaItem exoMediaItem, String assetId, PKDrmParams.Scheme scheme) {
            super(localDataStore, null, assetId, scheme);

            this.exoMediaItem = exoMediaItem;
            this.scheme = scheme;
        }

        public MediaItem getExoMediaItem() {
            return exoMediaItem;
        }

        public PKDrmParams.Scheme getScheme() {
            return scheme;
        }
    }


    public static class LocalExoMediaSource extends LocalAssetsManager.LocalMediaSource {
        private MediaSource exoMediaSource;
        PKDrmParams.Scheme scheme;

        /**
         * @param localDataStore - the storage from where drm keySetId is stored.
         * @param assetId        - the id of the media.
         * @param scheme
         */
        LocalExoMediaSource(LocalDataStore localDataStore, @NonNull MediaSource exoMediaSource, String assetId, PKDrmParams.Scheme scheme) {
            super(localDataStore, null, assetId, scheme);

            this.exoMediaSource = exoMediaSource;
            this.scheme = scheme;
        }

        public MediaSource getExoMediaSource() {
            return exoMediaSource;
        }

        public PKDrmParams.Scheme getScheme() {
            return scheme;
        }
    }
}
