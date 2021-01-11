package com.kaltura.playkit;

import android.content.Context;
import android.net.Uri;
import android.util.Pair;
import android.util.SparseArray;

import androidx.annotation.Nullable;

import com.kaltura.android.exoplayer2.C;
import com.kaltura.android.exoplayer2.MediaItem;
import com.kaltura.android.exoplayer2.drm.DrmSessionManager;
import com.kaltura.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.kaltura.android.exoplayer2.extractor.ExtractorsFactory;
import com.kaltura.android.exoplayer2.offline.StreamKey;
import com.kaltura.android.exoplayer2.source.ClippingMediaSource;
import com.kaltura.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.kaltura.android.exoplayer2.source.MediaSource;
import com.kaltura.android.exoplayer2.source.MediaSourceDrmHelper;
import com.kaltura.android.exoplayer2.source.MediaSourceFactory;
import com.kaltura.android.exoplayer2.source.MergingMediaSource;
import com.kaltura.android.exoplayer2.source.ProgressiveMediaSource;
import com.kaltura.android.exoplayer2.source.SingleSampleMediaSource;
import com.kaltura.android.exoplayer2.source.ads.AdsLoader;
import com.kaltura.android.exoplayer2.source.ads.AdsMediaSource;
import com.kaltura.android.exoplayer2.upstream.DataSource;
import com.kaltura.android.exoplayer2.upstream.DataSpec;
import com.kaltura.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.kaltura.android.exoplayer2.upstream.HttpDataSource;
import com.kaltura.android.exoplayer2.upstream.LoadErrorHandlingPolicy;
import com.kaltura.android.exoplayer2.util.Assertions;
import com.kaltura.android.exoplayer2.util.Log;
import com.kaltura.android.exoplayer2.util.Util;

import java.util.Arrays;
import java.util.List;

public class CustomMediaSourceFactory implements MediaSourceFactory {

    /**
     * Provides {@link AdsLoader} instances for media items that have {@link
     * MediaItem.PlaybackProperties#adTagUri ad tag URIs}.
     */
    public interface AdsLoaderProvider {

        /**
         * Returns an {@link AdsLoader} for the given {@link MediaItem.PlaybackProperties#adTagUri ad
         * tag URI}, or null if no ads loader is available for the given ad tag URI.
         *
         * <p>This method is called each time a {@link MediaSource} is created from a {@link MediaItem}
         * that defines an {@link MediaItem.PlaybackProperties#adTagUri ad tag URI}.
         */
        @Nullable
        AdsLoader getAdsLoader(Uri adTagUri);
    }

    private static final String TAG = "DefaultMediaSourceFactory";

    private final MediaSourceDrmHelper mediaSourceDrmHelper;
    private final DataSource.Factory dataSourceFactory;
    private final SparseArray<MediaSourceFactory> mediaSourceFactories;
    @C.ContentType private final int[] supportedTypes;

    @Nullable private DefaultMediaSourceFactory.AdsLoaderProvider adsLoaderProvider;
    @Nullable private AdsLoader.AdViewProvider adViewProvider;
    @Nullable private DrmSessionManager drmSessionManager;
    @Nullable private List<StreamKey> streamKeys;
    @Nullable private LoadErrorHandlingPolicy loadErrorHandlingPolicy;

    /**
     * Creates a new instance.
     *
     * @param context Any context.
     */
    public CustomMediaSourceFactory(Context context) {
        this(new DefaultDataSourceFactory(context));
    }

    /**
     * Creates a new instance.
     *
     * @param context Any context.
     * @param extractorsFactory An {@link ExtractorsFactory} used to extract progressive media from
     *     its container.
     */
    public CustomMediaSourceFactory(Context context, ExtractorsFactory extractorsFactory) {
        this(new DefaultDataSourceFactory(context), extractorsFactory);
    }

    /**
     * Creates a new instance.
     *
     * @param dataSourceFactory A {@link DataSource.Factory} to create {@link DataSource} instances
     *     for requesting media data.
     */
    public CustomMediaSourceFactory(DataSource.Factory dataSourceFactory) {
        this(dataSourceFactory, new DefaultExtractorsFactory());
    }

