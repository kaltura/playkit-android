/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 *
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 *
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit.player;

import static com.kaltura.playkit.utils.Consts.TIME_UNSET;
import static com.kaltura.playkit.utils.Consts.TRACK_TYPE_AUDIO;
import static com.kaltura.playkit.utils.Consts.TRACK_TYPE_TEXT;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Charsets;
import com.kaltura.android.exoplayer2.C;
import com.kaltura.android.exoplayer2.DefaultLoadControl;
import com.kaltura.android.exoplayer2.DefaultRenderersFactory;
import com.kaltura.android.exoplayer2.ExoPlayer;
import com.kaltura.android.exoplayer2.ExoPlayerLibraryInfo;
import com.kaltura.android.exoplayer2.Format;
import com.kaltura.android.exoplayer2.LoadControl;
import com.kaltura.android.exoplayer2.MediaItem;
import com.kaltura.android.exoplayer2.PlaybackException;
import com.kaltura.android.exoplayer2.PlaybackParameters;
import com.kaltura.android.exoplayer2.Player;
import com.kaltura.android.exoplayer2.Timeline;
import com.kaltura.android.exoplayer2.Tracks;
import com.kaltura.android.exoplayer2.audio.AudioAttributes;
import com.kaltura.android.exoplayer2.dashmanifestparser.CustomDashManifest;
import com.kaltura.android.exoplayer2.dashmanifestparser.CustomDashManifestParser;
import com.kaltura.android.exoplayer2.drm.DrmSessionManager;
import com.kaltura.android.exoplayer2.drm.DrmSessionManagerProvider;
import com.kaltura.android.exoplayer2.ext.okhttp.OkHttpDataSource;
import com.kaltura.android.exoplayer2.extractor.ExtractorsFactory;
import com.kaltura.android.exoplayer2.extractor.ts.DefaultTsPayloadReaderFactory;
import com.kaltura.android.exoplayer2.extractor.ts.TsExtractor;
import com.kaltura.android.exoplayer2.metadata.Metadata;
import com.kaltura.android.exoplayer2.metadata.MetadataOutput;
import com.kaltura.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.kaltura.android.exoplayer2.source.MediaSource;
import com.kaltura.android.exoplayer2.source.MergingMediaSource;
import com.kaltura.android.exoplayer2.source.ProgressiveMediaSource;
import com.kaltura.android.exoplayer2.source.SingleSampleMediaSource;
import com.kaltura.android.exoplayer2.source.dash.DashMediaSource;
import com.kaltura.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.kaltura.android.exoplayer2.source.dash.manifest.DashManifest;
import com.kaltura.android.exoplayer2.source.dash.manifest.DashManifestParserForThumbnail;
import com.kaltura.android.exoplayer2.source.dash.manifest.EventStream;
import com.kaltura.android.exoplayer2.source.hls.HlsMediaSource;
import com.kaltura.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.kaltura.android.exoplayer2.ui.SubtitleView;
import com.kaltura.android.exoplayer2.upstream.BandwidthMeter;
import com.kaltura.android.exoplayer2.upstream.ByteArrayDataSink;
import com.kaltura.android.exoplayer2.upstream.DataSource;
import com.kaltura.android.exoplayer2.upstream.DataSpec;
import com.kaltura.android.exoplayer2.upstream.DefaultAllocator;
import com.kaltura.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.kaltura.android.exoplayer2.upstream.DefaultDataSource;
import com.kaltura.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.kaltura.android.exoplayer2.upstream.HttpDataSource;
import com.kaltura.android.exoplayer2.upstream.TeeDataSource;
import com.kaltura.android.exoplayer2.upstream.TransferListener;
import com.kaltura.android.exoplayer2.upstream.UdpDataSource;
import com.kaltura.android.exoplayer2.upstream.cache.Cache;
import com.kaltura.android.exoplayer2.upstream.cache.CacheDataSource;
import com.kaltura.android.exoplayer2.util.TimestampAdjuster;
import com.kaltura.android.exoplayer2.video.ConfigurableLoadControl;
import com.kaltura.playkit.LocalAssetsManager;
import com.kaltura.playkit.LocalAssetsManagerExo;
import com.kaltura.playkit.PKAbrFilter;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKError;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.PKPlaybackException;
import com.kaltura.playkit.PKRequestConfig;
import com.kaltura.playkit.PKRequestParams;
import com.kaltura.playkit.PlaybackInfo;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.Utils;
import com.kaltura.playkit.drm.DeferredDrmSessionManager;
import com.kaltura.playkit.drm.DrmCallback;
import com.kaltura.playkit.player.metadata.MetadataConverter;
import com.kaltura.playkit.player.metadata.PKMetadata;
import com.kaltura.playkit.player.thumbnail.ThumbnailInfo;
import com.kaltura.playkit.utils.Consts;
import com.kaltura.playkit.utils.NativeCookieJarBridge;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.OkHttpClient;

public class ExoPlayerWrapper implements PlayerEngine, Player.Listener, MetadataOutput, BandwidthMeter.EventListener, ExoAnalyticsAggregator.InputFormatChangedListener {

    private ByteArrayDataSink dashLastDataSink;
    private String dashManifestString;

    public interface LoadControlStrategy {
        LoadControl getCustomLoadControl();
        BandwidthMeter getCustomBandwidthMeter();
    }

    private static final PKLog log = PKLog.get("ExoPlayerWrapper");

    private BandwidthMeter bandwidthMeter;
    @NonNull private PlayerSettings playerSettings;
    private EventListener eventListener;
    private StateChangedListener stateChangedListener;
    private ExoAnalyticsAggregator analyticsAggregator = new ExoAnalyticsAggregator();

    private Context context;
    private ExoPlayer player;
    private BaseExoplayerView exoPlayerView;
    private PlayerView rootView;
    private boolean rootViewUpdated;

    private PKTracks tracks;
    private List<EventStream> eventStreams;
    private Timeline.Window window;
    private TrackSelectionHelper trackSelectionHelper;
    private DeferredDrmSessionManager drmSessionManager;
    private MediaSource.Factory mediaSourceFactory;
    private LoadControl configurableLoadControl;
    private PlayerEvent.Type currentEvent;
    private PlayerState currentState = PlayerState.IDLE;
    private PlayerState previousState;

    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private PKError currentError = null;

    private boolean isSeeking;
    private boolean useTextureView;
    private boolean isSurfaceSecured;
    private boolean shouldGetTracksInfo;
    private boolean preferredLanguageWasSelected;
    private boolean shouldRestorePlayerToPreviousState;
    private boolean isPlayerReleased;
    private boolean isLoadedMetaDataFired;

    private int playerWindow;
    private long playerPosition = TIME_UNSET;

    private float lastKnownVolume = Consts.DEFAULT_VOLUME;
    private float lastKnownPlaybackRate = Consts.DEFAULT_PLAYBACK_RATE_SPEED;

    private List<PKMetadata> metadataList = new ArrayList<>();
    private String[] lastSelectedTrackIds = {TrackSelectionHelper.NONE, TrackSelectionHelper.NONE, TrackSelectionHelper.NONE, TrackSelectionHelper.NONE};

    private TrackSelectionHelper.TracksInfoListener tracksInfoListener = initTracksInfoListener();
    private TrackSelectionHelper.TracksErrorListener tracksErrorListener = initTracksErrorListener();
    private CustomLoadErrorHandlingPolicy customLoadErrorHandlingPolicy;
    private DeferredDrmSessionManager.DrmSessionListener drmSessionListener = initDrmSessionListener();
    private PKMediaSourceConfig sourceConfig;
    @NonNull private Profiler profiler = Profiler.NOOP;

    private Timeline.Period period;

    private Cache downloadCache;

    ExoPlayerWrapper(Context context, PlayerSettings playerSettings, PlayerView rootPlayerView) {
        this(context, new ExoPlayerView(context), playerSettings, rootPlayerView);
    }

    ExoPlayerWrapper(Context context, BaseExoplayerView exoPlayerView, PlayerSettings settings, PlayerView rootPlayerView) {
        this.context = context;

        playerSettings = settings != null ? settings : new PlayerSettings();
        rootView = rootPlayerView;

        LoadControlStrategy customLoadControlStrategy = getCustomLoadControlStrategy();
        if (customLoadControlStrategy != null && customLoadControlStrategy.getCustomBandwidthMeter() != null) {
            bandwidthMeter = customLoadControlStrategy.getCustomBandwidthMeter();
        } else {
            DefaultBandwidthMeter.Builder bandwidthMeterBuilder = new DefaultBandwidthMeter.Builder(context);

            Long initialBitrateEstimate = playerSettings.getAbrSettings().getInitialBitrateEstimate();

            if (initialBitrateEstimate != null && initialBitrateEstimate > 0) {
                bandwidthMeterBuilder.setInitialBitrateEstimate(initialBitrateEstimate);
            }

            bandwidthMeter = bandwidthMeterBuilder.build();
        }
        if (bandwidthMeter != null) {
            bandwidthMeter.addEventListener(mainHandler, this);
        }

        period = new Timeline.Period();
        this.exoPlayerView = exoPlayerView;
    }

    private LoadControlStrategy getCustomLoadControlStrategy() {
        Object loadControlStrategyObj = playerSettings.getCustomLoadControlStrategy();
        if (loadControlStrategyObj instanceof LoadControlStrategy) {
            return ((LoadControlStrategy) loadControlStrategyObj);
        } else {
            return null;
        }
    }

    @Override
    public void onBandwidthSample(int elapsedMs, long bytes, long bitrate) {
        if (assertPlayerIsNotNull("onBandwidthSample") && !isPlayerReleased && trackSelectionHelper != null) {
            sendEvent(PlayerEvent.Type.PLAYBACK_INFO_UPDATED);
        }
    }

