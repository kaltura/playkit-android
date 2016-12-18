package com.kaltura.playkit.player;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.ExoPlayerLibraryInfo;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.StreamingDrmSessionManager;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSource.Factory;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.kaltura.playkit.BuildConfig;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKTracks;
import com.kaltura.playkit.PlaybackParamsInfo;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.player.PlayerController.EventListener;
import com.kaltura.playkit.player.PlayerController.StateChangedListener;
import com.kaltura.playkit.utils.Consts;
import com.kaltura.playkit.utils.EventLogger;

import java.util.UUID;


/**
 * Created by anton.afanasiev on 31/10/2016.
 */
class ExoPlayerWrapper implements PlayerEngine, ExoPlayer.EventListener {

    private static final PKLog log = PKLog.get("ExoPlayerWrapper");

    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();

    static final UUID WIDEVINE_UUID = UUID.fromString("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed");

    private EventLogger eventLogger;
    private EventListener eventListener;
    private StateChangedListener stateChangedListener;

    private Context context;
    private SimpleExoPlayer player;
    private CustomExoPlayerView exoPlayerView;

    private PlayerEvent.Type currentEvent;
    private PlayerState currentState = PlayerState.IDLE, previousState;

    private Handler mainHandler = new Handler();
    private Factory mediaDataSourceFactory;

    private boolean isSeeking = false;

    private int playerWindow;
    private long playerPosition;
    private Uri lastPlayedSource;
    private Timeline.Window window;
    private boolean shouldResetPlayerPosition;
    private DeferredMediaDrmCallback deferredMediaDrmCallback;
    private String licenseUri;
    private long prevDuration = Consts.TIME_UNSET;

    private DefaultTrackSelector trackSelector;
    private PKTracks tracksInfo;
    private boolean shouldGetTracksInfo;

    private TrackSelectionHelper trackSelectionHelper;


    interface TracksInfoListener {

        void onTracksInfoReady(PKTracks PKTracks);

        void onTrackChanged();
    }

    private TracksInfoListener tracksInfoListener = new TracksInfoListener() {
        @Override
        public void onTracksInfoReady(PKTracks tracksInfoReady) {
            //when the track info is ready, cache it in ExoplayerWrapper. And send event that tracks are available.
            tracksInfo = tracksInfoReady;
            sendDistinctEvent(PlayerEvent.Type.TRACKS_AVAILABLE);
        }

        @Override
        public void onTrackChanged() {
            sendEvent(PlayerEvent.Type.PLAYBACK_PARAMS);
        }
    };


    ExoPlayerWrapper(Context context) {
        this.context = context;
        mediaDataSourceFactory = buildDataSourceFactory(true);
        exoPlayerView = new CustomExoPlayerView(context);
        window = new Timeline.Window();
    }

    private DeferredMediaDrmCallback.UrlProvider licenseUrlProvider = new DeferredMediaDrmCallback.UrlProvider() {
        @Override
        public String getUrl() {
            return licenseUri;
        }
    };

    private void initializePlayer() {
        eventLogger = new EventLogger();

        MappingTrackSelector trackSelector = initializeTrackSelector();

        // TODO: check if there's any overhead involved in creating a session manager and not using it.
        DrmSessionManager<FrameworkMediaCrypto> drmSessionManager = null;
        try {
            drmSessionManager = buildDrmSessionManager(WIDEVINE_UUID);
        } catch (UnsupportedDrmException e) {
            // TODO: proper error
            log.w("This device doesn't support widevine modular");
        }
        @SimpleExoPlayer.ExtensionRendererMode int extensionRendererMode = SimpleExoPlayer.EXTENSION_RENDERER_MODE_OFF;
        player = ExoPlayerFactory.newSimpleInstance(context, trackSelector, new DefaultLoadControl(), drmSessionManager, extensionRendererMode);
        setPlayerListeners();
        exoPlayerView.setPlayer(player);
        player.setPlayWhenReady(false);
    }

    private DrmSessionManager<FrameworkMediaCrypto> buildDrmSessionManager(UUID uuid) throws UnsupportedDrmException {
        if (Util.SDK_INT < 18) {
            return null;
        }

        // Using the deferred callback because we don't yet have the license URL.
        deferredMediaDrmCallback = new DeferredMediaDrmCallback(buildHttpDataSourceFactory(false), licenseUrlProvider);
        return new StreamingDrmSessionManager<>(uuid,
                FrameworkMediaDrm.newInstance(uuid), deferredMediaDrmCallback, null, mainHandler, eventLogger);
    }