    /**
     * Creates a new instance.
     *
     * @param dataSourceFactory A {@link DataSource.Factory} to create {@link DataSource} instances
     *     for requesting media data.
     * @param extractorsFactory An {@link ExtractorsFactory} used to extract progressive media from
     *     its container.
     */
    public CustomMediaSourceFactory(
            DataSource.Factory dataSourceFactory, ExtractorsFactory extractorsFactory) {
        this.dataSourceFactory = dataSourceFactory;
        mediaSourceDrmHelper = new MediaSourceDrmHelper();
        mediaSourceFactories = loadDelegates(dataSourceFactory, extractorsFactory);
        supportedTypes = new int[mediaSourceFactories.size()];
        for (int i = 0; i < mediaSourceFactories.size(); i++) {
            supportedTypes[i] = mediaSourceFactories.keyAt(i);
        }
    }

    /**
     * Sets the {@link DefaultMediaSourceFactory.AdsLoaderProvider} that provides {@link AdsLoader} instances for media items
     * that have {@link MediaItem.PlaybackProperties#adTagUri ad tag URIs}.
     *
     * @param adsLoaderProvider A provider for {@link AdsLoader} instances.
     * @return This factory, for convenience.
     */
    public CustomMediaSourceFactory setAdsLoaderProvider(
            @Nullable DefaultMediaSourceFactory.AdsLoaderProvider adsLoaderProvider) {
        this.adsLoaderProvider = adsLoaderProvider;
        return this;
    }

    /**
     * Sets the {@link AdsLoader.AdViewProvider} that provides information about views for the ad playback UI.
     *
     * @param adViewProvider A provider for {@link AdsLoader} instances.
     * @return This factory, for convenience.
     */
    public CustomMediaSourceFactory setAdViewProvider(@Nullable AdsLoader.AdViewProvider adViewProvider) {
        this.adViewProvider = adViewProvider;
        return this;
    }

    @Override
    public CustomMediaSourceFactory setDrmHttpDataSourceFactory(
            @Nullable HttpDataSource.Factory drmHttpDataSourceFactory) {
        mediaSourceDrmHelper.setDrmHttpDataSourceFactory(drmHttpDataSourceFactory);
        return this;
    }

    @Override
    public CustomMediaSourceFactory setDrmUserAgent(@Nullable String userAgent) {
        mediaSourceDrmHelper.setDrmUserAgent(userAgent);
        return this;
    }

    @Override
    public CustomMediaSourceFactory setDrmSessionManager(
            @Nullable DrmSessionManager drmSessionManager) {
        this.drmSessionManager = drmSessionManager;
        return this;
    }

    @Override
    public CustomMediaSourceFactory setLoadErrorHandlingPolicy(
            @Nullable LoadErrorHandlingPolicy loadErrorHandlingPolicy) {
        this.loadErrorHandlingPolicy = loadErrorHandlingPolicy;
        return this;
    }

    /**
     * @deprecated Use {@link MediaItem.Builder#setStreamKeys(List)} and {@link
     *     #createMediaSource(MediaItem)} instead.
     */
    @SuppressWarnings("deprecation")
    @Deprecated
    @Override
    public CustomMediaSourceFactory setStreamKeys(@Nullable List<StreamKey> streamKeys) {
        this.streamKeys = streamKeys != null && !streamKeys.isEmpty() ? streamKeys : null;
        return this;
    }

    @Override
    public int[] getSupportedTypes() {
        return Arrays.copyOf(supportedTypes, supportedTypes.length);
    }

    @SuppressWarnings("deprecation")
    @Override
    public MediaSource createMediaSource(MediaItem mediaItem) {
        Assertions.checkNotNull(mediaItem.playbackProperties);
        @C.ContentType
        int type =
                Util.inferContentTypeForUriAndMimeType(
                        mediaItem.playbackProperties.uri, mediaItem.playbackProperties.mimeType);
        @Nullable MediaSourceFactory mediaSourceFactory = mediaSourceFactories.get(type);
        Assertions.checkNotNull(
                mediaSourceFactory, "No suitable media source factory found for content type: " + type);
        mediaSourceFactory.setDrmSessionManager(
                drmSessionManager != null ? drmSessionManager : mediaSourceDrmHelper.create(mediaItem));
        mediaSourceFactory.setStreamKeys(
                !mediaItem.playbackProperties.streamKeys.isEmpty()
                        ? mediaItem.playbackProperties.streamKeys
                        : streamKeys);
        mediaSourceFactory.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy);

        MediaSource mediaSource = mediaSourceFactory.createMediaSource(mediaItem);

