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

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.kaltura.android.exoplayer2.C;
import com.kaltura.android.exoplayer2.DefaultLoadControl;
import com.kaltura.android.exoplayer2.ExoPlaybackException;
import com.kaltura.android.exoplayer2.ExoPlayerFactory;
import com.kaltura.android.exoplayer2.ExoPlayerLibraryInfo;
import com.kaltura.android.exoplayer2.Format;
import com.kaltura.android.exoplayer2.LoadControl;
import com.kaltura.android.exoplayer2.PlaybackParameters;
import com.kaltura.android.exoplayer2.Player;
import com.kaltura.android.exoplayer2.SimpleExoPlayer;
import com.kaltura.android.exoplayer2.Timeline;
import com.kaltura.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory;
import com.kaltura.android.exoplayer2.metadata.Metadata;
import com.kaltura.android.exoplayer2.metadata.MetadataOutput;
import com.kaltura.android.exoplayer2.source.BehindLiveWindowException;
import com.kaltura.android.exoplayer2.source.MediaSource;
import com.kaltura.android.exoplayer2.source.MergingMediaSource;
import com.kaltura.android.exoplayer2.source.ProgressiveMediaSource;
import com.kaltura.android.exoplayer2.source.SingleSampleMediaSource;
import com.kaltura.android.exoplayer2.source.TrackGroupArray;
import com.kaltura.android.exoplayer2.source.dash.DashMediaSource;
import com.kaltura.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.kaltura.android.exoplayer2.source.hls.HlsMediaSource;
import com.kaltura.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.kaltura.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.kaltura.android.exoplayer2.trackselection.TrackSelectionArray;
import com.kaltura.android.exoplayer2.ui.SubtitleView;
import com.kaltura.android.exoplayer2.upstream.BandwidthMeter;
import com.kaltura.android.exoplayer2.upstream.DataSource;
import com.kaltura.android.exoplayer2.upstream.DefaultAllocator;
import com.kaltura.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.kaltura.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.kaltura.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.kaltura.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.kaltura.android.exoplayer2.upstream.HttpDataSource;
import com.kaltura.android.exoplayer2.video.CustomLoadControl;
import com.kaltura.playkit.*;
import com.kaltura.playkit.drm.DeferredDrmSessionManager;
import com.kaltura.playkit.drm.DrmCallback;
import com.kaltura.playkit.player.metadata.MetadataConverter;
import com.kaltura.playkit.player.metadata.PKMetadata;
import com.kaltura.playkit.utils.Consts;
import com.kaltura.playkit.utils.NativeCookieJarBridge;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

import static com.kaltura.playkit.utils.Consts.DEFAULT_PITCH_RATE;
import static com.kaltura.playkit.utils.Consts.TIME_UNSET;
import static com.kaltura.playkit.utils.Consts.TRACK_TYPE_AUDIO;
import static com.kaltura.playkit.utils.Consts.TRACK_TYPE_TEXT;


public class ExoPlayerWrapper implements PlayerEngine, Player.EventListener, MetadataOutput, BandwidthMeter.EventListener {
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
    private SimpleExoPlayer player;
    private BaseExoplayerView exoPlayerView;
    private PlayerView rootView;
    private boolean rootViewUpdated;

    private PKTracks tracks;
    private Timeline.Window window;
    private TrackSelectionHelper trackSelectionHelper;
    private DeferredDrmSessionManager drmSessionManager;

    private PlayerEvent.Type currentEvent;
    private PlayerState currentState = PlayerState.IDLE;
    private PlayerState previousState;

    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private PKError currentError = null;

    private boolean isSeeking;
    private boolean useTextureView;
    private boolean isSurfaceSecured;
    private boolean shouldGetTracksInfo;
    private boolean shouldResetPlayerPosition;
    private boolean preferredLanguageWasSelected;
    private boolean shouldRestorePlayerToPreviousState;
    private boolean isPlayerReleased;

    private int playerWindow;
    private long playerPosition = TIME_UNSET;