    private void initializePlayer() {
        DefaultTrackSelector trackSelector = initializeTrackSelector();
        DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(context);
        renderersFactory.setAllowedVideoJoiningTimeMs(playerSettings.getLoadControlBuffers().getAllowedVideoJoiningTimeMs());
        renderersFactory.setEnableDecoderFallback(playerSettings.enableDecoderFallback());

        addCustomLoadErrorPolicy();
        mediaSourceFactory = new DefaultMediaSourceFactory(getDataSourceFactory(Collections.emptyMap()));
        mediaSourceFactory.setLoadErrorHandlingPolicy(customLoadErrorHandlingPolicy);
        configurableLoadControl = getUpdatedLoadControl();
        player = new ExoPlayer.Builder(context, renderersFactory)
                .setTrackSelector(trackSelector)
                .setLoadControl(configurableLoadControl)
                .setMediaSourceFactory(mediaSourceFactory)
                .setBandwidthMeter(bandwidthMeter)
                .setUsePlatformDiagnostics(false)
                .build();

        player.setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus= */ playerSettings.isHandleAudioFocus());
        player.setHandleAudioBecomingNoisy(playerSettings.isHandleAudioBecomingNoisyEnabled());
        player.setWakeMode(playerSettings.getWakeMode().ordinal());

        window = new Timeline.Window();
        setPlayerListeners();
        if (playerSettings.getAspectRatioResizeMode() != null) {
            configureAspectRatioResizeMode(playerSettings.getAspectRatioResizeMode());
        }
        exoPlayerView.setPlayer(player, useTextureView, isSurfaceSecured, playerSettings.isVideoViewHidden());

