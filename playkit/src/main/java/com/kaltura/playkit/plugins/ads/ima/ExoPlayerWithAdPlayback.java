package com.kaltura.playkit.plugins.ads.ima;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.google.ads.interactivemedia.v3.api.player.ContentProgressProvider;
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.PlaybackPreparer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.EventLogger;
import com.google.android.exoplayer2.util.Util;
import com.kaltura.playkit.PKError;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.drm.DeferredDrmSessionManager;
import com.kaltura.playkit.plugins.ads.AdCuePoints;
import com.kaltura.playkit.utils.Consts;

import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * Video player that can play content video and ads.
 */
public class ExoPlayerWithAdPlayback extends RelativeLayout implements PlaybackPreparer, Player.EventListener {
    private static final PKLog log = PKLog.get("ExoPlayerWithAdPlayback");

    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    private DefaultTrackSelector trackSelector;
    private TrackSelectionHelper trackSelectionHelper;
    private TrackGroupArray lastSeenTrackGroupArray;
    private EventLogger eventLogger;
    private android.os.Handler mainHandler = new Handler();
    private DefaultRenderersFactory renderersFactory;
    private SimpleExoPlayer player;

    private DataSource.Factory mediaDataSourceFactory;
    private Context mContext;
    private com.kaltura.playkit.Player contentPlayer;
    private boolean isPlayerReady = false;
    private AdCuePoints adCuePoints;
    private int adLoadTimeout = 8000; // mili sec

    // The wrapped video player.
    private PlayerView mVideoPlayer;

    // The SDK will render ad playback UI elements into this ViewGroup.
    private ViewGroup mAdUiContainer;

    // Used to track if the current video is an ad (as opposed to a content video).
    private boolean mIsAdDisplayed;

    // Used to track the current content video URL to resume content playback.
    private String mContentVideoUrl;

    // The saved position in the ad to resume if app is backgrounded during ad playback.
    private long mSavedAdPosition;

    // The saved position in the content to resume to after ad playback or if app is backgrounded
    // during content playback.
    private long mSavedContentPosition;

    // VideoAdPlayer interface implementation for the SDK to send ad play/pause type events.
    private VideoAdPlayer mVideoAdPlayer;

    // ContentProgressProvider interface implementation for the SDK to check content progress.
    private ContentProgressProvider mContentProgressProvider;

    private boolean adShouldPAutolay = true;

    private final List<VideoAdPlayer.VideoAdPlayerCallback> mAdCallbacks =
            new ArrayList<VideoAdPlayer.VideoAdPlayerCallback>(1);


    public ExoPlayerWithAdPlayback(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mContext = context;
    }