    private float lastKnownVolume = Consts.DEFAULT_VOLUME;
    private float lastKnownPlaybackRate = Consts.DEFAULT_PLAYBACK_RATE_SPEED;

    private List<PKMetadata> metadataList = new ArrayList<>();
    private String[] lastSelectedTrackIds = {TrackSelectionHelper.NONE, TrackSelectionHelper.NONE, TrackSelectionHelper.NONE};

    private TrackSelectionHelper.TracksInfoListener tracksInfoListener = initTracksInfoListener();
    private TrackSelectionHelper.TracksErrorListener tracksErrorListener = initTracksErrorListener();
    private DeferredDrmSessionManager.DrmSessionListener drmSessionListener = initDrmSessionListener();

    private PKMediaSourceConfig sourceConfig;
    @NonNull private Profiler profiler = Profiler.NOOP;

    private Timeline.Period period;

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
        if (loadControlStrategyObj != null && loadControlStrategyObj instanceof LoadControlStrategy) {
            return ((LoadControlStrategy) loadControlStrategyObj);
        } else {
            return null;
        }
    }

    @Override
    public void onBandwidthSample(int elapsedMs, long bytes, long bitrate) {
        sendEvent(PlayerEvent.Type.PLAYBACK_INFO_UPDATED);
    }

    private void initializePlayer() {
        DefaultTrackSelector trackSelector = initializeTrackSelector();

        final DrmCallback drmCallback = new DrmCallback(getHttpDataSourceFactory(null), playerSettings.getLicenseRequestAdapter());
        drmSessionManager = new DeferredDrmSessionManager(mainHandler, drmCallback, drmSessionListener);
        CustomRendererFactory renderersFactory = new CustomRendererFactory(context, playerSettings.allowClearLead(), playerSettings.enableDecoderFallback(), playerSettings.getLoadControlBuffers().getAllowedVideoJoiningTimeMs());

        player = ExoPlayerFactory.newSimpleInstance(context, renderersFactory, trackSelector, getUpdatedLoadControl(), drmSessionManager, bandwidthMeter);

        window = new Timeline.Window();
        setPlayerListeners();
        exoPlayerView.setSurfaceAspectRatioResizeMode(playerSettings.getAspectRatioResizeMode());
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
            return new CustomLoadControl(new DefaultAllocator(/* trimOnReset= */ true, C.DEFAULT_BUFFER_SEGMENT_SIZE),
                    loadControl.getMinPlayerBufferMs(),
                    loadControl.getMaxPlayerBufferMs(), // minBufferVideoMs is set same as the maxBufferMs due to issue in exo player FEM-2707
                    loadControl.getMaxPlayerBufferMs(),
                    loadControl.getMinBufferAfterInteractionMs(),
                    loadControl.getMinBufferAfterReBufferMs(),
                    DefaultLoadControl.DEFAULT_TARGET_BUFFER_BYTES,
                    DefaultLoadControl.DEFAULT_PRIORITIZE_TIME_OVER_SIZE_THRESHOLDS,
                    loadControl.getBackBufferDurationMs(),
                    loadControl.getRetainBackBufferFromKeyframe());
        }
    }

    private void setPlayerListeners() {
        log.v("setPlayerListeners");
        if (assertPlayerIsNotNull("setPlayerListeners()")) {
            player.addListener(this);
            player.addMetadataOutput(this);
            player.addAnalyticsListener(analyticsAggregator);
            final com.kaltura.android.exoplayer2.analytics.AnalyticsListener exoAnalyticsListener = profiler.getExoAnalyticsListener();
            if (exoAnalyticsListener != null) {
                player.addAnalyticsListener(exoAnalyticsListener);
            }
        }
    }

    private DefaultTrackSelector initializeTrackSelector() {

        DefaultTrackSelector trackSelector = new DefaultTrackSelector(new AdaptiveTrackSelection.Factory());
        DefaultTrackSelector.ParametersBuilder parametersBuilder = new DefaultTrackSelector.ParametersBuilder();
        parametersBuilder.setViewportSizeToPhysicalDisplaySize(context, true);
        if (playerSettings.isTunneledAudioPlayback() && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            parametersBuilder.setTunnelingAudioSessionId(C.generateAudioSessionIdV21(context));
        }
        trackSelector.setParameters(parametersBuilder.build());

        trackSelectionHelper = new TrackSelectionHelper(trackSelector, lastSelectedTrackIds);
        trackSelectionHelper.setTracksInfoListener(tracksInfoListener);
        trackSelectionHelper.setTracksErrorListener(tracksErrorListener);

        return trackSelector;
    }

    private void preparePlayer(@NonNull PKMediaSourceConfig sourceConfig) {
        this.sourceConfig = sourceConfig;
        //reset metadata on prepare.
        metadataList.clear();

        if (sourceConfig.mediaSource.hasDrmParams()) {
            drmSessionManager.setMediaSource(sourceConfig.mediaSource);
        }

        shouldGetTracksInfo = true;
        trackSelectionHelper.applyPlayerSettings(playerSettings);

        MediaSource mediaSource = buildExoMediaSource(sourceConfig);
        profiler.onPrepareStarted(sourceConfig);
        boolean haveStartPosition = player.getCurrentWindowIndex() != C.INDEX_UNSET;
        player.prepare(mediaSource, !haveStartPosition, shouldResetPlayerPosition);

        changeState(PlayerState.LOADING);

        if (playerSettings.getSubtitleStyleSettings() != null) {
            configureSubtitleView();
        }
    }

    private MediaSource buildExoMediaSource(PKMediaSourceConfig sourceConfig) {
        List<PKExternalSubtitle> externalSubtitleList = null;

        if (sourceConfig.getExternalSubtitleList() != null) {
            externalSubtitleList = sourceConfig.getExternalSubtitleList().size() > 0 ?
                    sourceConfig.getExternalSubtitleList() : null;
        }

        final MediaSource mediaSource;

        if (sourceConfig.mediaSource instanceof LocalAssetsManagerExo.LocalExoMediaSource) {
            final LocalAssetsManagerExo.LocalExoMediaSource pkMediaSource = (LocalAssetsManagerExo.LocalExoMediaSource) sourceConfig.mediaSource;
            mediaSource = pkMediaSource.getExoMediaSource();

        } else {
            mediaSource = buildInternalExoMediaSource(sourceConfig);
        }

        if (externalSubtitleList == null || externalSubtitleList.isEmpty()) {
            return mediaSource;
        } else {
            return new MergingMediaSource(buildMediaSourceList(mediaSource, externalSubtitleList));
        }
    }

    private MediaSource buildInternalExoMediaSource(PKMediaSourceConfig sourceConfig) {
        MediaSource mediaSource;
        PKMediaFormat format = sourceConfig.mediaSource.getMediaFormat();

        if (format == null) {
            // TODO: error?
            return null;
        }

        PKRequestParams requestParams = sourceConfig.getRequestParams();
        Uri uri = requestParams.url;

        final DataSource.Factory dataSourceFactory = getDataSourceFactory(requestParams.headers);

        switch (format) {
            case dash:
                mediaSource = new DashMediaSource.Factory(
                        new DefaultDashChunkSource.Factory(dataSourceFactory), dataSourceFactory).createMediaSource(uri);
                break;

            case hls:
                mediaSource = new HlsMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(uri);
                break;

            // mp4 and mp3 both use ExtractorMediaSource
            case mp4:
            case mp3:
                mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(uri);
                break;

            default:
                throw new IllegalStateException("Unsupported type: " + format);
        }
        return mediaSource;
    }

    /**
     * Return the media source with external subtitles if exists
     * @param externalSubtitleList External subtitle List
     * @return Media Source array
     */

    private MediaSource[] buildMediaSourceList(MediaSource mediaSource, List<PKExternalSubtitle> externalSubtitleList) {
        List<MediaSource> streamMediaSources = new ArrayList<>();

        if (externalSubtitleList != null && externalSubtitleList.size() > 0) {
            for (int subtitlePosition = 0 ; subtitlePosition < externalSubtitleList.size() ; subtitlePosition ++) {
                MediaSource subtitleMediaSource = buildExternalSubtitleSource(subtitlePosition, externalSubtitleList.get(subtitlePosition));
                streamMediaSources.add(subtitleMediaSource);
            }
        }

        // 0th position is secured for dash/hls/extractor media source
        streamMediaSources.add(0, mediaSource);
        return streamMediaSources.toArray(new MediaSource[0]);
    }

    /**
     * Create single Media Source object with each subtitle
     * @param pkExternalSubtitle External subtitle object
     * @return An object of external subtitle media source
     */

    @NonNull
    private MediaSource buildExternalSubtitleSource(int subtitleId, PKExternalSubtitle pkExternalSubtitle) {
        // Build the subtitle MediaSource.
        Format subtitleFormat = Format.createTextContainerFormat(
                String.valueOf(subtitleId), // An identifier for the track. May be null.
                pkExternalSubtitle.getLabel(),
                pkExternalSubtitle.getContainerMimeType(),
                pkExternalSubtitle.getMimeType(), // The mime type. Must be set correctly.
                pkExternalSubtitle.getCodecs(),
                pkExternalSubtitle.getBitrate(),
                pkExternalSubtitle.getSelectionFlags(),
                pkExternalSubtitle.getRoleFlag(),
                pkExternalSubtitle.getLanguage()); // The subtitle language. May be null.

        return new SingleSampleMediaSource.Factory(getDataSourceFactory(null))
                .createMediaSource(Uri.parse(pkExternalSubtitle.getUrl()), subtitleFormat, C.TIME_UNSET);
    }

    private HttpDataSource.Factory getHttpDataSourceFactory(Map<String, String> headers) {
        HttpDataSource.Factory httpDataSourceFactory;
        final String userAgent = getUserAgent(context);
        final boolean crossProtocolRedirectEnabled = playerSettings.crossProtocolRedirectEnabled();

        if (CookieHandler.getDefault() == null) {
            CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER));
        }

        if (!PKHttpClientManager.useSystem()) {

            final OkHttpClient.Builder builder = PKHttpClientManager.newClientBuilder()
                    .cookieJar(NativeCookieJarBridge.sharedCookieJar)
                    .followRedirects(true)
                    .followSslRedirects(crossProtocolRedirectEnabled)
                    .connectTimeout(DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                    .readTimeout(DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            builder.eventListener(analyticsAggregator);
            if (profiler != Profiler.NOOP) {
                final okhttp3.EventListener.Factory okListenerFactory = profiler.getOkListenerFactory();
                if (okListenerFactory != null) {
                    builder.eventListenerFactory(okListenerFactory);
                }
            }

            httpDataSourceFactory = new OkHttpDataSourceFactory(builder.build(), userAgent);

        } else {

            httpDataSourceFactory = new DefaultHttpDataSourceFactory(userAgent,
                    DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                    DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
                    crossProtocolRedirectEnabled);
        }

        if (headers != null) {
            HttpDataSource.RequestProperties defaultRequestProperties = httpDataSourceFactory.getDefaultRequestProperties();
            for (Map.Entry<String, String> headerEntry : headers.entrySet()) {
                defaultRequestProperties.set(headerEntry.getKey(), headerEntry.getValue());
            }
        }
        return httpDataSourceFactory;
    }

    private DataSource.Factory getDataSourceFactory(Map<String, String> headers) {
        return new DefaultDataSourceFactory(context, getHttpDataSourceFactory(headers));
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
    public void onLoadingChanged(boolean isLoading) {
        log.d("onLoadingChanged. isLoading => " + isLoading);
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
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        switch (playbackState) {
            case Player.STATE_IDLE:
                log.d("onPlayerStateChanged. IDLE. playWhenReady => " + playWhenReady);
                changeState(PlayerState.IDLE);
                if (isSeeking) {
                    isSeeking = false;
                }
                break;

            case Player.STATE_BUFFERING:
                log.d("onPlayerStateChanged. BUFFERING. playWhenReady => " + playWhenReady);
                changeState(PlayerState.BUFFERING);
                break;

            case Player.STATE_READY:
                log.d("onPlayerStateChanged. READY. playWhenReady => " + playWhenReady);
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
                log.d("onPlayerStateChanged. ENDED. playWhenReady => " + playWhenReady);
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
    public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {
        log.d("onTimelineChanged reason = " + reason + " duration = " + getDuration());
        if (reason == Player.TIMELINE_CHANGE_REASON_PREPARED) {
            sendDistinctEvent(PlayerEvent.Type.LOADED_METADATA);
            if (getDuration() != TIME_UNSET) {
                sendDistinctEvent(PlayerEvent.Type.DURATION_CHANGE);
                profiler.onDurationChanged(getDuration());
            }
        }

        if (reason == Player.TIMELINE_CHANGE_REASON_DYNAMIC && getDuration() != TIME_UNSET) {
            sendDistinctEvent(PlayerEvent.Type.DURATION_CHANGE);
        }
        shouldResetPlayerPosition = (reason == Player.TIMELINE_CHANGE_REASON_DYNAMIC);
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        log.d("onPlayerError error type => " + error.type);
        if (isBehindLiveWindow(error) && sourceConfig != null) {
            log.d("onPlayerError BehindLiveWindowException received, re-preparing player");
            player.prepare(buildExoMediaSource(sourceConfig), true, false);
            return;
        }

        Enum errorType;
        String errorMessage = error.getMessage();

        switch (error.type) {
            case ExoPlaybackException.TYPE_SOURCE:
                errorType = PKPlayerErrorType.SOURCE_ERROR;
                break;
            case ExoPlaybackException.TYPE_RENDERER:
                errorType = PKPlayerErrorType.RENDERER_ERROR;
                break;
            case ExoPlaybackException.TYPE_OUT_OF_MEMORY:
                errorType = PKPlayerErrorType.OUT_OF_MEMORY;
                break;
            case ExoPlaybackException.TYPE_REMOTE:
                errorType = PKPlayerErrorType.REMOTE_COMPONENT_ERROR;
                break;
            case ExoPlaybackException.TYPE_UNEXPECTED:
            default:
                errorType = PKPlayerErrorType.UNEXPECTED;
                break;
        }

        String errorStr = (errorMessage == null) ? "Player error: " + errorType.name() : errorMessage;
        log.e(errorStr);
        currentError = new PKError(errorType, errorStr, error);
        if (eventListener != null) {
            log.e("Error-Event sent, type = " + error.type);
            eventListener.onEvent(PlayerEvent.Type.ERROR);
        } else {
            log.e("eventListener is null cannot send Error-Event type = " + error.type);
        }
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
        sendEvent(PlayerEvent.Type.PLAYBACK_RATE_CHANGED);
    }

    @Override
    public void onPositionDiscontinuity(int reason) {
        log.d("onPositionDiscontinuity");
    }

    @Override
    public void onSeekProcessed() {
        // TODO: use this instead of STATE_READY after isSeeking.
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

        log.d("onTracksChanged");
        //if onOnTracksChanged happened when application went background, do not update the tracks.
        if (trackSelectionHelper == null) {
            return;
        }
        //if the track info new -> map the available tracks. and when ready, notify user about available tracks.
        if (shouldGetTracksInfo) {
            shouldGetTracksInfo = !trackSelectionHelper.prepareTracks(trackSelections);
        }

        trackSelectionHelper.notifyAboutTrackChange(trackSelections);
    }

    @Override
    public void onMetadata(Metadata metadata) {
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

    private boolean isBehindLiveWindow(ExoPlaybackException e) {
        if (e.type != ExoPlaybackException.TYPE_SOURCE) {
            return false;
        }
        Throwable cause = e.getSourceException();
        while (cause != null) {
            if (cause instanceof BehindLiveWindowException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
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

            profiler.onPlayRequested();
            player.setPlayWhenReady(true);
        }
    }

    @Override
    public void pause() {
        log.v("pause");
        if (assertPlayerIsNotNull("pause()")) {
            //If player already set to pause, return.
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
        final int currentWindowIndex = player.getCurrentWindowIndex();
        if (currentWindowIndex == C.INDEX_UNSET) {
            return TIME_UNSET;
        }
        final Timeline.Window window = player.getCurrentTimeline().getWindow(currentWindowIndex, new Timeline.Window());
        if (window == null) {
            return TIME_UNSET;
        }

        return window.presentationStartTimeMs;
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
    public void release() {
        log.v("release");
        if (assertPlayerIsNotNull("release()")) {
            savePlayerPosition();
            player.release();
            player = null;
            trackSelectionHelper.release();
            trackSelectionHelper = null;
        }
        isPlayerReleased = true;
        shouldRestorePlayerToPreviousState = true;
    }

    @Override
    public void restore() {
        log.v("restore");
        if (player == null) {
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

    @Override
    public void destroy() {
        log.v("destroy");
        closeProfilerSession();
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
        if (trackSelectionHelper == null) {
            log.w("Attempt to invoke 'changeTrack()' on null instance of the TracksSelectionHelper");
            return;
        }

        try {
            trackSelectionHelper.changeTrack(uniqueId);
        } catch (IllegalArgumentException ex) {
            sendTrackSelectionError(uniqueId, ex);
        }
    }

    @Override
    public void overrideMediaDefaultABR(long minVideoBitrate, long maxVideoBitrate) {
        if (trackSelectionHelper == null) {
            log.w("Attempt to invoke 'overrideMediaDefaultABR()' on null instance of the TracksSelectionHelper");
            return;
        }

        if (minVideoBitrate > maxVideoBitrate || maxVideoBitrate <= 0) {
            minVideoBitrate = Long.MIN_VALUE;
            maxVideoBitrate = Long.MAX_VALUE;
            String errorMessage = "given maxVideoBitrate is not greater than the minVideoBitrate";
            sendInvalidVideoBitrateRangeIfNeeded(errorMessage);
        }
        trackSelectionHelper.overrideMediaDefaultABR(minVideoBitrate, maxVideoBitrate);
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

        shouldResetPlayerPosition = true;
        preferredLanguageWasSelected = false;
        lastKnownVolume = Consts.DEFAULT_VOLUME;
        lastKnownPlaybackRate = Consts.DEFAULT_PLAYBACK_RATE_SPEED;
        lastSelectedTrackIds = new String[]{TrackSelectionHelper.NONE, TrackSelectionHelper.NONE, TrackSelectionHelper.NONE};
        if (trackSelectionHelper != null) {
            trackSelectionHelper.stop();
        }

        playerPosition = TIME_UNSET;

        if (assertPlayerIsNotNull("stop()")) {
            player.setPlayWhenReady(false);
            player.stop(true);
        }

        analyticsAggregator.reset();

        closeProfilerSession();
    }

    private void savePlayerPosition() {
        log.v("savePlayerPosition");
        if (assertPlayerIsNotNull("savePlayerPosition()")) {
            currentError = null;
            playerWindow = player.getCurrentWindowIndex();
            Timeline timeline = player.getCurrentTimeline();
            if (timeline != null && !timeline.isEmpty() && timeline.getWindow(playerWindow, window).isSeekable) {
                playerPosition = player.getCurrentPosition();
            }
        }
    }

    public List<PKMetadata> getMetadata() {
        return metadataList;
    }

    private TrackSelectionHelper.TracksErrorListener initTracksErrorListener() {
        return pkError -> {
            currentError = pkError;
            if (eventListener != null) {
                eventListener.onEvent(PlayerEvent.Type.ERROR);
            }
        };
    }

    private TrackSelectionHelper.TracksInfoListener initTracksInfoListener() {
        return new TrackSelectionHelper.TracksInfoListener() {
            @Override
            public void onTracksInfoReady(PKTracks tracksReady) {
                if (playerSettings.getAbrSettings().getMinVideoBitrate() != Long.MIN_VALUE || playerSettings.getAbrSettings().getMaxVideoBitrate() != Long.MAX_VALUE) {
                    overrideMediaDefaultABR(playerSettings.getAbrSettings().getMinVideoBitrate(), playerSettings.getAbrSettings().getMaxVideoBitrate());
                }
                //when the track info is ready, cache it in ExoPlayerWrapper. And send event that tracks are available.
                tracks = tracksReady;
                shouldRestorePlayerToPreviousState = false;
                sendDistinctEvent(PlayerEvent.Type.TRACKS_AVAILABLE);
                if (!preferredLanguageWasSelected) {
                    selectPreferredTracksLanguage();
                    preferredLanguageWasSelected = true;
                }

                if (tracksReady.getVideoTracks().size() == 0) {
                    exoPlayerView.hideVideoSurface();
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
        };
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
        return trackSelectionHelper.getLastSelectedTrack(renderType);
    }

    @Override
    public boolean isLive() {
        log.v("isLive");
        if (assertPlayerIsNotNull("isLive()")) {
            return player.isCurrentWindowDynamic();
        }
        return false;
    }

    private void closeProfilerSession() {
        profiler.onSessionFinished();
    }

    public void setPlaybackRate(float rate) {
        log.v("setPlaybackRate");
        if (assertPlayerIsNotNull("setPlaybackRate()")) {
            PlaybackParameters playbackParameters = new PlaybackParameters(rate, DEFAULT_PITCH_RATE);
            player.setPlaybackParameters(playbackParameters);
            this.lastKnownPlaybackRate = rate;
        }
    }

    @Override
    public float getPlaybackRate() {
        log.v("getPlaybackRate");
        if (assertPlayerIsNotNull("getPlaybackRate()") && player.getPlaybackParameters() != null) {
            return player.getPlaybackParameters().speed;
        }
        return lastKnownPlaybackRate;
    }

    @Override
    public void onOrientationChanged() {
        //Do nothing.
    }

    private void selectPreferredTracksLanguage() {

        for (int trackType : new int[]{TRACK_TYPE_AUDIO, TRACK_TYPE_TEXT}) {
            String preferredLanguageId = trackSelectionHelper.getPreferredTrackId(trackType);
            if (preferredLanguageId != null) {
                changeTrack(preferredLanguageId);
                log.d("preferred language selected for track type = " + trackType);
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
        SubtitleView exoPlayerSubtitleView = null;
        if(exoPlayerView != null) {
            exoPlayerSubtitleView = exoPlayerView.getSubtitleView();
        } else {
            log.e("ExoPlayerView is not available");
        }

        if (exoPlayerSubtitleView != null) {
            exoPlayerSubtitleView.setStyle(playerSettings.getSubtitleStyleSettings().toCaptionStyle());
            exoPlayerSubtitleView.setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * playerSettings.getSubtitleStyleSettings().getTextSizeFraction());
        } else {
            log.e("Subtitle View is not available");
        }
    }

    @Override
    public void updateSubtitleStyle(SubtitleStyleSettings subtitleStyleSettings) {
        if (playerSettings.getSubtitleStyleSettings() != null) {
            playerSettings.setSubtitleStyle(subtitleStyleSettings);
            configureSubtitleView();
            sendEvent(PlayerEvent.Type.SUBTITLE_STYLE_CHANGED);
        }
    }

    @Override
    public void updateSurfaceAspectRatioResizeMode(PKAspectRatioResizeMode resizeMode) {
        playerSettings.setSurfaceAspectRatioResizeMode(resizeMode);
        configureAspectRatioResizeMode();
        sendEvent(PlayerEvent.Type.ASPECT_RATIO_RESIZE_MODE_CHANGED);
    }

    private void configureAspectRatioResizeMode() {
        if(exoPlayerView != null){
            exoPlayerView.setSurfaceAspectRatioResizeMode(playerSettings.getAspectRatioResizeMode());
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
}
