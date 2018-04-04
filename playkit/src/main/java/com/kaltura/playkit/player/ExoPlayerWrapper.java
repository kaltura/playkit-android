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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.ExoPlayerLibraryInfo;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataOutput;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSource.Factory;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.kaltura.playkit.PKError;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.PlaybackInfo;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.drm.DeferredDrmSessionManager;
import com.kaltura.playkit.player.metadata.MetadataConverter;
import com.kaltura.playkit.player.metadata.PKMetadata;
import com.kaltura.playkit.utils.Consts;
import com.kaltura.playkit.utils.EventLogger;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by anton.afanasiev on 31/10/2016.
 */
class ExoPlayerWrapper implements PlayerEngine, Player.EventListener, MetadataOutput, BandwidthMeter.EventListener {

    private static final PKLog log = PKLog.get("ExoPlayerWrapper");

    private static final CookieManager DEFAULT_COOKIE_MANAGER;

    static {
        DEFAULT_COOKIE_MANAGER = new CookieManager();
        DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    }

    private DefaultBandwidthMeter bandwidthMeter;

    private EventLogger eventLogger;
    private EventListener eventListener;
    private StateChangedListener stateChangedListener;

    private Context context;
    private SimpleExoPlayer player;
    private ExoPlayerView exoPlayerView;

    private PKTracks tracks;
    private TrackSelectionHelper trackSelectionHelper;
    private DeferredDrmSessionManager drmSessionManager;

    private PlayerEvent.Type currentEvent;
    private PlayerState currentState = PlayerState.IDLE;
    private PlayerState previousState;

    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private PKError currentError = null;

    private boolean isSeeking = false;
    private boolean useTextureView = false;
    private boolean crossProtocolRedirectEnabled = false;
    private boolean shouldRestorePlayerToPreviousState = false;

    private int playerWindow;
    private long playerPosition = Consts.TIME_UNSET;
    private Timeline.Window window;
    private boolean shouldGetTracksInfo;
    private boolean shouldResetPlayerPosition;
    private List<PKMetadata> metadataList = new ArrayList<>();


    private String[] lastSelectedTrackIds = {TrackSelectionHelper.NONE, TrackSelectionHelper.NONE, TrackSelectionHelper.NONE};

    private TrackSelectionHelper.TracksInfoListener tracksInfoListener = initTracksInfoListener();
    private DeferredDrmSessionManager.DrmSessionListener drmSessionListener = initDrmSessionListener();


    @Override
    public void onBandwidthSample(int elapsedMs, long bytes, long bitrate) {
        sendEvent(PlayerEvent.Type.PLAYBACK_INFO_UPDATED);
    }


