package com.kaltura.playkit.player;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelections;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSource.Factory;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.player.PlayerController.EventListener;
import com.kaltura.playkit.player.PlayerController.StateChangedListener;
import com.kaltura.playkit.utils.EventLogger;


/**
 * Created by anton.afanasiev on 31/10/2016.
 */
public class ExoPlayerWrapper implements PlayerEngine, ExoPlayer.EventListener, TrackSelector.EventListener<MappingTrackSelector.MappedTrackInfo> {
    private static final String TAG = ExoPlayerWrapper.class.getSimpleName();
    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();

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

    private boolean firstPlay;
    private boolean isSeeking = false;

    private int playerWindow;
    private long playerPosition;
    private Uri lastPlayedSource;
    private Timeline.Window window;
    private boolean isTimelineStatic;


    public ExoPlayerWrapper(Context context) {
        this.context = context;
        mediaDataSourceFactory = buildDataSourceFactory(true);
        exoPlayerView = new CustomExoPlayerView(context);
        window = new Timeline.Window();
    }

    private void initializePlayer() {
        eventLogger = new EventLogger();
        DefaultTrackSelector trackSelector = initializeTrackSelector();

        player = ExoPlayerFactory.newSimpleInstance(context, trackSelector, new DefaultLoadControl(), null, false); // TODO check if we need DRM Session manager.
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
            player.setId3Output(eventLogger);
        }
    }

    private DefaultTrackSelector initializeTrackSelector() {
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveVideoTrackSelection.Factory(BANDWIDTH_METER);
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(mainHandler, videoTrackSelectionFactory);
        trackSelector.addListener(this);
        trackSelector.addListener(eventLogger);

        return trackSelector;
    }

    private void preparePlayer(Uri mediaSourceUri) {
        firstPlay = !mediaSourceUri.equals(lastPlayedSource);
        this.lastPlayedSource = mediaSourceUri;
        changeState(PlayerState.LOADING);
        MediaSource mediaSource = buildMediaSource(mediaSourceUri, null);
        player.prepare(mediaSource, isTimelineStatic, isTimelineStatic);
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
        return new DefaultHttpDataSourceFactory(Util.getUserAgent(context, "PlayKit"), useBandwidthMeter ? BANDWIDTH_METER : null);
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

    private void sendEvent(PlayerEvent.Type newEvent) {
        if (newEvent.equals(currentEvent)) {
            return;
        }

        currentEvent = newEvent;
        if (eventListener != null) {
            eventListener.onEvent(currentEvent);
        }
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
        Log.d(TAG, "onLoadingChanged. isLoading => " + isLoading);
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        switch (playbackState) {
            case ExoPlayer.STATE_IDLE:
                Log.d(TAG, "onPlayerStateChanged. IDLE. playWhenReady => " + playWhenReady);
                changeState(PlayerState.IDLE);
                if (isSeeking) {
                    isSeeking = false;
                }
                break;
            case ExoPlayer.STATE_BUFFERING:
                Log.d(TAG, "onPlayerStateChanged. BUFFERING. playWhenReady => " + playWhenReady);
                changeState(PlayerState.BUFFERING);
                break;
            case ExoPlayer.STATE_READY:
                Log.d(TAG, "onPlayerStateChanged. READY. playWhenReady => " + playWhenReady);
                changeState(PlayerState.READY);

                if (isSeeking) {
                    isSeeking = false;
                    sendEvent(PlayerEvent.Type.SEEKED);
                }

                if (!previousState.equals(PlayerState.READY)) {
                    sendEvent(PlayerEvent.Type.CAN_PLAY);
                }

                if (playWhenReady) {
                    if (firstPlay) {
                        firstPlay = false;
                        sendEvent(PlayerEvent.Type.FIRST_PLAY);
                    }

                    sendEvent(PlayerEvent.Type.PLAYING);
                }

                break;
            case ExoPlayer.STATE_ENDED:
                Log.d(TAG, "onPlayerStateChanged. ENDED. playWhenReady => " + playWhenReady);
                changeState(PlayerState.IDLE);
                sendEvent(PlayerEvent.Type.ENDED);
                break;
            default:
                break;

        }

    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {
        Log.d(TAG, "onTimelineChanged");
        isTimelineStatic = timeline != null && timeline.getWindowCount() > 0
                && !timeline.getWindow(timeline.getWindowCount() - 1, window).isDynamic;
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {

        Log.d(TAG, "onPlayerError error type => " + error.type);
        sendEvent(PlayerEvent.Type.ERROR);
    }

    @Override
    public void onPositionDiscontinuity() {
        Log.d(TAG, "onPositionDiscontinuity");
    }

    @Override
    public void onTrackSelectionsChanged(TrackSelections<? extends MappingTrackSelector.MappedTrackInfo> trackSelections) {
        Log.d(TAG, "onTrackSelectionsChanged");
    }

    @Override
    public void load(Uri mediaSourceUri) {
        Log.d(TAG, "load");
        if (player == null) {
            initializePlayer();
        }

        preparePlayer(mediaSourceUri);

    }

    @Override
    public View getView() {
        return exoPlayerView;
    }

    @Override
    public void play() {
        Log.d(TAG, "play");
        if (player.getPlayWhenReady()) {
            return;
        }
        
        sendEvent(PlayerEvent.Type.PLAY);
        
        player.setPlayWhenReady(true);
    }

    @Override
    public void pause() {
        if (!player.getPlayWhenReady()) {
            return;
        }
        sendEvent(PlayerEvent.Type.PAUSE);
        player.setPlayWhenReady(false);
    }

    @Override
    public long getCurrentPosition() {
        if (player != null) {
            return player.getCurrentPosition();
        }
        return C.POSITION_UNSET;
    }

    @Override
    public void seekTo(long position) {
        isSeeking = true;
        sendEvent(PlayerEvent.Type.SEEKING);
        player.seekTo(position);
    }

    @Override
    public long getDuration() {
        if (player != null) {
            return player.getDuration();
        }
        return C.TIME_UNSET;
    }

    @Override
    public long getBufferedPosition() {
        if (player != null) {
            return player.getBufferedPosition();
        }
        return C.POSITION_UNSET;
    }

    @Override
    public void release() {
        Log.d(TAG, "release");
        if (player != null) {

            playerWindow = player.getCurrentWindowIndex();
            playerPosition = C.TIME_UNSET;
            Timeline timeline = player.getCurrentTimeline();
            if (timeline != null && timeline.getWindow(playerWindow, window).isSeekable) {
                playerPosition = player.getCurrentPosition();
            }

            this.eventLogger = null;
            player.release();
            player = null;
            eventLogger = null;
        }
    }

    @Override
    public void restore() {
        Log.d(TAG, "resume");
        initializePlayer();

        if (isTimelineStatic) {
            if (playerPosition == C.TIME_UNSET) {
                player.seekToDefaultPosition(playerWindow);
            } else {
                player.seekTo(playerWindow, playerPosition);
            }
        }
    }

    public void setEventListener(final EventListener eventTrigger) {
        this.eventListener = eventTrigger;
    }

    public void setStateChangedListener(StateChangedListener stateChangedTrigger) {
        this.stateChangedListener = stateChangedTrigger;
    }

}