    public ExoPlayerWithAdPlayback(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public ExoPlayerWithAdPlayback(Context context, int adLoadTimeout) {
        super(context,null);
        this.mContext = context;
        if (adLoadTimeout < Consts.MILLISECONDS_MULTIPLIER) {
            this.adLoadTimeout = adLoadTimeout * 1000;
        }
        init();
    }

    public ViewGroup getmAdUiContainer() {
        return mAdUiContainer;
    }

    public PlayerView getExoPlayerView() {
        return mVideoPlayer;
    }

    private DeferredDrmSessionManager.DrmSessionListener initDrmSessionListener() {
        return new DeferredDrmSessionManager.DrmSessionListener() {
            @Override
            public void onError(PKError error) {
            }
        };
    }

    private void init() {
        mIsAdDisplayed = false;
        mSavedAdPosition = 0;
        mSavedContentPosition = 0;
        mVideoPlayer = new PlayerView(getContext());
        mVideoPlayer.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mVideoPlayer.setId(new Integer(123456789));
        mVideoPlayer.setUseController(false);
        if (player == null) {

            mediaDataSourceFactory = buildDataSourceFactory(true);

            renderersFactory = new DefaultRenderersFactory(mContext,
                    null, DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF);

            TrackSelection.Factory adaptiveTrackSelectionFactory =
                    new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
            trackSelector = new DefaultTrackSelector(adaptiveTrackSelectionFactory);
            trackSelectionHelper = new TrackSelectionHelper(trackSelector, adaptiveTrackSelectionFactory);
            lastSeenTrackGroupArray = null;
            eventLogger = new EventLogger(trackSelector);
            player = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector);
            player.addListener(eventLogger);
            player.addMetadataOutput(eventLogger);
            player.addAudioDebugListener(eventLogger);
            player.addVideoDebugListener(eventLogger);
            mVideoPlayer.setPlayer(player);
        }

        mAdUiContainer = mVideoPlayer;

        // Define VideoAdPlayer connector.
        mVideoAdPlayer = new VideoAdPlayer() {
            @Override
            public void playAd() {
                log.d("playAd mIsAdDisplayed = " + mIsAdDisplayed);
                mVideoPlayer.getPlayer().setPlayWhenReady(adShouldPAutolay);
                if (mIsAdDisplayed && isPlayerReady) {
                    for (VideoAdPlayer.VideoAdPlayerCallback callback : mAdCallbacks) {
                        log.d("playAd->onResume");
                        callback.onResume();
                    }
                } else {
                    mIsAdDisplayed = true;
                    for (VideoAdPlayer.VideoAdPlayerCallback callback : mAdCallbacks) {
                        log.d("playAd->onPlay");
                        callback.onPlay();
                    }
                }

                //Make sure events will be fired ater pause
                for (VideoAdPlayer.VideoAdPlayerCallback callback : mAdCallbacks) {
                    callback.onPlay();
                }
            }

            @Override
            public void loadAd(String url) {
                log.d("loadAd = " + url);
                mIsAdDisplayed = true;
                initializePlayer(Uri.parse(url));
            }

            @Override
            public void stopAd() {
                log.d("stopAd");
                isPlayerReady = false;
                mVideoPlayer.getPlayer().stop();
            }

            @Override
            public void pauseAd() {
                log.d("pauseAd");
                for (VideoAdPlayer.VideoAdPlayerCallback callback : mAdCallbacks) {
                    callback.onPause();
                }
                mVideoPlayer.getPlayer().setPlayWhenReady(false);
            }

            @Override
            public void resumeAd() {
                log.d("resumeAd");
                playAd();

            }

            @Override
            public void addCallback(VideoAdPlayerCallback videoAdPlayerCallback) {
                mAdCallbacks.add(videoAdPlayerCallback);
            }

            @Override
            public void removeCallback(VideoAdPlayerCallback videoAdPlayerCallback) {
                mAdCallbacks.remove(videoAdPlayerCallback);
            }

            @Override
            public VideoProgressUpdate getAdProgress() {
                long duration = mVideoPlayer.getPlayer().getDuration();
                long position = mVideoPlayer.getPlayer().getCurrentPosition();
                if (!isPlayerReady || !mIsAdDisplayed || duration < 0 || position < 0) {
                    return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
                }

                //log.d("getAdProgress getDuration " +  duration);
                //log.d("getAdProgress getCurrentPosition " +  position);
                return new VideoProgressUpdate(position, duration);
            }
        };
        mVideoPlayer.getPlayer().addListener(this);
    }

