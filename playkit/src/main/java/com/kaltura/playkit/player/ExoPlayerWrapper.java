package com.kaltura.playkit.player;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.ExoPlayerLibraryInfo;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSource.Factory;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.kaltura.playkit.BuildConfig;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.PlaybackParamsInfo;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.drm.DeferredDrmSessionManager;
import com.kaltura.playkit.player.PlayerController.EventListener;
import com.kaltura.playkit.player.PlayerController.StateChangedListener;
import com.kaltura.playkit.utils.Consts;
import com.kaltura.playkit.utils.EventLogger;


/**
 * Created by anton.afanasiev on 31/10/2016.
 */
class ExoPlayerWrapper implements PlayerEngine, ExoPlayer.EventListener {

    private static final PKLog log = PKLog.get("ExoPlayerWrapper");

    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();

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
    private PlayerState currentState = PlayerState.IDLE, previousState;

    private Factory mediaDataSourceFactory;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private Exception currentException = null;

    private boolean isSeeking = false;

    private int playerWindow;
    private long playerPosition;
    private Uri lastPlayedSource;
    private Timeline.Window window;
    private boolean shouldGetTracksInfo;
    private boolean shouldResetPlayerPosition;
    private long prevDuration = Consts.TIME_UNSET;
    private int sameErrorOccurrenceCounter = 0;


    interface TracksInfoListener {

        void onTracksInfoReady(PKTracks PKTracks);

        void onTrackChanged();
    }

    private TracksInfoListener tracksInfoListener = new TracksInfoListener() {
        @Override
        public void onTracksInfoReady(PKTracks tracksReady) {
            //when the track info is ready, cache it in ExoplayerWrapper. And send event that tracks are available.
            tracks = tracksReady;
            sendDistinctEvent(PlayerEvent.Type.TRACKS_AVAILABLE);
        }

        @Override
        public void onTrackChanged() {
            sendEvent(PlayerEvent.Type.PLAYBACK_PARAMS_UPDATED);
        }
    };


    ExoPlayerWrapper(Context context) {
        this.context = context;
        mediaDataSourceFactory = buildDataSourceFactory(true);
        exoPlayerView = new ExoPlayerView(context);
        window = new Timeline.Window();
    }

    private void initializePlayer() {
        eventLogger = new EventLogger();

        DefaultTrackSelector trackSelector = initializeTrackSelector();
        drmSessionManager = new DeferredDrmSessionManager(mainHandler, eventLogger, buildHttpDataSourceFactory(false));
        player = ExoPlayerFactory.newSimpleInstance(context, trackSelector, new DefaultLoadControl(), drmSessionManager);
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
            player.setMetadataOutput(eventLogger);
        }
    }

    private DefaultTrackSelector initializeTrackSelector() {

        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveVideoTrackSelection.Factory(BANDWIDTH_METER);
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        trackSelectionHelper = new TrackSelectionHelper(trackSelector, videoTrackSelectionFactory);
        trackSelectionHelper.setTracksInfoListener(tracksInfoListener);

        return trackSelector;
    }

    private void preparePlayer(PKMediaSource pkMediaSource) {
        sameErrorOccurrenceCounter = 0;
        drmSessionManager.setMediaSource(pkMediaSource);

        shouldGetTracksInfo = true;
        this.lastPlayedSource = Uri.parse(pkMediaSource.getUrl());
        MediaSource mediaSource = buildExoMediaSource(pkMediaSource);
        player.prepare(mediaSource, shouldResetPlayerPosition, shouldResetPlayerPosition);
        changeState(PlayerState.LOADING);
    }

    private MediaSource buildExoMediaSource(PKMediaSource source) {
        PKMediaFormat format = source.getMediaFormat();
        if (format == null) {
            // TODO: error?
            return null;
        }

        Uri uri = Uri.parse(source.getUrl());


        switch (format) {
            case mp4_clear:
                return new ExtractorMediaSource(uri, mediaDataSourceFactory, new DefaultExtractorsFactory(),
                        mainHandler, eventLogger);

            case dash_clear:
            case dash_widevine:
                return new DashMediaSource(uri, buildDataSourceFactory(false),
                        new DefaultDashChunkSource.Factory(mediaDataSourceFactory), mainHandler, eventLogger);

            case hls_clear:
                return new HlsMediaSource(uri, mediaDataSourceFactory, mainHandler, eventLogger);

            default:
                throw new IllegalStateException("Unsupported type: " + format);
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
        if (currentException != null) {
            //if error have same message as the previous one, update the errorCounter.
            //this is need to avoid infinity retries on the same error.
            if (currentException.getMessage().equals(error.getMessage())) {
                sameErrorOccurrenceCounter++;
            } else {
                sameErrorOccurrenceCounter = 0;
            }
        }

        currentException = error;
        sendDistinctEvent(PlayerEvent.Type.ERROR);
    }

    @Override
    public void onPositionDiscontinuity() {
        log.d("onPositionDiscontinuity");
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
    public void load(PKMediaSource mediaSource) {
        log.d("load");
        if (player == null) {
            initializePlayer();
        }

        preparePlayer(mediaSource);
    }

    @Override
    public PlayerView getView() {
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
            savePlayerPosition();
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
        return this.tracks;
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

    @Override
    public PlayerEvent.ExceptionInfo getCurrentException() {
        return new PlayerEvent.ExceptionInfo(currentException, sameErrorOccurrenceCounter);
    }

    void savePlayerPosition() {
        if (player == null) {
            log.e("Attempt to invoke 'savePlayerPosition()' on null instance of the exoplayer");
            return;
        }
        currentException = null;
        playerWindow = player.getCurrentWindowIndex();
        playerPosition = Consts.TIME_UNSET;
        Timeline timeline = player.getCurrentTimeline();
        if (timeline != null && !timeline.isEmpty() && timeline.getWindow(playerWindow, window).isSeekable) {
            playerPosition = player.getCurrentPosition();
        }
    }
}