    private void setPlayerListeners() {
        if (player != null) {
            player.addListener(this);
            player.addListener(eventLogger);
            player.setVideoDebugListener(eventLogger);
            player.setAudioDebugListener(eventLogger);
            player.setId3Output(eventLogger);
            player.setVideoDebugListener(trackSelectionHelper);
            player.setAudioDebugListener(trackSelectionHelper);
        }
    }

    private MappingTrackSelector initializeTrackSelector() {

        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveVideoTrackSelection.Factory(BANDWIDTH_METER);
        trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        trackSelectionHelper = new TrackSelectionHelper(trackSelector, videoTrackSelectionFactory);
        trackSelectionHelper.setTracksInfoListener(tracksInfoListener);

        return trackSelector;
    }

    private void preparePlayer(Uri mediaSourceUri, String licenseUri) {
        this.licenseUri = licenseUri;
        shouldGetTracksInfo = true;
        this.lastPlayedSource = mediaSourceUri;
        MediaSource mediaSource = buildMediaSource(mediaSourceUri, null);
        player.prepare(mediaSource, shouldResetPlayerPosition, shouldResetPlayerPosition);
        changeState(PlayerState.LOADING);
    }

    private MediaSource buildMediaSource(Uri uri, String overrideExtension) {

        int type = Util.inferContentType(!TextUtils.isEmpty(overrideExtension) ? "." + overrideExtension
                : uri.getLastPathSegment());
        switch (type) {
            case C.TYPE_SS:
                return new SsMediaSource(uri, buildDataSourceFactory(false),
                        new DefaultSsChunkSource.Factory(mediaDataSourceFactory), mainHandler, eventLogger);
            case C.TYPE_DASH:
                return new DashMediaSource(uri, buildDataSourceFactory(false),
                        new DefaultDashChunkSource.Factory(mediaDataSourceFactory), mainHandler, eventLogger);
            case C.TYPE_HLS:
                return new HlsMediaSource(uri, mediaDataSourceFactory, mainHandler, eventLogger);
            case C.TYPE_OTHER:
                return new ExtractorMediaSource(uri, mediaDataSourceFactory, new DefaultExtractorsFactory(),
                        mainHandler, eventLogger);
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }

    /**
     * Returns a new DataSource factory.
     *
     * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
     *                          DataSource factory.
     * @return A new DataSource factory.
     */
    private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
        return new DefaultDataSourceFactory(context, useBandwidthMeter ? BANDWIDTH_METER : null,
                buildHttpDataSourceFactory(useBandwidthMeter));
    }