    ExoPlayerWrapper(Context context) {
        this.context = context;
        bandwidthMeter = new DefaultBandwidthMeter(mainHandler, this);
        exoPlayerView = new ExoPlayerView(context);
        if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER) {
            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER);
        }
    }

    private void initializePlayer() {
        eventLogger = new EventLogger();

        DefaultTrackSelector trackSelector = initializeTrackSelector();
        drmSessionManager = new DeferredDrmSessionManager(mainHandler, buildHttpDataSourceFactory(false), drmSessionListener);
        CustomRendererFactory rendererFactory = new CustomRendererFactory(context,
                drmSessionManager, DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF);
        player = ExoPlayerFactory.newSimpleInstance(rendererFactory, trackSelector);
        window = new Timeline.Window();
        setPlayerListeners();
        exoPlayerView.setPlayer(player);
        player.setPlayWhenReady(false);
    }

    private void setPlayerListeners() {
        if (player != null) {
            player.addListener(this);
            player.addListener(eventLogger);
            player.setVideoDebugListener(eventLogger);
            player.setAudioDebugListener(eventLogger);
            player.addMetadataOutput(this);
        }
    }

    private DefaultTrackSelector initializeTrackSelector() {

        TrackSelection.Factory trackSelectionFactory =
                new AdaptiveTrackSelection.Factory(bandwidthMeter);
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(trackSelectionFactory);
        trackSelectionHelper = new TrackSelectionHelper(trackSelector, trackSelectionFactory, lastSelectedTrackIds);
        DefaultTrackSelector.ParametersBuilder parametersBuilder = new DefaultTrackSelector.ParametersBuilder();
        parametersBuilder.setViewportSizeToPhysicalDisplaySize(context, true);
        trackSelector.setParameters(parametersBuilder.build());
        trackSelectionHelper.setTracksInfoListener(tracksInfoListener);

        return trackSelector;
    }

    private void preparePlayer(PKMediaSourceConfig sourceConfig) {
        //reset metadata on prepare.
        metadataList.clear();

        if (sourceConfig.mediaSource.hasDrmParams()) {
            drmSessionManager.setMediaSource(sourceConfig.mediaSource);
        }

        shouldGetTracksInfo = true;
        trackSelectionHelper.setCea608CaptionsEnabled(sourceConfig.cea608CaptionsEnabled);

        MediaSource mediaSource = buildExoMediaSource(sourceConfig);
        player.prepare(mediaSource, shouldResetPlayerPosition, shouldResetPlayerPosition);
        changeState(PlayerState.LOADING);
    }

    private MediaSource buildExoMediaSource(PKMediaSourceConfig sourceConfig) {
        PKMediaFormat format = sourceConfig.mediaSource.getMediaFormat();
        if (format == null) {
            // TODO: error?
            return null;
        }

        Uri uri = sourceConfig.getUrl();

        Factory mediaDataSourceFactory = buildDataSourceFactory(true);
        switch (format) {
            // mp4 and mp3 both use ExtractorMediaSource
            case mp4:
            case mp3:
                return new ExtractorMediaSource(uri, mediaDataSourceFactory, new DefaultExtractorsFactory(),
                        mainHandler, eventLogger);

            case dash:
                return new DashMediaSource(uri, buildDataSourceFactory(false),
                        new DefaultDashChunkSource.Factory(mediaDataSourceFactory), mainHandler, eventLogger);

            case hls:
                return new HlsMediaSource(uri, mediaDataSourceFactory, mainHandler, eventLogger);

            default:
                throw new IllegalStateException("Unsupported type: " + format);
        }
    }

    /**
     * Returns a new DataSource factory.
     *
     * @param useBandwidthMeter Whether to set {@link #bandwidthMeter} as a listener to the new
     *                          DataSource factory.
     * @return A new DataSource factory.
     */
    private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
        return new DefaultDataSourceFactory(context, useBandwidthMeter ? bandwidthMeter : null,
                buildHttpDataSourceFactory(useBandwidthMeter));
    }

    /**
     * Returns a new HttpDataSource factory.
     *
     * @param useBandwidthMeter Whether to set {@link #bandwidthMeter} as a listener to the new
     *                          DataSource factory.
     * @return A new HttpDataSource factory.
     */
    private HttpDataSource.Factory buildHttpDataSourceFactory(boolean useBandwidthMeter) {
        return new DefaultHttpDataSourceFactory(getUserAgent(context), useBandwidthMeter ? bandwidthMeter : null, DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS, crossProtocolRedirectEnabled);
    }

    private static String getUserAgent(Context context) {
        String applicationName;
        try {
            String packageName = context.getPackageName();
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            applicationName = packageName + "/" + info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            applicationName = "?";
        }

        return PlayKitManager.CLIENT_TAG + " " + applicationName + " (Linux;Android " + Build.VERSION.RELEASE
                + ") " + "ExoPlayerLib/" + ExoPlayerLibraryInfo.VERSION;
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
        if (shouldRestorePlayerToPreviousState) {
            log.i("Trying to send event " + event.name() + ". Should be blocked from sending now, because the player is restoring to the previous state.");
            return;
        }
        currentEvent = event;
        if (eventListener != null) {
            log.i("Event sent: " + event.name());
            eventListener.onEvent(currentEvent);
        }
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
        log.d("onLoadingChanged. isLoading => " + isLoading);
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

                if (playWhenReady) {
                    sendDistinctEvent(PlayerEvent.Type.PLAYING);
                }

                break;
            case Player.STATE_ENDED:
                log.d("onPlayerStateChanged. ENDED. playWhenReady => " + playWhenReady);
                changeState(PlayerState.IDLE);
                sendDistinctEvent(PlayerEvent.Type.ENDED);
                break;
            default:
                break;

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
    public void onTimelineChanged(Timeline timeline, Object manifest,  int reason) {
        log.d("onTimelineChanged");
        sendDistinctEvent(PlayerEvent.Type.LOADED_METADATA);
        sendDistinctEvent(PlayerEvent.Type.DURATION_CHANGE);
        shouldResetPlayerPosition = (reason == Player.TIMELINE_CHANGE_REASON_DYNAMIC);
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        log.d("onPlayerError error type => " + error.type);
        Enum errorType;
        String errorMessage = error.getMessage();

        switch (error.type) {
            case ExoPlaybackException.TYPE_SOURCE:
                errorType = PKPlayerErrorType.SOURCE_ERROR;
                break;
            case ExoPlaybackException.TYPE_RENDERER:
                errorType = PKPlayerErrorType.RENDERER_ERROR;
                break;
            default:
                errorType = PKPlayerErrorType.UNEXPECTED;
                break;
        }

        String errorStr = (errorMessage == null) ? "Player error: " + errorType.name() : errorMessage;
        log.e(errorStr);
        currentError = new PKError(errorType, errorStr, error);
        eventListener.onEvent(PlayerEvent.Type.ERROR);
    }

    @Override
    public void onPositionDiscontinuity(int reason) {
        log.d("onPositionDiscontinuity");
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
        // TODO: if/when we start using ExoPlayer's speed and pitch settings, listen to this event.
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
            shouldGetTracksInfo = !trackSelectionHelper.prepareTracks();
        }

        trackSelectionHelper.updateSelectedTracksBitrate(trackSelections);
    }

    @Override
    public void onMetadata(Metadata metadata) {

        this.metadataList = MetadataConverter.convert(metadata);

        sendEvent(PlayerEvent.Type.METADATA_AVAILABLE);
    }

    @Override
    public void load(PKMediaSourceConfig mediaSourceConfig) {
        log.d("load");
        crossProtocolRedirectEnabled = mediaSourceConfig.crossProtocolRedirectEnabled;
        if (player == null) {
            initializePlayer();
        }

        maybeChangePlayerRenderView(mediaSourceConfig.useTextureView);
        preparePlayer(mediaSourceConfig);
    }

    private void maybeChangePlayerRenderView(boolean useTextureView) {
        if (this.useTextureView == useTextureView) {
            return;
        }

        this.useTextureView = useTextureView;
        exoPlayerView.swapVideoSurface(useTextureView);
    }

    @Override
    public PlayerView getView() {
        return exoPlayerView;
    }

    @Override
    public void play() {
        log.d("play");
        if (player == null) {
            log.w("Attempt to invoke 'play()' on null instance of the exoplayer.");
            return;
        }

        //If player already set to play, return.
        if (player.getPlayWhenReady()) {
            return;
        }

        sendDistinctEvent(PlayerEvent.Type.PLAY);

        player.setPlayWhenReady(true);
    }

    @Override
    public void pause() {
        if (player == null) {
            log.w("Attempt to invoke 'pause()' on null instance of the exoplayer");
            return;
        }

        //If player already set to pause, return.
        if (!player.getPlayWhenReady()) {
            return;
        }

        if (currentEvent == PlayerEvent.Type.ENDED) {
            return;
        }

        sendDistinctEvent(PlayerEvent.Type.PAUSE);
        player.setPlayWhenReady(false);
    }

    @Override
    public long getCurrentPosition() {
        if (player == null) {
            return Consts.POSITION_UNSET;
        }
        return player.getCurrentPosition();
    }

    @Override
    public void seekTo(long position) {
        if (player == null) {
            log.w("Attempt to invoke 'seekTo()' on null instance of the exoplayer");
            return;
        }
        isSeeking = true;
        sendDistinctEvent(PlayerEvent.Type.SEEKING);
        player.seekTo(position);
    }

    @Override
    public long getDuration() {
        return player == null ? Consts.TIME_UNSET : player.getDuration();
    }

    @Override
    public long getBufferedPosition() {
        if (player == null) {
            return Consts.POSITION_UNSET;
        }
        return player.getBufferedPosition();
    }

    @Override
    public void release() {
        log.d("release");
        if (player != null) {
            savePlayerPosition();
            this.eventLogger = null;
            player.release();
            player = null;
            trackSelectionHelper.release();
            trackSelectionHelper = null;
            eventLogger = null;
        }
        shouldRestorePlayerToPreviousState = true;
    }

    @Override
    public void restore() {
        log.d("resume");
        if (player == null) {
            initializePlayer();
        }
        if (playerPosition == Consts.TIME_UNSET) {
            player.seekToDefaultPosition(playerWindow);
        } else {
            player.seekTo(playerWindow, playerPosition);
        }
    }

    @Override
    public void destroy() {
        log.d("release");
        if (player != null) {
            player.release();
        }
        window = null;
        player = null;
        eventLogger = null;
        exoPlayerView = null;
        playerPosition = Consts.TIME_UNSET;
    }

    @Override
    public void changeTrack(String uniqueId) {
        if (trackSelectionHelper == null) {
            log.w("Attempt to invoke 'changeTrack()' on null instance of the TracksSelectionHelper");
            return;
        }
        trackSelectionHelper.changeTrack(uniqueId);
    }

    public PKTracks getPKTracks() {
        return this.tracks;
    }

    @Override
    public void startFrom(long position) {
        if (player == null) {
            log.w("Attempt to invoke 'startFrom()' on null instance of the exoplayer");
            return;
        }

        if (shouldRestorePlayerToPreviousState) {
            log.i("Restoring player from previous known state. So skip this block.");
            return;
        }
        isSeeking = false;
        player.seekTo(position);
    }

    public void setEventListener(final EventListener eventTrigger) {
        this.eventListener = eventTrigger;
    }

    public void setStateChangedListener(StateChangedListener stateChangedTrigger) {
        this.stateChangedListener = stateChangedTrigger;
    }

    @Override
    public void replay() {
        if (player == null) {
            log.w("Attempt to invoke 'replay()' on null instance of the exoplayer");
            return;
        }
        isSeeking = false;
        player.seekTo(0);
        player.setPlayWhenReady(true);
        sendDistinctEvent(PlayerEvent.Type.REPLAY);
    }

    @Override
    public void setVolume(float volume) {
        if (player == null) {
            log.w("Attempt to invoke 'setVolume()' on null instance of the exoplayer");
            return;
        }

        if (volume < 0) {
            volume = 0;
        } else if (volume > 1) {
            volume = 1;
        }

        if (volume != player.getVolume()) {
            player.setVolume(volume);
            sendEvent(PlayerEvent.Type.VOLUME_CHANGED);
        }
    }

    @Override
    public boolean isPlaying() {
        return player != null && player.getPlayWhenReady() && currentState == PlayerState.READY;
    }

    @Override
    public float getVolume() {
        if (player == null) {
            return Consts.VOLUME_UNKNOWN;
        }
        return player.getVolume();
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
        if (player != null) {
            player.setPlayWhenReady(false);
            player.seekTo(0);
            player.stop();
            sendDistinctEvent(PlayerEvent.Type.STOPPED);
        }
    }

    private void savePlayerPosition() {
        if (player == null) {
            log.w("Attempt to invoke 'savePlayerPosition()' on null instance of the exoplayer");
            return;
        }
        currentError = null;
        playerWindow = player.getCurrentWindowIndex();
        Timeline timeline = player.getCurrentTimeline();
        if (timeline != null && !timeline.isEmpty() && timeline.getWindow(playerWindow, window).isSeekable) {
            playerPosition = player.getCurrentPosition();
        }
    }

    public List<PKMetadata> getMetadata() {
        return metadataList;
    }

    private TrackSelectionHelper.TracksInfoListener initTracksInfoListener() {
        return new TrackSelectionHelper.TracksInfoListener() {
            @Override
            public void onTracksInfoReady(PKTracks tracksReady) {
                //when the track info is ready, cache it in ExoplayerWrapper. And send event that tracks are available.
                tracks = tracksReady;
                shouldRestorePlayerToPreviousState = false;
                sendDistinctEvent(PlayerEvent.Type.TRACKS_AVAILABLE);
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

    private DeferredDrmSessionManager.DrmSessionListener initDrmSessionListener() {
        return new DeferredDrmSessionManager.DrmSessionListener() {
            @Override
            public void onError(PKError error) {
                currentError = error;
                sendEvent(PlayerEvent.Type.ERROR);
            }
        };
    }

    @Override
    public BaseTrack getLastSelectedTrack(int renderType) {
        return trackSelectionHelper.getLastSelectedTrack(renderType);
    }

    @Override
    public boolean isLiveStream() {
        return player != null && player.isCurrentWindowDynamic();
    }

    @Override
    public void onConfigurationChanged() {
        //Do nothing.
    }
}