    @Override
    public void preparePlayback() {
        log.d("preparePlayback");
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {
        log.d("onTimelineChanged");
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        log.d("onLoadingChanged");
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
        log.d("onTracksChanged");
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        log.d("onPlayerStateChanged " + playbackState);
        switch (playbackState) {
            case Player.STATE_IDLE:
                log.d("onPlayerStateChanged. IDLE. playWhenReady => " + playWhenReady);
                break;
            case Player.STATE_BUFFERING:
                log.d("onPlayerStateChanged. BUFFERING. playWhenReady => " + playWhenReady);

                break;
            case Player.STATE_READY:
                log.d("onPlayerStateChanged. READY. playWhenReady => " + playWhenReady);
                isPlayerReady = true;
                if (playWhenReady) {
                    if (mVideoPlayer.getPlayer().getDuration() > 0) {
                        for (VideoAdPlayer.VideoAdPlayerCallback callback : mAdCallbacks) {
                            callback.onResume();
                        }
                    } else {
                        for (VideoAdPlayer.VideoAdPlayerCallback callback : mAdCallbacks) {
                            callback.onPlay();
                        }
                    }
                }
                else {
                    for (VideoAdPlayer.VideoAdPlayerCallback callback : mAdCallbacks) {
                        callback.onPause();
                    }
                }
                break;
            case Player.STATE_ENDED:
                log.d("onPlayerStateChanged. ENDED. playWhenReady => " + playWhenReady);
                isPlayerReady = false;
                if (mIsAdDisplayed) {
                    for (VideoAdPlayer.VideoAdPlayerCallback callback : mAdCallbacks) {
                        callback.onEnded();
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {
        log.d("onRepeatModeChanged");
    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
        log.d("onShuffleModeEnabledChanged");
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        log.d("onPlayerError " + error.getMessage());
        for (VideoAdPlayer.VideoAdPlayerCallback callback : mAdCallbacks) {
            callback.onError();
        }

    }

    @Override
    public void onPositionDiscontinuity(int reason) {
        log.d("onPositionDiscontinuity");
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
        log.d("onPlaybackParametersChanged");

    }

    @Override
    public void onSeekProcessed() {
        log.d("onSeekProcessed");
    }

    public void setContentProgressProvider(final com.kaltura.playkit.Player contentPlayer) {
        this.contentPlayer = contentPlayer;
        mContentProgressProvider = new ContentProgressProvider() {
            @Override
            public VideoProgressUpdate getContentProgress() {

                if (contentPlayer.getDuration() <= 0) {
                    return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
                }
                long duration = contentPlayer.getDuration();
                long position = contentPlayer.getCurrentPosition();
                //log.d("xxx getContentProgress getDuration " +  duration);
                //log.d("xxx getContentProgress getCurrentPosition " + position);
                if (position > 0 && duration > 0 && position >= duration && adCuePoints != null && !adCuePoints.hasPostRoll()) {
                    mContentProgressProvider = null;
                    return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
                }
                return new VideoProgressUpdate(contentPlayer.getCurrentPosition(),
                        duration);
            }
        };
    }

    /**
     * Save the playback progress state of the currently playing video. This is called when content
     * is paused to prepare for ad playback or when app is backgrounded.
     */
    public void savePosition() {
        if (mIsAdDisplayed) {
            mSavedAdPosition = mVideoPlayer.getPlayer().getCurrentPosition();
        } else {
            mSavedContentPosition = mVideoPlayer.getPlayer().getCurrentPosition();
        }
    }

    /**
     * Restore the currently loaded video to its previously saved playback progress state. This is
     * called when content is resumed after ad playback or when focus has returned to the app.
     */
    public void restorePosition() {
        if (mIsAdDisplayed) {
            mVideoPlayer.getPlayer().seekTo(mSavedAdPosition);
        } else {
            mVideoPlayer.getPlayer().seekTo(mSavedContentPosition);
        }
    }

    /**
     * Pauses the content video.
     */
    public void pause() {
        mVideoPlayer.getPlayer().setPlayWhenReady(false);
    }

    public void stop() {
        isPlayerReady = false;
        mVideoPlayer.getPlayer().setPlayWhenReady(false);
        mVideoPlayer.getPlayer().stop();
    }

    /**
     * Plays the content video.
     */
    public void play() {
        mVideoPlayer.getPlayer().setPlayWhenReady(true);
    }

    /**
     * Seeks the content video.
     */
    public void seek(int time) {
        if (mIsAdDisplayed) {
            // When ad is playing, set the content video position to seek to when ad finishes.
            mSavedContentPosition = time;
        } else {
            mVideoPlayer.getPlayer().seekTo(time);
        }
    }

    /**
     * Returns current content video play time.
     */
    public long getCurrentContentTime() {
        if (mIsAdDisplayed) {
            return mSavedContentPosition;
        } else {
            return mVideoPlayer.getPlayer().getCurrentPosition();
        }
    }

    /**
     * Pause the currently playing content video in preparation for an ad to play, and disables
     * the media controller.
     */
    public void pauseContentForAdPlayback() {
        //mVideoPlayer.getPlayer().disablePlaybackControls();
        savePosition();
        mVideoPlayer.getPlayer().stop();
    }

    /**
     * Returns the UI element for rendering video ad elements.
     */
    public ViewGroup getAdUiContainer() {
        return mAdUiContainer;
    }

    /**
     * Returns an implementation of the SDK's VideoAdPlayer interface.
     */
    public VideoAdPlayer getVideoAdPlayer() {
        return mVideoAdPlayer;
    }

    /**
     * Returns if an ad is displayed.
     */
    public boolean getIsAdDisplayed() {
        return mIsAdDisplayed;
    }

    public ContentProgressProvider getContentProgressProvider() {
        return mContentProgressProvider;
    }

    public void setAdCuePoints(AdCuePoints adCuePoints) {
        this.adCuePoints = adCuePoints;
    }

    private void initializePlayer(Uri currentSourceUri) {

        if (player == null) {
            player = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector);
            mVideoPlayer.setPlayer(player);
        }


        MediaSource mediaSource =  buildMediaSource(currentSourceUri, null, mainHandler, eventLogger);
        mVideoPlayer.getPlayer().stop();
        player.prepare(mediaSource);
        mVideoPlayer.getPlayer().setPlayWhenReady(adShouldPAutolay);
    }

    private MediaSource buildMediaSource(
            Uri uri,
            String overrideExtension,
            @Nullable Handler handler,
            @Nullable MediaSourceEventListener listener) {
        @C.ContentType int type = TextUtils.isEmpty(overrideExtension) ? Util.inferContentType(uri)
                : Util.inferContentType("." + overrideExtension);
        switch (type) {
            case C.TYPE_DASH:
                return new DashMediaSource.Factory(
                        new DefaultDashChunkSource.Factory(mediaDataSourceFactory),
                        buildDataSourceFactory(false))
                        .createMediaSource(uri, handler, listener);
            case C.TYPE_SS:
                return new SsMediaSource.Factory(
                        new DefaultSsChunkSource.Factory(mediaDataSourceFactory),
                        buildDataSourceFactory(false))
                        .createMediaSource(uri, handler, listener);
            case C.TYPE_HLS:
                return new HlsMediaSource.Factory(mediaDataSourceFactory)
                        .createMediaSource(uri, handler, listener);
            case C.TYPE_OTHER:
                return new ExtractorMediaSource.Factory(mediaDataSourceFactory)
                        .createMediaSource(uri, handler, listener);
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }

    public void setIsAppInBackground(boolean isAppInBackground) {
        adShouldPAutolay = !isAppInBackground;
    }

    public void resumeContentAfterAdPlayback() {
        mIsAdDisplayed = false;
        isPlayerReady = false;
    }

    private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
        return new DefaultDataSourceFactory(getContext(), useBandwidthMeter ? BANDWIDTH_METER : null,
                buildHttpDataSourceFactory(useBandwidthMeter));
    }

    private HttpDataSource.Factory buildHttpDataSourceFactory(boolean useBandwidthMeter) {
        return new DefaultHttpDataSourceFactory(Util.getUserAgent(getContext(), "AdPlayKit"), useBandwidthMeter ? BANDWIDTH_METER : null,
                adLoadTimeout,
                adLoadTimeout, true);
    }

}