    /**
     * Returns a new HttpDataSource factory.
     *
     * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
     *                          DataSource factory.
     * @return A new HttpDataSource factory.
     */
    private HttpDataSource.Factory buildHttpDataSourceFactory(boolean useBandwidthMeter) {
        return new DefaultHttpDataSourceFactory(getUserAgent(context), useBandwidthMeter ? BANDWIDTH_METER : null);
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

        String sdkName = "PlayKit/" + BuildConfig.VERSION_NAME;

        return sdkName + " " + applicationName + " (Linux;Android " + Build.VERSION.RELEASE
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
        currentEvent = event;
        if (eventListener != null) {
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
            case ExoPlayer.STATE_IDLE:
                log.d("onPlayerStateChanged. IDLE. playWhenReady => " + playWhenReady);
                changeState(PlayerState.IDLE);
                if (isSeeking) {
                    isSeeking = false;
                }
                break;
            case ExoPlayer.STATE_BUFFERING:
                log.d("onPlayerStateChanged. BUFFERING. playWhenReady => " + playWhenReady);
                changeState(PlayerState.BUFFERING);
                break;
            case ExoPlayer.STATE_READY:
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
            case ExoPlayer.STATE_ENDED:
                log.d("onPlayerStateChanged. ENDED. playWhenReady => " + playWhenReady);
                changeState(PlayerState.IDLE);
                sendDistinctEvent(PlayerEvent.Type.ENDED);
                break;
            default:
                break;

        }

    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {
        log.d("onTimelineChanged");
        shouldResetPlayerPosition = timeline != null && !timeline.isEmpty()
                && !timeline.getWindow(timeline.getWindowCount() - 1, window).isDynamic;

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        log.d("onPlayerError error type => " + error.type);
        sendDistinctEvent(PlayerEvent.Type.ERROR);
    }

    @Override
    public void onPositionDiscontinuity() {
        log.d("onPositionDiscontinuity");
    }


    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        log.d("onTracksChanged");
        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo != null) {
            if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_VIDEO)
                    == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                log.w("Error unsupported video");
            }
            if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_AUDIO)
                    == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                log.w("Error unsupported audio");
            }
        }
        //if the track info new -> map the available tracks. and when ready, notify user about available tracks.
        if (shouldGetTracksInfo) {
            shouldGetTracksInfo = false;
            trackSelectionHelper.prepareTracksInfo();
        }
    }

    @Override
    public void load(Uri mediaSourceUri, String licenseUri) {
        log.d("load");
        if (player == null) {
            initializePlayer();
        }

        preparePlayer(mediaSourceUri, licenseUri);
    }

    @Override
    public View getView() {
        return exoPlayerView;
    }

    @Override
    public void play() {
        log.d("play");
        if (player == null || player.getPlayWhenReady()) {
            log.e("Attempt to invoke 'play()' on null instance of the exoplayer");
            return;
        }

        sendDistinctEvent(PlayerEvent.Type.PLAY);

        player.setPlayWhenReady(true);
    }

    @Override
    public void pause() {
        if (player == null || !player.getPlayWhenReady()) {
            log.e("Attempt to invoke 'pause()' on null instance of the exoplayer");
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
            log.e("Attempt to invoke 'seekTo()' on null instance of the exoplayer");
            return;
        }
        isSeeking = true;
        sendDistinctEvent(PlayerEvent.Type.SEEKING);
        player.seekTo(position);
    }

    @Override
    public long getDuration() {
        long currentDuration;
        currentDuration = player == null ? Consts.TIME_UNSET : player.getDuration();
        if (prevDuration != currentDuration) {
            sendDistinctEvent(PlayerEvent.Type.DURATION_CHANGE);
        }
        prevDuration = currentDuration;
        return prevDuration;
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
            playerWindow = player.getCurrentWindowIndex();
            playerPosition = Consts.TIME_UNSET;
            Timeline timeline = player.getCurrentTimeline();
            if (timeline != null && !timeline.isEmpty() && timeline.getWindow(playerWindow, window).isSeekable) {
                playerPosition = player.getCurrentPosition();
            }
            this.eventLogger = null;
            player.release();
            player = null;
            trackSelectionHelper.release();
            trackSelectionHelper = null;
            eventLogger = null;
        }
    }

    @Override
    public void restore() {
        log.d("resume");
        initializePlayer();
        if (shouldResetPlayerPosition) {
            if (playerPosition == Consts.TIME_UNSET) {
                player.seekToDefaultPosition(playerWindow);
            } else {
                player.seekTo(playerWindow, playerPosition);
            }
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
        lastPlayedSource = null;
    }

    @Override
    public void changeTrack(String uniqueId) {
        if (trackSelectionHelper == null) {
            log.e("Attempt to invoke 'changeTrack()' on null instance of the TracksSelectionHelper");
            return;
        }
        trackSelectionHelper.changeTrack(uniqueId);
    }

    public PKTracks getPKTracks() {
        return this.tracksInfo;
    }

    @Override
    public void startFrom(long position) {
        if (player == null) {
            log.e("Attempt to invoke 'startFrom()' on null instance of the exoplayer");
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
            log.e("Attempt to invoke 'replay()' on null instance of the exoplayer");
            return;
        }
        isSeeking = false;
        player.seekTo(0);
        sendDistinctEvent(PlayerEvent.Type.REPLAY);
    }

    @Override
    public void setVolume(float volume) {
        if (player == null) {
            log.e("Attempt to invoke 'setVolume()' on null instance of the exoplayer");
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
        if (player == null) {
            return false;
        }
        return player.getPlayWhenReady() && currentState == PlayerState.READY;
    }

    @Override
    public float getVolume() {
        if (player == null) {
            return Consts.VOLUME_UNKNOWN;
        }
        return player.getVolume();
    }

    @Override
    public PlaybackParamsInfo getPlaybackParamsInfo() {
        return new PlaybackParamsInfo(lastPlayedSource.toString(), trackSelectionHelper.getCurrentVideoBitrate(), trackSelectionHelper.getCurrentAudioBitrate());
    }
}