        player.setPlayWhenReady(false);
    }

    @NonNull
    private LoadControl getUpdatedLoadControl() {
        LoadControlStrategy customLoadControlStrategy = getCustomLoadControlStrategy();
        if (customLoadControlStrategy != null && customLoadControlStrategy.getCustomLoadControl() != null) {
            return customLoadControlStrategy.getCustomLoadControl();
        } else {
            final LoadControlBuffers loadControl = playerSettings.getLoadControlBuffers();
            if (!loadControl.isDefaultValuesModified()) {
                return new DefaultLoadControl();
            }
            return new ConfigurableLoadControl(new DefaultAllocator(/* trimOnReset= */ true, C.DEFAULT_BUFFER_SEGMENT_SIZE),
                    loadControl.getMaxPlayerBufferMs(),
                    loadControl.getMaxPlayerBufferMs(),
                    loadControl.getMinBufferAfterInteractionMs(),
                    loadControl.getMinBufferAfterReBufferMs(),
                    loadControl.getTargetBufferBytes(),
                    loadControl.getPrioritizeTimeOverSizeThresholds(),
                    loadControl.getBackBufferDurationMs(),
                    loadControl.getRetainBackBufferFromKeyframe());
        }
    }

    @Override
    public void updateLoadControlBuffers(LoadControlBuffers loadControlBuffers) {
        if (configurableLoadControl != null) {
            Handler playerHandler = new Handler(player.getApplicationLooper());
            playerHandler.post(() -> {
//                configurableLoadControl = new ConfigurableLoadControl(new DefaultAllocator(/* trimOnReset= */ true, C.DEFAULT_BUFFER_SEGMENT_SIZE),
//                        loadControlBuffers.getMaxPlayerBufferMs(),
//                        loadControlBuffers.getMaxPlayerBufferMs(),
//                        loadControlBuffers.getMinBufferAfterInteractionMs(),
//                        loadControlBuffers.getMinBufferAfterReBufferMs(),
//                        loadControlBuffers.getTargetBufferBytes(),
//                        loadControlBuffers.getPrioritizeTimeOverSizeThresholds(),
//                        loadControlBuffers.getBackBufferDurationMs(),
//                        loadControlBuffers.getRetainBackBufferFromKeyframe());

                ((ConfigurableLoadControl)configurableLoadControl).setMaxBufferUs(loadControlBuffers.getMaxPlayerBufferMs());
                ((ConfigurableLoadControl)configurableLoadControl).setMinBufferUs(loadControlBuffers.getMaxPlayerBufferMs());
                ((ConfigurableLoadControl)configurableLoadControl).setBufferForPlaybackUs(loadControlBuffers.getMinBufferAfterInteractionMs());
                ((ConfigurableLoadControl)configurableLoadControl).setBufferForPlaybackAfterRebufferUs(loadControlBuffers.getMinBufferAfterReBufferMs());
                ((ConfigurableLoadControl)configurableLoadControl).setBackBufferDurationUs(loadControlBuffers.getBackBufferDurationMs());
                ((ConfigurableLoadControl)configurableLoadControl).setRetainBackBufferFromKeyframe(loadControlBuffers.getRetainBackBufferFromKeyframe());
                ((ConfigurableLoadControl)configurableLoadControl).setTargetBufferBytes(loadControlBuffers.getTargetBufferBytes());
                ((ConfigurableLoadControl)configurableLoadControl).setPrioritizeTimeOverSizeThresholds(loadControlBuffers.getPrioritizeTimeOverSizeThresholds());
            });
        }
    }

    private void setPlayerListeners() {
        log.v("setPlayerListeners");
        if (assertPlayerIsNotNull("setPlayerListeners()")) {
            player.addListener(this);

//            PlaybackStatsListener playbackStatsListener  = new PlaybackStatsListener(true, new PlaybackStatsListener.Callback() {
//                @Override
//            public void onPlaybackStatsReady(com.kaltura.android.exoplayer2.analytics.AnalyticsListener.EventTime eventTime, PlaybackStats playbackStats) {
//                    log.d("PlaybackStatsListener playbackCount = " + playbackStats.playbackCount);
//                }
//            });
//            player.addAnalyticsListener(playbackStatsListener);

            player.addAnalyticsListener(analyticsAggregator);
            final com.kaltura.android.exoplayer2.analytics.AnalyticsListener exoAnalyticsListener = profiler.getExoAnalyticsListener();
            if (exoAnalyticsListener != null) {
                player.addAnalyticsListener(exoAnalyticsListener);
            }
        }
    }

    private DefaultTrackSelector initializeTrackSelector() {

        DefaultTrackSelector trackSelector = new DefaultTrackSelector(context);
        DefaultTrackSelector.Parameters.Builder parametersBuilder = new DefaultTrackSelector.Parameters.Builder(context);
        trackSelectionHelper = new TrackSelectionHelper(context, trackSelector, lastSelectedTrackIds);
        trackSelectionHelper.updateTrackSelectorParameter(playerSettings, parametersBuilder);
        trackSelector.setParameters(parametersBuilder.build());

        trackSelectionHelper.setTracksInfoListener(tracksInfoListener);
        trackSelectionHelper.setTracksErrorListener(tracksErrorListener);

        return trackSelector;
    }

    private void preparePlayer(@NonNull PKMediaSourceConfig sourceConfig) {
        this.sourceConfig = sourceConfig;
        //reset metadata on prepare.
        metadataList.clear();
        isLoadedMetaDataFired = false;
        shouldGetTracksInfo = true;
        // Need to clear the overrides of DefaultTrackSelector before loading the next media.
        trackSelectionHelper.clearPreviousMediaOverrides();
        trackSelectionHelper.applyPlayerSettings(playerSettings);

        MediaSource mediaSource = null;
        MediaItem mediaItem = buildExoMediaItem(sourceConfig);
        if (mediaItem != null && !isLocalMediaItem(sourceConfig) && !isLocalMediaSource(sourceConfig)) {
            mediaSource = buildInternalExoMediaSource(mediaItem, sourceConfig);
        }

        if (mediaItem != null) {
            profiler.onPrepareStarted(sourceConfig);
            if (mediaSource == null) {
                player.setMediaItems(Collections.singletonList(mediaItem), 0, playerPosition == TIME_UNSET ? 0 : playerPosition);
            } else {
                player.setMediaSources(Collections.singletonList(mediaSource), 0, playerPosition == TIME_UNSET ? 0 : playerPosition);
            }

            player.prepare();

            changeState(PlayerState.LOADING);

            if (playerSettings.getSubtitleStyleSettings() != null) {
                configureSubtitleView();
            }
        } else {
            sendPrepareSourceError(sourceConfig);
        }
    }

    private boolean isLocalMediaSource(@NonNull PKMediaSourceConfig sourceConfig) {
        return sourceConfig.mediaSource instanceof LocalAssetsManager.LocalMediaSource;
    }

    private boolean isLocalMediaItem(@NonNull PKMediaSourceConfig sourceConfig) {
        return sourceConfig.mediaSource instanceof LocalAssetsManagerExo.LocalExoMediaItem;
    }

    private void sendPrepareSourceError(@NonNull PKMediaSourceConfig sourceConfig) {
        String errorMessage = "Media Error";
        if (sourceConfig == null) {
            errorMessage += " sourceConfig == null";
        } else if (sourceConfig.mediaSource == null) {
            errorMessage += " sourceConfig.mediaSource == null";
        } else {
            errorMessage += " source = " + sourceConfig.mediaSource.getUrl() + " format = " + sourceConfig.mediaSource.getMediaFormat();
        }
        currentError = new PKError(PKPlayerErrorType.SOURCE_ERROR, PKError.Severity.Fatal, errorMessage, new IllegalArgumentException(errorMessage));
        eventListener.onEvent(PlayerEvent.Type.ERROR);
    }

    private MediaItem buildExoMediaItem(PKMediaSourceConfig sourceConfig) {
        List<PKExternalSubtitle> externalSubtitleList = null;

        if (sourceConfig == null || sourceConfig.mediaSource == null) {
            return null;
        }

        if (sourceConfig.getExternalSubtitleList() != null) {
            externalSubtitleList = sourceConfig.getExternalSubtitleList().size() > 0 ?
                    sourceConfig.getExternalSubtitleList() : null;
        }

        if (externalSubtitleList == null || externalSubtitleList.isEmpty()) {
            if (assertTrackSelectionIsNotNull("buildExoMediaItem")) {
                trackSelectionHelper.hasExternalSubtitles(false);
            }
        } else {
            if (assertTrackSelectionIsNotNull("buildExoMediaItem")) {
                trackSelectionHelper.hasExternalSubtitles(true);
            }
        }

        if (isLocalMediaSource(sourceConfig) && sourceConfig.mediaSource.hasDrmParams()) {
            drmSessionManager = getDeferredDRMSessionManager();
            drmSessionManager.setMediaSource(sourceConfig.mediaSource);
        }

        MediaItem mediaItem;
        if (isLocalMediaItem(sourceConfig)) {
            LocalAssetsManagerExo.LocalExoMediaItem pkMediaSource = (LocalAssetsManagerExo.LocalExoMediaItem) sourceConfig.mediaSource;
            mediaItem = pkMediaSource.getExoMediaItem();
        } else {
            mediaItem = buildInternalExoMediaItem(sourceConfig, externalSubtitleList);
        }

        if (mediaItem == null) {
            return mediaItem;
        }

        if (mediaItem.localConfiguration != null) {
            MediaItem.DrmConfiguration drmConfiguration = mediaItem.localConfiguration.drmConfiguration;
            if (!(sourceConfig.mediaSource instanceof LocalAssetsManager.LocalMediaSource) &&
                    drmConfiguration != null &&
                    drmConfiguration.licenseUri != null &&
                    !TextUtils.isEmpty(drmConfiguration.licenseUri.toString())) {
                if (playerSettings.getDRMSettings().getDrmScheme() != PKDrmParams.Scheme.PlayReadyCENC) {
                    drmSessionManager = getDeferredDRMSessionManager();
                    drmSessionManager.setLicenseUrl(drmConfiguration.licenseUri.toString());
                }
            }
        }

        if (drmSessionManager != null) {
            mediaSourceFactory.setDrmSessionManagerProvider(getDrmSessionManagerProvider(sourceConfig.mediaSource));
        }

        return mediaItem;
    }

    private DeferredDrmSessionManager getDeferredDRMSessionManager() {
        final DrmCallback drmCallback = new DrmCallback(getHttpDataSourceFactory(null), playerSettings.getLicenseRequestAdapter());
        return new DeferredDrmSessionManager(mainHandler, drmCallback, drmSessionListener, playerSettings.allowClearLead(), playerSettings.isForceWidevineL3Playback());
    }

    private DrmSessionManagerProvider getDrmSessionManagerProvider(PKMediaSource pkMediaSource) {
        return drmMediaItem -> {
            if (pkMediaSource.hasDrmParams()) {
                return drmSessionManager;
            } else {
                return DrmSessionManager.DRM_UNSUPPORTED;
            }
        };
    }

    private MediaSource buildInternalExoMediaSource(MediaItem mediaItem, PKMediaSourceConfig sourceConfig) {
        List<PKExternalSubtitle> externalSubtitleList = null;

        if (sourceConfig.getExternalSubtitleList() != null) {
            externalSubtitleList = sourceConfig.getExternalSubtitleList().size() > 0 ?
                    sourceConfig.getExternalSubtitleList() : null;
        }

        PKMediaFormat format = sourceConfig.mediaSource.getMediaFormat();

        if (format == null) {
            return null;
        }

        PKRequestParams requestParams = sourceConfig.getRequestParams();
        final DataSource.Factory dataSourceFactory = getDataSourceFactory(requestParams.headers);
        MediaSource mediaSource;
        switch (format) {
            case dash:

                final DataSource.Factory teedDtaSourceFactory = () -> {
                    dashManifestString = null;
                    dashLastDataSink = new ByteArrayDataSink();
                    TeeDataSource teeDataSource = new TeeDataSource(dataSourceFactory.createDataSource(), dashLastDataSink);
                    teeDataSource.addTransferListener(new TransferListener() {
                        @Override
                        public void onTransferInitializing(@NonNull DataSource dataSource, @NonNull DataSpec dataSpec, boolean b) {

                        }

                        @Override
                        public void onTransferStart(@NonNull DataSource dataSource, @NonNull DataSpec dataSpec, boolean b) {

                        }

                        @Override
                        public void onBytesTransferred(@NonNull DataSource dataSource, @NonNull DataSpec dataSpec, boolean b, int i) {

                        }

                        @Override
                        public void onTransferEnd(@NonNull DataSource dataSource, @NonNull DataSpec dataSpec, boolean b) {
                            log.d("teeDataSource onTransferEnd");
                            if (dashManifestString != null) {
                                return;
                            }
                            if (dashLastDataSink == null) {
                                return;
                            }

                            byte[] bytes = dashLastDataSink.getData();
                            if (bytes == null) {
                                return;
                            }

                            dashManifestString = new String(bytes, Charsets.UTF_8);
                            //log.d("teeDataSource manifest  " + dashManifestString);
                        }
                    });
                    return teeDataSource;
                };

                DashMediaSource.Factory dashMediaSourceFactory = new DashMediaSource.Factory(
                        new DefaultDashChunkSource.Factory(dataSourceFactory), teedDtaSourceFactory)
                        .setManifestParser(new DashManifestParserForThumbnail())
                        .setLoadErrorHandlingPolicy(customLoadErrorHandlingPolicy);

                if (playerSettings.getDRMSettings().getDrmScheme() != PKDrmParams.Scheme.PlayReadyCENC) {
                    dashMediaSourceFactory.setDrmSessionManagerProvider(getDrmSessionManagerProvider(sourceConfig.mediaSource));
                }
                mediaSource = dashMediaSourceFactory.createMediaSource(mediaItem);
                break;

            case hls:
                mediaSource = new HlsMediaSource.Factory(dataSourceFactory)
                        .setDrmSessionManagerProvider(getDrmSessionManagerProvider(sourceConfig.mediaSource))
                        .setLoadErrorHandlingPolicy(customLoadErrorHandlingPolicy)
                        .setAllowChunklessPreparation(playerSettings.isAllowChunklessPreparation())
                        .createMediaSource(mediaItem);
                break;

            // mp4 and mp3 both use ExtractorMediaSource
            case mp4:
            case mp3:
                mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                        .setLoadErrorHandlingPolicy(customLoadErrorHandlingPolicy)
                        .createMediaSource(mediaItem);
                break;

            case udp:
                MulticastSettings multicastSettings = (playerSettings != null) ? playerSettings.getMulticastSettings() : new MulticastSettings();
                if (multicastSettings.getUseExoDefaultSettings()) {
                    mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                            .setLoadErrorHandlingPolicy(customLoadErrorHandlingPolicy)
                            .createMediaSource(mediaItem);
                } else {
                    DataSource.Factory udpDatasourceFactory = () -> new UdpDataSource(multicastSettings.getMaxPacketSize(), multicastSettings.getSocketTimeoutMillis());
                    ExtractorsFactory tsExtractorFactory = () -> new TsExtractor[]{
                            new TsExtractor(multicastSettings.getExtractorMode().mode,
                                    new TimestampAdjuster(multicastSettings.getFirstSampleTimestampUs()), new DefaultTsPayloadReaderFactory())
                    };
                    mediaSource = new ProgressiveMediaSource.Factory(udpDatasourceFactory, tsExtractorFactory)
                            .setLoadErrorHandlingPolicy(customLoadErrorHandlingPolicy)
                            .createMediaSource(mediaItem);
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown media format: " + format + " for url: " + requestParams.url);
        }

        if (externalSubtitleList == null || externalSubtitleList.isEmpty()) {
            return mediaSource;
        } else {
            addExternalTextTrackErrorListener();
            return new MergingMediaSource(buildMediaSourceList(mediaSource, externalSubtitleList));
        }
    }

    /**
     * Return the media source with external subtitles if exists
     * @param externalSubtitleList External subtitle List
     * @return Media Source array
     */

    private MediaSource[] buildMediaSourceList(MediaSource mediaSource, List<PKExternalSubtitle> externalSubtitleList) {
        List<MediaSource> streamMediaSources = new ArrayList<>();
        List<MediaItem.SubtitleConfiguration> mediaItemSubtitles = buildSubtitlesList(externalSubtitleList);
        if (externalSubtitleList != null && externalSubtitleList.size() > 0) {
            for (int subtitlePosition = 0 ; subtitlePosition < externalSubtitleList.size() ; subtitlePosition ++) {
                MediaSource subtitleMediaSource = buildExternalSubtitleSource(mediaItemSubtitles.get(subtitlePosition));
                streamMediaSources.add(subtitleMediaSource);
            }
        }
        // 0th position is secured for dash/hls/extractor media source
        streamMediaSources.add(0, mediaSource);
        return streamMediaSources.toArray(new MediaSource[0]);
    }

    /**
     * Create single Media Source object with each subtitle
     * @param mediaItemSubtitle External subtitle object
     * @return An object of external subtitle media source
     */

    @NonNull
    private MediaSource buildExternalSubtitleSource(MediaItem.SubtitleConfiguration mediaItemSubtitle) {
        return new SingleSampleMediaSource.Factory(getDataSourceFactory(null))
                .setLoadErrorHandlingPolicy(customLoadErrorHandlingPolicy)
                .setTreatLoadErrorsAsEndOfStream(true)
                .createMediaSource(mediaItemSubtitle, C.TIME_UNSET);
    }

    private void removeCustomLoadErrorPolicy() {
        if (customLoadErrorHandlingPolicy != null) {
            customLoadErrorHandlingPolicy.setOnTextTrackErrorListener(null);
            customLoadErrorHandlingPolicy = null;
        }
    }

    private void addCustomLoadErrorPolicy() {
        if (customLoadErrorHandlingPolicy == null) {
            customLoadErrorHandlingPolicy = new CustomLoadErrorHandlingPolicy(playerSettings.getPKRequestConfig().getMaxRetries());
        }
    }

    private void addExternalTextTrackErrorListener() {
        addCustomLoadErrorPolicy();
        customLoadErrorHandlingPolicy.setOnTextTrackErrorListener(err -> {
            currentError = err;
            if (eventListener != null) {
                log.e("Error-Event sent, type = " + currentError.errorType);
                eventListener.onEvent(PlayerEvent.Type.ERROR);
            }
        });
    }

    private MediaItem buildInternalExoMediaItem(PKMediaSourceConfig sourceConfig, List<PKExternalSubtitle> externalSubtitleList) {
        PKMediaFormat format = sourceConfig.mediaSource.getMediaFormat();
        PKRequestParams requestParams = sourceConfig.getRequestParams();

        if (format == null || TextUtils.isEmpty(requestParams.url.toString())) {
            return null; // No MediaItem will be created and returning null will send SOURCE_ERROR with Fatal severity
        }

        MediaItem.ClippingConfiguration clippingConfiguration = new MediaItem.ClippingConfiguration
                .Builder()
                .setStartPositionMs(0L)
                .setEndPositionMs(C.TIME_END_OF_SOURCE)
                .build();

        Uri uri = requestParams.url;
        MediaItem.Builder builder =
                new MediaItem.Builder()
                        .setUri(uri)
                        .setMimeType(format.mimeType)
                        .setSubtitleConfigurations(buildSubtitlesList(externalSubtitleList))
                        .setClippingConfiguration(clippingConfiguration);

        if (playerSettings.getPKLowLatencyConfig() != null && (isLiveMediaWithDvr() || isLiveMediaWithoutDvr())) {
            MediaItem.LiveConfiguration lowLatencyConfiguration = getLowLatencyConfigFromPlayerSettings();
            builder.setLiveConfiguration(lowLatencyConfiguration);
        }

        if (format == PKMediaFormat.dash || (format == PKMediaFormat.hls && sourceConfig.mediaSource.hasDrmParams())) {
            setMediaItemBuilderDRMParams(sourceConfig, builder);
        } else  if (format == PKMediaFormat.udp) {
            builder.setMimeType(null);
        }
        return builder.build();
    }

    @NonNull
    private MediaItem.LiveConfiguration getLowLatencyConfigFromPlayerSettings() {
        MediaItem.LiveConfiguration.Builder liveConfigurationBuilder = new MediaItem.LiveConfiguration.Builder();

        if (playerSettings.getPKLowLatencyConfig().getTargetOffsetMs() > 0) {
            liveConfigurationBuilder.setTargetOffsetMs(playerSettings.getPKLowLatencyConfig().getTargetOffsetMs());
        }
        if (playerSettings.getPKLowLatencyConfig().getMinOffsetMs() > 0) {
            liveConfigurationBuilder.setMinOffsetMs(playerSettings.getPKLowLatencyConfig().getMinOffsetMs());
        }
        if (playerSettings.getPKLowLatencyConfig().getMaxOffsetMs() > 0) {
            liveConfigurationBuilder.setMaxOffsetMs(playerSettings.getPKLowLatencyConfig().getMaxOffsetMs());
        }
        if (playerSettings.getPKLowLatencyConfig().getMinPlaybackSpeed() > 0) {
            liveConfigurationBuilder.setMinPlaybackSpeed(playerSettings.getPKLowLatencyConfig().getMinPlaybackSpeed());
        }
        if (playerSettings.getPKLowLatencyConfig().getMaxPlaybackSpeed() > 0) {
            liveConfigurationBuilder.setMaxPlaybackSpeed(playerSettings.getPKLowLatencyConfig().getMaxPlaybackSpeed());
        }

        return liveConfigurationBuilder.build();
    }

    private void setMediaItemBuilderDRMParams(PKMediaSourceConfig sourceConfig, MediaItem.Builder builder) {
        PKDrmParams.Scheme scheme = playerSettings.getDRMSettings().getDrmScheme();
        log.d("PKDrmParams.Scheme = " + scheme);

        String licenseUri = getDrmLicenseUrl(sourceConfig.mediaSource, scheme);
        UUID uuid = (scheme == PKDrmParams.Scheme.WidevineCENC) ? MediaSupport.WIDEVINE_UUID : MediaSupport.PLAYREADY_UUID;
        boolean isForceDefaultLicenseUri = playerSettings.getDRMSettings().getIsForceDefaultLicenseUri();

        if (!TextUtils.isEmpty(licenseUri) ||
                (uuid == MediaSupport.PLAYREADY_UUID && TextUtils.isEmpty(licenseUri) && !isForceDefaultLicenseUri)) {

            MediaItem.DrmConfiguration.Builder drmConfigurationBuilder = new MediaItem.DrmConfiguration
                    .Builder(uuid)
                    .setLicenseUri(licenseUri)
                    .setMultiSession(playerSettings.getDRMSettings().getIsMultiSession())
                    .setForceDefaultLicenseUri(isForceDefaultLicenseUri);

            Map<String, String> licenseRequestParamsHeaders = getLicenseRequestParamsHeaders(licenseUri);
            if (licenseRequestParamsHeaders != null) {
                drmConfigurationBuilder.setLicenseRequestHeaders(licenseRequestParamsHeaders);
            }

            builder.setDrmConfiguration(drmConfigurationBuilder.build());
        }
    }

    /**
     * Get the license request headers from the Adapter
     *
     * @param licenseUri license URI for the media
     * @return return the license request header's map
     */
    @Nullable
    private Map<String, String> getLicenseRequestParamsHeaders(String licenseUri) {
        Map<String, String> licenseRequestParamsHeaders = null;
        if (!TextUtils.isEmpty(licenseUri)) {
            PKRequestParams licenseRequestParams = new PKRequestParams(Uri.parse(licenseUri), new HashMap<>());
            if (playerSettings.getLicenseRequestAdapter() != null) {
                licenseRequestParams = playerSettings.getLicenseRequestAdapter().adapt(licenseRequestParams);
            }
            licenseRequestParamsHeaders = licenseRequestParams.headers;
        }
        return licenseRequestParamsHeaders;
    }

    @Nullable
    private String getDrmLicenseUrl(PKMediaSource mediaSource, PKDrmParams.Scheme scheme) {
        String licenseUrl = null;

        if (mediaSource.hasDrmParams()) {
            List<PKDrmParams> drmData = mediaSource.getDrmData();
            for (PKDrmParams pkDrmParam : drmData) {
                if (scheme == pkDrmParam.getScheme()) {
                    licenseUrl = pkDrmParam.getLicenseUri();
                    break;
                }
            }
        }
        return licenseUrl;
    }

    private List<MediaItem.SubtitleConfiguration> buildSubtitlesList(List<PKExternalSubtitle> externalSubtitleList) {
        List<MediaItem.SubtitleConfiguration> subtitleList = new ArrayList<>();

        if (externalSubtitleList != null && externalSubtitleList.size() > 0) {
            for (int subtitlePosition = 0 ; subtitlePosition < externalSubtitleList.size() ; subtitlePosition ++) {
                PKExternalSubtitle pkExternalSubtitle = externalSubtitleList.get(subtitlePosition);
                String subtitleMimeType = pkExternalSubtitle.getMimeType() == null ? "Unknown" : pkExternalSubtitle.getMimeType();

                MediaItem.SubtitleConfiguration.Builder builder = new MediaItem.SubtitleConfiguration.Builder(Uri.parse(pkExternalSubtitle.getUrl()));
                builder.setMimeType(subtitleMimeType);

                // "-" is important to be added between lang and mimetype
                // This is how we understand that this is external subtitle in TrackSelectionHelper
                builder.setLanguage(pkExternalSubtitle.getLanguage() + "-" + subtitleMimeType);

                builder.setSelectionFlags(pkExternalSubtitle.getSelectionFlags());
                builder.setRoleFlags(pkExternalSubtitle.getRoleFlag());
                builder.setLabel(pkExternalSubtitle.getLabel());

                MediaItem.SubtitleConfiguration subtitleMediaItem = builder.build();
                subtitleList.add(subtitleMediaItem);
            }
        }
        return subtitleList;
    }

    private HttpDataSource.Factory getHttpDataSourceFactory(Map<String, String> headers) {
        HttpDataSource.Factory httpDataSourceFactory;
        final String userAgent = getUserAgent(context);

        final PKRequestConfig pkRequestConfig = playerSettings.getPKRequestConfig();
        final int connectTimeout = pkRequestConfig.getConnectTimeoutMs();
        final int readTimeout = pkRequestConfig.getReadTimeoutMs();
        final boolean crossProtocolRedirectEnabled = pkRequestConfig.getCrossProtocolRedirectEnabled();

        if (CookieHandler.getDefault() == null) {
            CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER));
        }

        if (!PKHttpClientManager.useSystem()) {

            final OkHttpClient.Builder builder = PKHttpClientManager.newClientBuilder()
                    .cookieJar(NativeCookieJarBridge.sharedCookieJar)
                    .followRedirects(true)
                    .followSslRedirects(crossProtocolRedirectEnabled)
                    .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                    .readTimeout(readTimeout, TimeUnit.MILLISECONDS);
            builder.eventListener(analyticsAggregator);
            if (profiler != Profiler.NOOP) {
                final okhttp3.EventListener.Factory okListenerFactory = profiler.getOkListenerFactory();
                if (okListenerFactory != null) {
                    builder.eventListenerFactory(okListenerFactory);
                }
            }
            httpDataSourceFactory = new OkHttpDataSource.Factory((Call.Factory) builder.build()).setUserAgent(userAgent);
        } else {

            httpDataSourceFactory = new DefaultHttpDataSource.Factory().setUserAgent(userAgent)
                    .setConnectTimeoutMs(connectTimeout)
                    .setReadTimeoutMs(readTimeout)
                    .setAllowCrossProtocolRedirects(crossProtocolRedirectEnabled);
        }

        if (headers != null && !headers.isEmpty()) {
            httpDataSourceFactory.setDefaultRequestProperties(headers);
        }
        return httpDataSourceFactory;
    }

    private DataSource.Factory getDataSourceFactory(Map<String, String> headers) {
        DataSource.Factory httpDataSourceFactory = new DefaultDataSource.Factory(context, getHttpDataSourceFactory(headers));
        if (downloadCache != null) {
            return buildReadOnlyCacheDataSource(httpDataSourceFactory, downloadCache);
        } else {
            return httpDataSourceFactory;
        }
    }

    private static String getUserAgent(Context context) {
        return Utils.getUserAgent(context) + " ExoPlayerLib/" + ExoPlayerLibraryInfo.VERSION;
    }

    private void changeState(PlayerState newState) {
        previousState = currentState;
        if (newState.equals(currentState)) {
            return;
        }
        this.currentState = newState;
        if (stateChangedListener != null) {
            stateChangedListener.onStateChanged(previousState, currentState);
        }
    }

    private CacheDataSource.Factory buildReadOnlyCacheDataSource(
            DataSource.Factory upstreamFactory, Cache cache) {
        return new CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(upstreamFactory)
                .setCacheWriteDataSinkFactory(null)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
    }

    private void sendDistinctEvent(PlayerEvent.Type newEvent) {
        if (newEvent.equals(currentEvent)) {
            return;
        }
        sendEvent(newEvent);
    }

    private void sendEvent(PlayerEvent.Type event) {
        if (shouldRestorePlayerToPreviousState && event != PlayerEvent.Type.DURATION_CHANGE &&
                !(currentEvent == PlayerEvent.Type.PAUSE && event == PlayerEvent.Type.PLAY)) {
            log.i("Trying to send event " + event.name() + ". Should be blocked from sending now, because the player is restoring to the previous state.");
            return;
        }

        if (event == PlayerEvent.Type.LOADED_METADATA) {
            isLoadedMetaDataFired = true;
        }

        currentEvent = event;
        if (eventListener != null) {
            if (event != PlayerEvent.Type.PLAYBACK_INFO_UPDATED) {
                log.d("Event sent: " + event.name());
            }
            eventListener.onEvent(currentEvent);
        } else {
            log.e("eventListener is null cannot send Event: " + event.name());
        }
    }

    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        if (isPlaying) {
            sendDistinctEvent(PlayerEvent.Type.PLAYING);
        }
    }

    @Override
    public void onPlaybackSuppressionReasonChanged(int playbackSuppressionReason) {
        log.d("onPlaybackSuppressionReasonChanged. playbackSuppressionReason => " + playbackSuppressionReason);
    }


    @Override
    public void onPlaybackStateChanged(int playbackState) {
        switch (playbackState) {
            case Player.STATE_IDLE:
                log.d("onPlayerStateChanged = IDLE");
                changeState(PlayerState.IDLE);
                if (isSeeking) {
                    isSeeking = false;
                }
                break;

            case Player.STATE_BUFFERING:
                log.d("onPlayerStateChanged = BUFFERING");
                changeState(PlayerState.BUFFERING);
                break;

            case Player.STATE_READY:
                log.d("onPlayerStateChanged. READY");
                changeState(PlayerState.READY);

                if (isSeeking) {
                    isSeeking = false;
                    sendDistinctEvent(PlayerEvent.Type.SEEKED);
                }

                if (!previousState.equals(PlayerState.READY)) {
                    sendDistinctEvent(PlayerEvent.Type.CAN_PLAY);
                }
                break;

            case Player.STATE_ENDED:
                log.d("onPlayerStateChanged = ENDED");
                pausePlayerAfterEndedEvent();
                changeState(PlayerState.IDLE);
                sendDistinctEvent(PlayerEvent.Type.ENDED);
                break;

            default:
                break;
        }
    }

    private void pausePlayerAfterEndedEvent() {
        if (PlayerEvent.Type.ENDED != currentEvent) {
            log.d("Pause pausePlayerAfterEndedEvent");
            player.setPlayWhenReady(false);
        }
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {
        // TODO: if/when we start using ExoPlayer's repeat mode, listen to this event.
    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
        // TODO: implement if we add playlist support
    }

    @Override
    public void onTimelineChanged(@NonNull Timeline timeline, int reason) {
        log.d("onTimelineChanged reason = " + reason + " duration = " + getDuration());
        if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
            isLoadedMetaDataFired = false;
            if (getDuration() != TIME_UNSET || PKMediaFormat.udp.equals(sourceConfig.mediaSource.getMediaFormat())) {
                sendDistinctEvent(PlayerEvent.Type.DURATION_CHANGE);
                profiler.onDurationChanged(getDuration());
            }
        }

        if (reason == Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE) {
            if (getDuration() != TIME_UNSET) {
                if (!isLoadedMetaDataFired) {
                    sendDistinctEvent(PlayerEvent.Type.LOADED_METADATA);
                }
                sendDistinctEvent(PlayerEvent.Type.DURATION_CHANGE);
            }

            if (player.getCurrentManifest() instanceof DashManifest) {
                if (((DashManifest) player.getCurrentManifest()).getPeriodCount() > 0) {
                    List<EventStream> eventStreamList = ((DashManifest) player.getCurrentManifest()).getPeriod(0).eventStreams;
                    if (!eventStreamList.isEmpty()) {
                        eventStreams = eventStreamList;
                        sendDistinctEvent(PlayerEvent.Type.EVENT_STREAM_CHANGED);
                    }
                }
            }
        }
    }

    @Override
    public void onPlayerError(PlaybackException playbackException) {
        log.d("onPlayerError error type => " + playbackException.errorCode);
        if (isBehindLiveWindow(playbackException) && sourceConfig != null) {
            log.d("onPlayerError BehindLiveWindowException received, re-preparing player");
            MediaItem mediaItem = buildExoMediaItem(sourceConfig);
            if (mediaItem != null) {
                PKMediaFormat format = sourceConfig.mediaSource.getMediaFormat();
                if (format == null) {
                    player.setMediaItems(Collections.singletonList(mediaItem), 0, C.TIME_UNSET);
                } else {
                    player.setMediaSources(Collections.singletonList(buildInternalExoMediaSource(mediaItem, sourceConfig)), 0, playerPosition == TIME_UNSET ? 0 : playerPosition);
                }
                player.prepare();
            } else {
                sendPrepareSourceError(sourceConfig);
            }
            return;
        }

        // Fire the more accurate error codes coming under PlaybackException from ExoPlayer
        Pair<PKPlayerErrorType, String> exceptionPair = PKPlaybackException.getPlaybackExceptionType(playbackException);
        if (exceptionPair.first == PKPlayerErrorType.TIMEOUT && exceptionPair.second.contains(Consts.EXO_TIMEOUT_OPERATION_RELEASE)) {
            // ExoPlayer is being stopped internally in other EXO_TIMEOUT_EXCEPTION types
            currentError = new PKError(PKPlayerErrorType.TIMEOUT, PKError.Severity.Recoverable, exceptionPair.second, playbackException);
        } else {
            currentError = new PKError(exceptionPair.first, exceptionPair.second, playbackException);
        }
        log.e("ExoPlaybackException, type = " + exceptionPair.first);

        if (eventListener != null) {
            log.e("Error-Event Sent");
            eventListener.onEvent(PlayerEvent.Type.ERROR);
        } else {
            log.e("eventListener is null cannot send Error-Event type = " + playbackException.getErrorCodeName());
        }
    }

    @Override
    public void onPlaybackParametersChanged(@NonNull PlaybackParameters playbackParameters) {
        sendEvent(PlayerEvent.Type.PLAYBACK_RATE_CHANGED);
    }

    @Override
    public void onPositionDiscontinuity(@NonNull Player.PositionInfo oldPosition, @NonNull Player.PositionInfo newPosition, @Player.DiscontinuityReason int reason) {
        log.d("onPositionDiscontinuity reason = " + reason);
    }

    @Override
    public void onTracksChanged(@NonNull Tracks tracks) {
        log.d("onTracksChanged");

        //if onTracksChanged happened when application went background, do not update the tracks.
        if (assertTrackSelectionIsNotNull("onTracksChanged()")) {
            //if the track info new -> map the available tracks. and when ready, notify user about available tracks.
            if (shouldGetTracksInfo) {
                CustomDashManifest customDashManifest = null;
                // Local Configuration includes the local playback properties
                MediaItem.LocalConfiguration localConfiguration = player.getMediaItemAt(0).localConfiguration;
                if (!TextUtils.isEmpty(dashManifestString) && dashLastDataSink != null
                        && player.getCurrentManifest() instanceof DashManifest && localConfiguration != null) {
                    // byte[] bytes = dashLastDataSink.getData();
                    try {
                        customDashManifest = new CustomDashManifestParser().parse(localConfiguration.uri, dashManifestString);
                    } catch (IOException e) {
                        log.e("imageTracks assemble error " + e.getMessage());
                    } finally {
                        dashLastDataSink = null;
                        dashManifestString = null;
                    }
                }
                shouldGetTracksInfo = !trackSelectionHelper.prepareTracks(tracks, sourceConfig.getExternalVttThumbnailUrl(), customDashManifest);
            }
            trackSelectionHelper.notifyAboutTrackChange(tracks);
        }
    }

    @Override
    public void onVideoInputFormatChanged(@NonNull Format format) {
        if (assertTrackSelectionIsNotNull("onVideoInputFormatChanged")) {
            trackSelectionHelper.setCurrentVideoFormat(format);
        }
    }

    @Override
    public void onAudioInputFormatChanged(@NonNull Format format) {
        if (assertTrackSelectionIsNotNull("onAudioInputFormatChanged")) {
            trackSelectionHelper.setCurrentAudioFormat(format);
        }
    }

    @Override
    public void onMetadata(@NonNull Metadata metadata) {
        this.metadataList = MetadataConverter.convert(metadata);
        sendEvent(PlayerEvent.Type.METADATA_AVAILABLE);
    }

    @Override
    public void load(PKMediaSourceConfig mediaSourceConfig) {
        log.d("load");

        if (player == null) {
            this.useTextureView = playerSettings.useTextureView();
            this.isSurfaceSecured = playerSettings.isSurfaceSecured();
            initializePlayer();
        } else {
            // for change media case need to verify if surface swap is needed
            maybeChangePlayerRenderView();
        }
        preparePlayer(mediaSourceConfig);
    }

    private boolean isBehindLiveWindow(PlaybackException e) {
        return e.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW;
    }

    private void maybeChangePlayerRenderView() {
        // no need to swap video surface if no change was done in surface settings
        if (this.useTextureView == playerSettings.useTextureView() && this.isSurfaceSecured == playerSettings.isSurfaceSecured()) {
            exoPlayerView.toggleVideoViewVisibility(playerSettings.isVideoViewHidden());
            return;
        }
        if (playerSettings.useTextureView() && playerSettings.isSurfaceSecured()) {
            log.w("Using TextureView with secured surface is not allowed. Secured surface request will be ignored.");
        }

        this.useTextureView = playerSettings.useTextureView();
        this.isSurfaceSecured = playerSettings.isSurfaceSecured();
        exoPlayerView.setVideoSurfaceProperties(playerSettings.useTextureView(), playerSettings.isSurfaceSecured(), playerSettings.isVideoViewHidden());
    }

    @Override
    public PlayerView getView() {
        return exoPlayerView;
    }

    @Override
    public void play() {
        log.v("play");
        if (assertPlayerIsNotNull("play()")) {
            //If player already set to play, return.
            if (player.getPlayWhenReady()) {
                return;
            }

            if (!rootViewUpdated) {
                rootView.addView(getView(),0);
                rootViewUpdated = true;
            }

            sendDistinctEvent(PlayerEvent.Type.PLAY);
            if (isLiveMediaWithoutDvr()) {
                player.seekToDefaultPosition();
            }
            //trackSelectionHelper.disabledVideoTrack(false);
            profiler.onPlayRequested();
            player.setPlayWhenReady(true);
        }
    }

    @Override
    public void pause() {
        log.v("pause");
        if (assertPlayerIsNotNull("pause()")) {
            //If player already set to pause, return.
            //trackSelectionHelper.disabledVideoTrack(true);
            if (!player.getPlayWhenReady()) {
                return;
            }

            if (currentEvent == PlayerEvent.Type.ENDED) {
                return;
            }
            sendDistinctEvent(PlayerEvent.Type.PAUSE);
            profiler.onPauseRequested();
            player.setPlayWhenReady(false);
        }
    }

    @Override
    public long getCurrentPosition() {
        log.v("getCurrentPosition");
        if (assertPlayerIsNotNull("getCurrentPosition()")) {
            return player.getCurrentPosition();
        }
        return Consts.POSITION_UNSET;
    }

    @Override
    public long getPositionInWindowMs() {
        log.v("getPositionInWindowMs");
        long positionInWindowMs = 0;
        if (assertPlayerIsNotNull("getPositionInWindowMs()")) {
            Timeline currentTimeline = player.getCurrentTimeline();
            if (!currentTimeline.isEmpty()) {
                return currentTimeline.getPeriod(player.getCurrentPeriodIndex(), period).getPositionInWindowMs();
            }
        }
        return positionInWindowMs;
    }

    @Override
    public long getProgramStartTime() {
        log.v("getProgramStartTime");
        long windowStartTimeMs = TIME_UNSET;
        if (assertPlayerIsNotNull("getProgramStartTime()")) {
            final int currentWindowIndex = player.getCurrentMediaItemIndex();
            if (currentWindowIndex == C.INDEX_UNSET) {
                return windowStartTimeMs;
            }
            if (player.getCurrentTimeline() != null && currentWindowIndex >= 0 && currentWindowIndex < player.getCurrentTimeline().getWindowCount()) {
                final Timeline.Window window = player.getCurrentTimeline().getWindow(currentWindowIndex, new Timeline.Window());
                if (window == null) {
                    return windowStartTimeMs;
                }
                windowStartTimeMs = window.windowStartTimeMs;
            }
        }
        return windowStartTimeMs;
    }

    @Override
    public void seekTo(long position) {
        log.v("seekTo");

        if (assertPlayerIsNotNull("seekTo()")) {
            isSeeking = true;
            sendDistinctEvent(PlayerEvent.Type.SEEKING);
            profiler.onSeekRequested(position);
            if (player.getDuration() == TIME_UNSET) {
                return;
            }
            if (isLive() && position >= player.getDuration()) {
                player.seekToDefaultPosition();
            } else {
                if (position < 0) {
                    position = 0;
                } else if (position > player.getDuration()) {
                    position = player.getDuration();
                }
                player.seekTo(position);
            }
        }
    }

    @Override
    public void seekToDefaultPosition() {
        log.v("seekToDefaultPosition");
        if (assertPlayerIsNotNull("seekToDefaultPosition()")) {
            player.seekToDefaultPosition();
        }
    }

    @Override
    public long getDuration() {
        log.v("getDuration");
        if (assertPlayerIsNotNull("getDuration()")) {
            return player.getDuration();
        }
        return TIME_UNSET;
    }

    @Override
    public long getBufferedPosition() {
        log.v("getBufferedPosition");
        if (assertPlayerIsNotNull("getBufferedPosition()")) {
            return player.getBufferedPosition();
        }
        return Consts.POSITION_UNSET;
    }

    @Override
    public long getCurrentLiveOffset() {
        log.v("getCurrentLiveOffset");
        if (assertPlayerIsNotNull("getCurrentLiveOffset()")) {
            return player.getCurrentLiveOffset();
        }
        return TIME_UNSET;
    }

    @Override
    public void release() {
        log.v("release");
        if (assertPlayerIsNotNull("release()")) {
            savePlayerPosition();
            player.release();
            player = null;
            if (bandwidthMeter != null) {
                bandwidthMeter.removeEventListener(this);
            }
            removeCustomLoadErrorPolicy();
            if (assertTrackSelectionIsNotNull("release()")) {
                trackSelectionHelper.release();
                trackSelectionHelper = null;
            }
        }
        isPlayerReleased = true;
        shouldRestorePlayerToPreviousState = true;
    }

    @Override
    public void restore() {
        log.v("restore");
        if (player == null) {
            if (bandwidthMeter != null) {
                bandwidthMeter.addEventListener(mainHandler, this);
            }
            initializePlayer();
            setVolume(lastKnownVolume);
            setPlaybackRate(lastKnownPlaybackRate);
        }

        if (playerPosition == TIME_UNSET || isLiveMediaWithoutDvr()) {
            player.seekToDefaultPosition();
        } else {
            if (isPlayerReleased) {
                player.seekTo(playerWindow, playerPosition);
            } else {
                playerPosition = TIME_UNSET;
            }
        }

        isPlayerReleased = false;
    }

    private boolean isLiveMediaWithoutDvr() {
        if (sourceConfig != null) {
            return (PKMediaEntry.MediaEntryType.Live == sourceConfig.mediaEntryType);
        }
        return false;
    }

    private boolean isLiveMediaWithDvr() {
        if (sourceConfig != null) {
            return (PKMediaEntry.MediaEntryType.DvrLive == sourceConfig.mediaEntryType);
        }
        return false;
    }

    @Override
    public void destroy() {
        log.v("destroy");
        closeProfilerSession();
        removeCustomLoadErrorPolicy();
        if (assertPlayerIsNotNull("destroy()")) {
            player.release();
        }
        window = null;
        player = null;
        if (exoPlayerView != null) {
            exoPlayerView.removeAllViews();
        }
        exoPlayerView = null;
        playerPosition = TIME_UNSET;
    }

    @Override
    public void changeTrack(String uniqueId) {
        if (assertTrackSelectionIsNotNull("changeTrack()")) {
            try {
                trackSelectionHelper.changeTrack(uniqueId);
            } catch (IllegalArgumentException ex) {
                int trackTypeId = trackSelectionHelper.getTrackTypeId(uniqueId);
                if (trackTypeId >= 0) {
                   lastSelectedTrackIds[trackTypeId] = TrackSelectionHelper.NONE;
                }
                sendTrackSelectionError(uniqueId, ex);
            }
        }
    }

    @Override
    public void overrideMediaDefaultABR(long minAbr, long maxAbr, PKAbrFilter pkAbrFilter) {
        if (trackSelectionHelper == null) {
            log.w("Attempt to invoke 'overrideMediaDefaultABR()' on null instance of the tracksSelectionHelper");
            return;
        }

        if (minAbr > maxAbr || maxAbr <= 0) {
            minAbr = Long.MIN_VALUE;
            maxAbr = Long.MAX_VALUE;
            pkAbrFilter = PKAbrFilter.NONE;
            String errorMessage = "Either given min ABR value is greater than max ABR or max ABR is <= 0";
            sendInvalidVideoBitrateRangeIfNeeded(errorMessage);
        }

        trackSelectionHelper.overrideMediaDefaultABR(minAbr, maxAbr, pkAbrFilter);
    }

    @Override
    public void overrideMediaVideoCodec() {
        if (trackSelectionHelper == null) {
            log.w("Attempt to invoke 'overrideMediaVideoCodec()' on null instance of the TracksSelectionHelper");
            return;
        }

        trackSelectionHelper.overrideMediaVideoCodec();
    }

    private void sendTrackSelectionError(String uniqueId, IllegalArgumentException invalidUniqueIdException) {
        String errorStr = "Track Selection failed uniqueId = " + uniqueId;
        log.e(errorStr);
        currentError = new PKError(PKPlayerErrorType.TRACK_SELECTION_FAILED, errorStr, invalidUniqueIdException);
        if (eventListener != null) {
            log.e("Error-Event sent, type = " + PKPlayerErrorType.TRACK_SELECTION_FAILED);
            eventListener.onEvent(PlayerEvent.Type.ERROR);
        } else {
            log.e("eventListener is null cannot send Error-Event type = " + PKPlayerErrorType.TRACK_SELECTION_FAILED + " uniqueId = " + uniqueId);
        }
    }

    public PKTracks getPKTracks() {
        return this.tracks;
    }

    @Override
    public void startFrom(long position) {
        log.v("startFrom");
        if (assertPlayerIsNotNull("startFrom()")) {
            if (shouldRestorePlayerToPreviousState) {
                log.i("Restoring player from previous known state. So skip this block.");
                return;
            }
            isSeeking = false;
            playerPosition = position;
            player.seekTo(position);
        }
    }

    @Override
    public void setEventListener(final EventListener eventTrigger) {
        this.eventListener = eventTrigger;
    }

    @Override
    public void setStateChangedListener(StateChangedListener stateChangedTrigger) {
        this.stateChangedListener = stateChangedTrigger;
    }

    @Override
    public void setAnalyticsListener(AnalyticsListener analyticsListener) {
        this.analyticsAggregator.setListener(analyticsListener);
    }

    @Override
    public void setInputFormatChangedListener(Boolean enableListener) {
        this.analyticsAggregator.setInputFormatChangedListener(enableListener != null ? this : null);
    }

    @Override
    public void setRedirectedManifestURL(String redirectedManifestURL) {
        if (player.getCurrentMediaItem() != null &&
                player.getCurrentMediaItem().localConfiguration != null  &&
                player.getCurrentMediaItem().localConfiguration.uri != null) {
            trackSelectionHelper.setRedirectedManifestURL(player.getCurrentMediaItem().localConfiguration.uri.toString(), redirectedManifestURL);
        }
    }

    @Override
    public void replay() {
        log.v("replay");
        if (assertPlayerIsNotNull("replay()")) {
            isSeeking = false;
            profiler.onReplayRequested();
            player.seekTo(0);
            player.setPlayWhenReady(true);
            sendDistinctEvent(PlayerEvent.Type.REPLAY);
        }
    }

    @Override
    public void setVolume(float volume) {
        log.v("setVolume");
        if (assertPlayerIsNotNull("setVolume()")) {
            this.lastKnownVolume = volume;
            if (lastKnownVolume < 0) {
                lastKnownVolume = 0;
            } else if (lastKnownVolume > 1) {
                lastKnownVolume = 1;
            }

            if (volume != player.getVolume()) {
                player.setVolume(lastKnownVolume);
                sendEvent(PlayerEvent.Type.VOLUME_CHANGED);
            }
        }
    }

    @Override
    public float getVolume() {
        log.v("getVolume");
        if (assertPlayerIsNotNull("getVolume()")) {
            return player.getVolume();
        }
        return Consts.VOLUME_UNKNOWN;
    }

    @Override
    public boolean isPlaying() {
        log.v("isPlaying");
        if (assertPlayerIsNotNull("isPlaying()")) {
            return player.isPlaying() || (player.getPlayWhenReady() && (currentState == PlayerState.READY || currentState == PlayerState.BUFFERING));
        }
        return false;
    }

    @Override
    public PlaybackInfo getPlaybackInfo() {
        if (bandwidthMeter == null) {
            log.e("BandwidthMeter is null");
            return null;
        }

        if (trackSelectionHelper == null) {
            log.e("TrackSelectionHelper is null");
            return null;
        }

        return new PlaybackInfo(trackSelectionHelper.getCurrentVideoBitrate(),
                trackSelectionHelper.getCurrentAudioBitrate(),
                bandwidthMeter.getBitrateEstimate(),
                trackSelectionHelper.getCurrentVideoWidth(),
                trackSelectionHelper.getCurrentVideoHeight());
    }

    @Override
    public PKError getCurrentError() {
        return currentError;
    }

    @Override
    public void stop() {
        log.v("stop");

        preferredLanguageWasSelected = false;
        lastKnownVolume = Consts.DEFAULT_VOLUME;
        lastKnownPlaybackRate = Consts.DEFAULT_PLAYBACK_RATE_SPEED;
        lastSelectedTrackIds = new String[]{TrackSelectionHelper.NONE, TrackSelectionHelper.NONE, TrackSelectionHelper.NONE, TrackSelectionHelper.NONE};
        if (assertTrackSelectionIsNotNull("stop()")) {
            trackSelectionHelper.stop();
        }

        playerPosition = TIME_UNSET;

        if (assertPlayerIsNotNull("stop()")) {
            player.setPlayWhenReady(false);
            player.stop();
            player.clearMediaItems();
        }

        analyticsAggregator.reset();

        closeProfilerSession();
    }

    private void savePlayerPosition() {
        log.v("savePlayerPosition");
        if (assertPlayerIsNotNull("savePlayerPosition()")) {
            currentError = null;
            playerWindow = player.getCurrentMediaItemIndex();
            Timeline timeline = player.getCurrentTimeline();
            if (timeline != null && !timeline.isEmpty()  && playerWindow >= 0 && playerWindow < player.getCurrentTimeline().getWindowCount() && timeline.getWindow(playerWindow, window).isSeekable) {
                playerPosition = player.getCurrentPosition();
            }
        }
    }

    public List<PKMetadata> getMetadata() {
        return metadataList;
    }

    private TrackSelectionHelper.TracksErrorListener initTracksErrorListener() {
        return new TrackSelectionHelper.TracksErrorListener() {
            @Override
            public void onTracksOverrideABRError(PKError pkError) {
                currentError = pkError;
                if (eventListener != null) {
                    eventListener.onEvent(PlayerEvent.Type.ERROR);
                }
            }

            @Override
            public void onUnsupportedVideoTracksError(PKError pkError) {
                currentError = pkError;
                if (eventListener != null) {
                    eventListener.onEvent(PlayerEvent.Type.ERROR);
                }
            }

            @Override
            public void onUnsupportedAudioTracksError(PKError pkError) {
                currentError = pkError;
                if (eventListener != null) {
                    eventListener.onEvent(PlayerEvent.Type.ERROR);
                }
            }

            @Override
            public void onUnsupportedAudioVideoTracksError(PKError pkError) {
                currentError = pkError;
                if (eventListener != null) {
                    eventListener.onEvent(PlayerEvent.Type.ERROR);
                }
            }

            @Override
            public void onUnsupportedTracksAvailableError(PKError pkError) {
                currentError = pkError;
                if (eventListener != null) {
                    eventListener.onEvent(PlayerEvent.Type.ERROR);
                }
            }
        };
    }

    private TrackSelectionHelper.TracksInfoListener initTracksInfoListener() {
        return new TrackSelectionHelper.TracksInfoListener() {
            @Override
            public void onTracksInfoReady(PKTracks tracksReady) {
                HashMap<PKAbrFilter, Pair<Long, Long>> abrPrecedence = checkABRPriority();
                PKAbrFilter abrFilter = getPKAbrFilter(abrPrecedence);
                boolean isABREnabled = abrFilter != PKAbrFilter.NONE;

                if(isABREnabled) {
                    overrideMediaDefaultABR(abrPrecedence.get(abrFilter).first, abrPrecedence.get(abrFilter).second, abrFilter);
                } else {
                    overrideMediaVideoCodec();
                }

                //when the track info is ready, cache it in ExoPlayerWrapper. And send event that tracks are available.
                tracks = tracksReady;
                shouldRestorePlayerToPreviousState = false;

                if (!preferredLanguageWasSelected) {
                    selectPreferredTracksLanguage(tracksReady);
                    preferredLanguageWasSelected = true;
                }
                sendDistinctEvent(PlayerEvent.Type.TRACKS_AVAILABLE);
                if (exoPlayerView != null) {
                    if (assertTrackSelectionIsNotNull("initTracksInfoListener()") && trackSelectionHelper.isAudioOnlyStream()) {
                        exoPlayerView.hideVideoSurface();
                    }

                    if (!tracksReady.getTextTracks().isEmpty()) {
                        exoPlayerView.showVideoSubtitles();
                    }
                }
            }

            @Override
            public void onRelease(String[] selectedTrackIds) {
                lastSelectedTrackIds = selectedTrackIds;
            }

            @Override
            public void onVideoTrackChanged() {
                sendEvent(PlayerEvent.Type.VIDEO_TRACK_CHANGED);
                sendEvent(PlayerEvent.Type.PLAYBACK_INFO_UPDATED);
            }

            @Override
            public void onAudioTrackChanged() {
                sendEvent(PlayerEvent.Type.AUDIO_TRACK_CHANGED);
                sendEvent(PlayerEvent.Type.PLAYBACK_INFO_UPDATED);
            }

            @Override
            public void onTextTrackChanged() {
                sendEvent(PlayerEvent.Type.TEXT_TRACK_CHANGED);
            }

            @Override
            public void onImageTrackChanged() {
                sendEvent(PlayerEvent.Type.IMAGE_TRACK_CHANGED);
            }

            @Override
            public void onEventStreamsChanged(List<EventStream> eventStreamList) {
                eventStreams = eventStreamList;
                sendDistinctEvent(PlayerEvent.Type.EVENT_STREAM_CHANGED);
            }
        };
    }

    private PKAbrFilter getPKAbrFilter(HashMap<PKAbrFilter, Pair<Long, Long>> abrPrecedence) {
        Set<PKAbrFilter> abrSet = abrPrecedence.keySet();
        PKAbrFilter abrFilter = PKAbrFilter.NONE;

        if (abrSet != null && abrSet.size() == 1) {
            abrFilter = (PKAbrFilter) abrSet.toArray()[0];
        }
        return abrFilter;
    }

    private HashMap<PKAbrFilter, Pair<Long, Long>> checkABRPriority() {
        HashMap<PKAbrFilter, Pair<Long, Long>> abrPriorityMap = new HashMap<>();

        ABRSettings abrSettings = playerSettings.getAbrSettings();
        Long minVideoHeight = abrSettings.getMinVideoHeight();
        Long maxVideoHeight = abrSettings.getMaxVideoHeight();
        Long minVideoWidth = abrSettings.getMinVideoWidth();
        Long maxVideoWidth = abrSettings.getMaxVideoWidth();
        Long minVideoBitrate = abrSettings.getMinVideoBitrate();
        Long maxVideoBitrate = abrSettings.getMaxVideoBitrate();

        if ((maxVideoHeight != Long.MAX_VALUE && minVideoHeight != Long.MIN_VALUE) &&
                (maxVideoWidth != Long.MAX_VALUE && minVideoWidth != Long.MIN_VALUE)) {
            abrPriorityMap.put(PKAbrFilter.PIXEL, new Pair<>(minVideoWidth * minVideoHeight, maxVideoWidth * maxVideoHeight));
        } else if (maxVideoHeight != Long.MAX_VALUE || minVideoHeight != Long.MIN_VALUE) {
            abrPriorityMap.put(PKAbrFilter.HEIGHT, new Pair<>(minVideoHeight, maxVideoHeight));
        } else if (maxVideoWidth != Long.MAX_VALUE || minVideoWidth != Long.MIN_VALUE) {
            abrPriorityMap.put(PKAbrFilter.WIDTH, new Pair<>(minVideoWidth, maxVideoWidth));
        } else if (maxVideoBitrate != Long.MAX_VALUE || minVideoBitrate != Long.MIN_VALUE) {
            abrPriorityMap.put(PKAbrFilter.BITRATE, new Pair<>(minVideoBitrate, maxVideoBitrate));
        } else {
            abrPriorityMap.put(PKAbrFilter.NONE, new Pair<>(Long.MIN_VALUE, Long.MAX_VALUE));
        }

        return abrPriorityMap;
    }

    private void sendInvalidVideoBitrateRangeIfNeeded(String errorMessage) {
        if (eventListener != null) {
            currentError = new PKError(PKPlayerErrorType.UNEXPECTED, PKError.Severity.Recoverable, errorMessage, new IllegalArgumentException(errorMessage));
            eventListener.onEvent(PlayerEvent.Type.ERROR);
        }
    }

    private DeferredDrmSessionManager.DrmSessionListener initDrmSessionListener() {
        return error -> {
            currentError = error;
            sendEvent(PlayerEvent.Type.ERROR);
        };
    }

    @Override
    public BaseTrack getLastSelectedTrack(int renderType) {
        if (assertTrackSelectionIsNotNull("getLastSelectedTrack()")) {
            return trackSelectionHelper.getLastSelectedTrack(renderType);
        }
        return null;
    }

    @Override
    public List<EventStream> getEventStreams() {
        return eventStreams;
    }

    @Override
    public boolean isLive() {
        log.v("isLive");
        if (assertPlayerIsNotNull("isLive()")) {
            return player.isCurrentMediaItemLive();
        }
        return false;
    }

    @Override
    public void setDownloadCache(Cache downloadCache) {
        this.downloadCache = downloadCache;
    }

    @Override
    public ThumbnailInfo getThumbnailInfo(long positionMS) {
        log.v("getThumbnailInfo positionMS = " + positionMS);
        if (assertPlayerIsNotNull("getThumbnailInfo()")) {
            if (isLive()) {
                Timeline timeline = player.getCurrentTimeline();
                if (!timeline.isEmpty()) {
                    positionMS -= timeline.getPeriod(player.getCurrentPeriodIndex(), new Timeline.Period()).getPositionInWindowMs();
                }
            }
            return trackSelectionHelper.getThumbnailInfo(positionMS);
        }
        return null;
    }

    private void closeProfilerSession() {
        profiler.onSessionFinished();
    }

    public void setPlaybackRate(float rate) {
        log.v("setPlaybackRate");
        if (assertPlayerIsNotNull("setPlaybackRate()")) {
            PlaybackParameters  playbackParameters = new PlaybackParameters(rate);
            player.setPlaybackParameters(playbackParameters);
            this.lastKnownPlaybackRate = rate;
        }
    }

    @Override
    public float getPlaybackRate() {
        log.v("getPlaybackRate");
        if (assertPlayerIsNotNull("getPlaybackRate()")) {
            return player.getPlaybackParameters().speed;
        }
        return lastKnownPlaybackRate;
    }

    @Override
    public void onOrientationChanged() {
        //Do nothing.
    }

    private void selectPreferredTracksLanguage(PKTracks tracksReady) {
        if (assertTrackSelectionIsNotNull("selectPreferredTracksLanguage()")) {
            for (int trackType : new int[]{TRACK_TYPE_AUDIO, TRACK_TYPE_TEXT}) {
                String preferredLanguageId = trackSelectionHelper.getPreferredTrackId(trackType);
                if (preferredLanguageId != null) {
                    log.d("preferred language selected for track type = " + trackType + " preferredLanguageId = " + preferredLanguageId);
                    changeTrack(preferredLanguageId);
                    updateDefaultSelectionIndex(tracksReady, trackType, preferredLanguageId);
                }
            }
        }
    }

    private void updateDefaultSelectionIndex(PKTracks tracksReady, int trackType, String preferredLanguageId) {
        if (trackType == TRACK_TYPE_AUDIO) {
            for (int i = 0; i < tracksReady.getAudioTracks().size(); i++) {
                if (tracksReady.getAudioTracks().get(i) != null && preferredLanguageId.equals(tracksReady.getAudioTracks().get(i).getUniqueId())) {
                    tracksReady.defaultAudioTrackIndex = i;
                    break;
                }
            }
        } else if (trackType == TRACK_TYPE_TEXT) {
            for (int i = 0; i < tracksReady.getTextTracks().size(); i++) {
                if (tracksReady.getTextTracks().get(i) != null && preferredLanguageId.equals(tracksReady.getTextTracks().get(i).getUniqueId())) {
                    tracksReady.defaultTextTrackIndex = i;
                    break;
                }
            }
        }
    }

    public void setProfiler(Profiler profiler) {
        if (profiler != null) {
            this.profiler = profiler;
            profiler.setPlayerEngine(this);
        }
    }

    private void configureSubtitleView() {
        SubtitleView exoPlayerSubtitleView;
        SubtitleStyleSettings subtitleStyleSettings = playerSettings.getSubtitleStyleSettings();
        if (exoPlayerView != null) {
            if (subtitleStyleSettings.getSubtitlePosition() != null) {
                exoPlayerView.setSubtitleViewPosition(subtitleStyleSettings.getSubtitlePosition());
            }
            exoPlayerSubtitleView = exoPlayerView.getSubtitleView();
        } else {
            log.e("ExoPlayerView is not available");
            return;
        }

        if (exoPlayerSubtitleView != null ) {
            // Setting `false` will tell ExoPlayer to remove the styling 
            // and the font size of the cue.
            // Separate ExoPlayer API to remove font size is `setApplyEmbeddedFontSizes`.
            // In our API, for FE apps, default is `true` means override the styling
            // Hence reverting the value coming in `subtitleStyleSettings.isOverrideCueStyling()`
            exoPlayerSubtitleView.setApplyEmbeddedStyles(!subtitleStyleSettings.isOverrideCueStyling());

            exoPlayerSubtitleView.setStyle(subtitleStyleSettings.toCaptionStyle());
            exoPlayerSubtitleView.setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * subtitleStyleSettings.getTextSizeFraction());
        } else {
            log.e("Subtitle View is not available");
            return;
        }
        exoPlayerView.applySubtitlesChanges();
    }

    @Override
    public void updateSubtitleStyle(SubtitleStyleSettings subtitleStyleSettings) {
        if (playerSettings.getSubtitleStyleSettings() != null && subtitleStyleSettings != null) {
            playerSettings.setSubtitleStyle(subtitleStyleSettings);
            configureSubtitleView();
            sendEvent(PlayerEvent.Type.SUBTITLE_STYLE_CHANGED);
        }
    }

    @Override
    public void updateABRSettings(ABRSettings abrSettings) {
        playerSettings.setABRSettings(abrSettings);
        HashMap<PKAbrFilter, Pair<Long, Long>> abrPrecedence = checkABRPriority();
        PKAbrFilter abrFilter = getPKAbrFilter(abrPrecedence);
        overrideMediaDefaultABR(abrPrecedence.get(abrFilter).first, abrPrecedence.get(abrFilter).second, abrFilter);
        sendDistinctEvent(PlayerEvent.Type.TRACKS_AVAILABLE);
    }

    @Override
    public void resetABRSettings() {
        playerSettings.setABRSettings(ABRSettings.RESET);
        overrideMediaDefaultABR(Long.MIN_VALUE, Long.MAX_VALUE, PKAbrFilter.NONE);
        sendDistinctEvent(PlayerEvent.Type.TRACKS_AVAILABLE);
    }

    @Nullable
    @Override
    public Object getCurrentMediaManifest() {
        if (assertPlayerIsNotNull("getCurrentMediaManifest")) {
            return player.getCurrentManifest();
        }
        return null;
    }

    @Override
    public void updateSurfaceAspectRatioResizeMode(PKAspectRatioResizeMode resizeMode) {
        if (resizeMode == null) {
            log.e("Resize mode is invalid");
            return;
        }
        playerSettings.setSurfaceAspectRatioResizeMode(resizeMode);
        configureAspectRatioResizeMode(playerSettings.getAspectRatioResizeMode());
        sendEvent(PlayerEvent.Type.ASPECT_RATIO_RESIZE_MODE_CHANGED);
    }

    @Override
    public void updatePKLowLatencyConfig(PKLowLatencyConfig pkLowLatencyConfig) {
        if (!isLiveMediaWithDvr() && !isLiveMediaWithoutDvr()) {
            return;
        }

        pkLowLatencyConfig = validatePKLowLatencyConfig(pkLowLatencyConfig);
        playerSettings.setPKLowLatencyConfig(pkLowLatencyConfig);

        if (assertPlayerIsNotNull("updatePKLowLatencyConfig") && player.getCurrentMediaItem() != null) {
            MediaItem.LiveConfiguration liveConfiguration = getLowLatencyConfigFromPlayerSettings();
            player.setMediaItem(player.getCurrentMediaItem().buildUpon()
                    .setLiveConfiguration(liveConfiguration)
                    .build());
        }
    }

    private PKLowLatencyConfig validatePKLowLatencyConfig(PKLowLatencyConfig pkLowLatencyConfig) {

        PKLowLatencyConfig unsetPKLowLatencyConfig = PKLowLatencyConfig.UNSET;
        if (pkLowLatencyConfig == null) {
            pkLowLatencyConfig = unsetPKLowLatencyConfig;
        } else {
            if (pkLowLatencyConfig.getTargetOffsetMs() <= 0) {
                pkLowLatencyConfig.setTargetOffsetMs(unsetPKLowLatencyConfig.getTargetOffsetMs());
            }
            if (pkLowLatencyConfig.getMinOffsetMs() <= 0) {
                pkLowLatencyConfig.setMinOffsetMs(unsetPKLowLatencyConfig.getMinOffsetMs());
            }
            if (pkLowLatencyConfig.getMaxOffsetMs() <= 0) {
                pkLowLatencyConfig.setMaxOffsetMs(unsetPKLowLatencyConfig.getMaxOffsetMs());
            }
            if (pkLowLatencyConfig.getMinPlaybackSpeed() <= 0) {
                pkLowLatencyConfig.setMinPlaybackSpeed(unsetPKLowLatencyConfig.getMinPlaybackSpeed());
            }
            if (pkLowLatencyConfig.getMaxPlaybackSpeed() <= 0) {
                pkLowLatencyConfig.setMaxPlaybackSpeed(unsetPKLowLatencyConfig.getMaxPlaybackSpeed());
            }
        }
        return pkLowLatencyConfig;
    }

    private void configureAspectRatioResizeMode(PKAspectRatioResizeMode resizeMode) {
        if (exoPlayerView != null) {
            exoPlayerView.setSurfaceAspectRatioResizeMode(resizeMode);
        }
    }

    private boolean assertPlayerIsNotNull(String methodName) {
        if (player != null) {
            return true;
        }
        String nullPlayerMsgFormat = "Attempt to invoke '%s' on null instance of the player engine";
        log.w(String.format(nullPlayerMsgFormat, methodName));
        return false;
    }

    private boolean assertTrackSelectionIsNotNull(String methodName) {
        if (trackSelectionHelper != null) {
            return true;
        }
        String nullTrackSelectionMsgFormat = "Attempt to invoke '%s' on null instance of trackSelectionHelper";
        log.w(String.format(nullTrackSelectionMsgFormat, methodName));
        return false;
    }
}