        List<MediaItem.Subtitle> subtitles = mediaItem.playbackProperties.subtitles;
        if (!subtitles.isEmpty()) {
            MediaSource[] mediaSources = new MediaSource[subtitles.size() + 1];
            mediaSources[0] = mediaSource;
            SingleSampleMediaSource.Factory singleSampleSourceFactory =
                    new SingleSampleMediaSource.Factory(dataSourceFactory)
                    .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
                    .setTreatLoadErrorsAsEndOfStream(true);
            for (int i = 0; i < subtitles.size(); i++) {
                mediaSources[i + 1] =
                        singleSampleSourceFactory.createMediaSource(
                                subtitles.get(i), /* durationUs= */ C.TIME_UNSET);
            }
            mediaSource = new MergingMediaSource(mediaSources);
        }
        return maybeWrapWithAdsMediaSource(mediaItem, maybeClipMediaSource(mediaItem, mediaSource));
    }

    // internal methods

    private static MediaSource maybeClipMediaSource(MediaItem mediaItem, MediaSource mediaSource) {
        if (mediaItem.clippingProperties.startPositionMs == 0
                && mediaItem.clippingProperties.endPositionMs == C.TIME_END_OF_SOURCE
                && !mediaItem.clippingProperties.relativeToDefaultPosition) {
            return mediaSource;
        }
        return new ClippingMediaSource(
                mediaSource,
                C.msToUs(mediaItem.clippingProperties.startPositionMs),
                C.msToUs(mediaItem.clippingProperties.endPositionMs),
                /* enableInitialDiscontinuity= */ !mediaItem.clippingProperties.startsAtKeyFrame,
                /* allowDynamicClippingUpdates= */ mediaItem.clippingProperties.relativeToLiveWindow,
                mediaItem.clippingProperties.relativeToDefaultPosition);
    }

    private MediaSource maybeWrapWithAdsMediaSource(MediaItem mediaItem, MediaSource mediaSource) {
        Assertions.checkNotNull(mediaItem.playbackProperties);
        @Nullable Uri adTagUri = mediaItem.playbackProperties.adTagUri;
        if (adTagUri == null) {
            return mediaSource;
        }
        DefaultMediaSourceFactory.AdsLoaderProvider adsLoaderProvider = this.adsLoaderProvider;
        AdsLoader.AdViewProvider adViewProvider = this.adViewProvider;
        if (adsLoaderProvider == null || adViewProvider == null) {
            Log.w(
                    TAG,
                    "Playing media without ads. Configure ad support by calling setAdsLoaderProvider and"
                            + " setAdViewProvider.");
            return mediaSource;
        }
        @Nullable AdsLoader adsLoader = adsLoaderProvider.getAdsLoader(adTagUri);
        if (adsLoader == null) {
            Log.w(TAG, "Playing media without ads. No AdsLoader for provided adTagUri");
            return mediaSource;
        }
        return new AdsMediaSource(
                mediaSource,
                new DataSpec(adTagUri),
                /* adMediaSourceFactory= */ this,
                adsLoader,
                adViewProvider);
    }

    private static SparseArray<MediaSourceFactory> loadDelegates(
            DataSource.Factory dataSourceFactory, ExtractorsFactory extractorsFactory) {
        SparseArray<MediaSourceFactory> factories = new SparseArray<>();
        // LINT.IfChange
        try {
            Class<? extends MediaSourceFactory> factoryClazz =
                    Class.forName("com.kaltura.android.exoplayer2.source.dash.DashMediaSource$Factory")
                            .asSubclass(MediaSourceFactory.class);
            factories.put(
                    C.TYPE_DASH,
                    factoryClazz.getConstructor(DataSource.Factory.class).newInstance(dataSourceFactory));
        } catch (Exception e) {
            // Expected if the app was built without the dash module.
        }
        try {
            Class<? extends MediaSourceFactory> factoryClazz =
                    Class.forName(
                            "com.kaltura.android.exoplayer2.source.smoothstreaming.SsMediaSource$Factory")
                            .asSubclass(MediaSourceFactory.class);
            factories.put(
                    C.TYPE_SS,
                    factoryClazz.getConstructor(DataSource.Factory.class).newInstance(dataSourceFactory));
        } catch (Exception e) {
            // Expected if the app was built without the smoothstreaming module.
        }
        try {
            Class<? extends MediaSourceFactory> factoryClazz =
                    Class.forName("com.kaltura.android.exoplayer2.source.hls.HlsMediaSource$Factory")
                            .asSubclass(MediaSourceFactory.class);
            factories.put(
                    C.TYPE_HLS,
                    factoryClazz.getConstructor(DataSource.Factory.class).newInstance(dataSourceFactory));
        } catch (Exception e) {
            // Expected if the app was built without the hls module.
        }
        // LINT.ThenChange(../../../../../../../../proguard-rules.txt)
        factories.put(
                C.TYPE_OTHER, new ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory));
        return factories;
    }
}