package com.kaltura.playkit.plugins.ads.ima;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
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
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataRenderer;
import com.google.android.exoplayer2.source.AdaptiveMediaSourceEventListener;
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
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.EventLogger;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoRendererEventListener;
import com.kaltura.playkit.PKError;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.drm.DeferredDrmSessionManager;
import com.kaltura.playkit.player.CustomHttpDataSourceFactory;
import com.kaltura.playkit.player.CustomRendererFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * Video player that can play content video and ads.
 */
public class ExoPlayerWithAdPlayback extends RelativeLayout implements PlaybackPreparer, Player.EventListener {
    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    private DefaultTrackSelector trackSelector;
    private TrackSelectionHelper trackSelectionHelper;
    private TrackGroupArray lastSeenTrackGroupArray;
    private EventLogger eventLogger;
    private android.os.Handler mainHandler = new Handler();
    private DeferredDrmSessionManager drmSessionManager;
    private DefaultRenderersFactory renderersFactory;
    private SimpleExoPlayer player;

    private DataSource.Factory mediaDataSourceFactory;
    private Context mContext;
    private com.kaltura.playkit.Player contentPlayer;

    @Override
    public void preparePlayback() {

    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {
        Log.d("xxx", "onTimelineChanged");
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        Log.d("xxx", "onLoadingChanged");
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
        Log.d("xxx", "onTracksChanged");
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        Log.d("xxx XXXX ", "onPlayerStateChanged " + playbackState);
        switch (playbackState) {
            case Player.STATE_IDLE:
                Log.d("xxx XXXX ","onPlayerStateChanged. IDLE. playWhenReady => " + playWhenReady);

                break;
            case Player.STATE_BUFFERING:
                Log.d("xxx XXXX ","onPlayerStateChanged. BUFFERING. playWhenReady => " + playWhenReady);

                break;
            case Player.STATE_READY:
                Log.d("xxx XXXX ","onPlayerStateChanged. READY. playWhenReady => " + playWhenReady);
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
                        callback.onResume();
                    }
                }
                break;
            case Player.STATE_ENDED:
                Log.d("xxx XXXX ","onPlayerStateChanged. ENDED. playWhenReady => " + playWhenReady);
                if (mIsAdDisplayed) {
                    for (VideoAdPlayer.VideoAdPlayerCallback callback : mAdCallbacks) {
                        callback.onEnded();
                        mIsAdDisplayed = false;
                    }
                } else {
                    // Alert an external listener that our content video is complete.
                    if (mOnContentCompleteListener != null) {
                        mOnContentCompleteListener.onContentComplete();
                    }
                }
                break;
            default:
                break;

        }


    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {
        Log.d("xxx", "onRepeatModeChanged");
    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
        Log.d("xxx", "onShuffleModeEnabledChanged");
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        Log.d("xxx", "onPlayerError " + error.getMessage());
        for (VideoAdPlayer.VideoAdPlayerCallback callback : mAdCallbacks) {
            callback.onError();
        }

    }

    @Override
    public void onPositionDiscontinuity(int reason) {
        Log.d("xxx", "onPositionDiscontinuity");
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
        Log.d("xxx", "onPlaybackParametersChanged");

    }

    @Override
    public void onSeekProcessed() {
        Log.d("xxx", "onSeekProcessed");
    }

    /**
     * Interface for alerting caller of video completion.
     */
    public interface OnContentCompleteListener {
        public void onContentComplete();
    }

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

    // Called when the content is completed.
    private OnContentCompleteListener mOnContentCompleteListener;

    // VideoAdPlayer interface implementation for the SDK to send ad play/pause type events.
    private VideoAdPlayer mVideoAdPlayer;

    // ContentProgressProvider interface implementation for the SDK to check content progress.
    private ContentProgressProvider mContentProgressProvider;

    private final List<VideoAdPlayer.VideoAdPlayerCallback> mAdCallbacks =
            new ArrayList<VideoAdPlayer.VideoAdPlayerCallback>(1);

    public ExoPlayerWithAdPlayback(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mContext = context;
    }

    public ExoPlayerWithAdPlayback(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public ExoPlayerWithAdPlayback(Context context) {
        super(context,null);
        this.mContext = context;
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
            DeferredDrmSessionManager drmSessionManager = new DeferredDrmSessionManager(mainHandler, buildHttpDataSourceFactory(false), initDrmSessionListener());

            renderersFactory = new DefaultRenderersFactory(mContext,
                    drmSessionManager, DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF);

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

        FrameLayout adFrameLayout = new FrameLayout(getContext());
        adFrameLayout.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mAdUiContainer = adFrameLayout;

        // Define VideoAdPlayer connector.
        mVideoAdPlayer = new VideoAdPlayer() {
            @Override
            public void playAd() {
                Log.d("xxx", "playAd");
                mIsAdDisplayed = true;
                mVideoPlayer.getPlayer().setPlayWhenReady(true);
                for (VideoAdPlayer.VideoAdPlayerCallback callback : mAdCallbacks) {
                    callback.onPlay();
                }
            }

            @Override
            public void loadAd(String url) {
                Log.d("xxx", "loadAd");
                mIsAdDisplayed = true;
                //mVideoPlayer.getPlayer().prepare(buildMediaSource(Uri.parse(mContentVideoUrl), "mp4"));
                initializePlayer(Uri.parse(url));
            }

            @Override
            public void stopAd() {
                Log.d("xxx", "stopAd");
                mVideoPlayer.getPlayer().stop();
            }

            @Override
            public void pauseAd() {
                Log.d("xxx", "pauseAd");
                for (VideoAdPlayer.VideoAdPlayerCallback callback : mAdCallbacks) {
                    callback.onPause();
                }
                mVideoPlayer.getPlayer().setPlayWhenReady(false);
            }

            @Override
            public void resumeAd() {
                Log.d("xxx", "resumeAd");
                for (VideoAdPlayer.VideoAdPlayerCallback callback : mAdCallbacks) {
                    callback.onResume();
                }
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
                if (!mIsAdDisplayed || mVideoPlayer.getPlayer().getDuration() <= 0) {
                    return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
                }
                long duration = mVideoPlayer.getPlayer().getDuration();
                long position = mVideoPlayer.getPlayer().getCurrentPosition();
                Log.d("YYY", "getDuration " +  duration);
                Log.d("YYY", "getCurrentPosition " +  position);
                return new VideoProgressUpdate(position, duration);
            }
        };


//        // Set player callbacks for delegating major video events.
//        mVideoPlayer.addPlayerCallback(new VideoPlayer.PlayerCallback() {
//            @Override
//            public void onPlay() {
//                if (mIsAdDisplayed) {
//                    for (VideoAdPlayer.VideoAdPlayerCallback callback : mAdCallbacks) {
//                        callback.onPlay();
//                    }
//                }
//            }
//
//            @Override
//            public void onPause() {
//                if (mIsAdDisplayed) {
//                    for (VideoAdPlayer.VideoAdPlayerCallback callback : mAdCallbacks) {
//                        callback.onPause();
//                    }
//                }
//            }
//
//            @Override
//            public void onResume() {
//                if (mIsAdDisplayed) {
//                    for (VideoAdPlayer.VideoAdPlayerCallback callback : mAdCallbacks) {
//                        callback.onResume();
//                    }
//                }
//            }
//
//            @Override
//            public void onError() {
//                if (mIsAdDisplayed) {
//                    for (VideoAdPlayer.VideoAdPlayerCallback callback : mAdCallbacks) {
//                        callback.onError();
//                    }
//                }
//            }
//
//            @Override
//            public void onCompleted() {
//                if (mIsAdDisplayed) {
//                    for (VideoAdPlayer.VideoAdPlayerCallback callback : mAdCallbacks) {
//                        callback.onEnded();
//                    }
//                } else {
//                    // Alert an external listener that our content video is complete.
//                    if (mOnContentCompleteListener != null) {
//                        mOnContentCompleteListener.onContentComplete();
//                    }
//                }
//            }
//        });
//

        mVideoPlayer.getPlayer().addListener(this);
    }

    public void setContentProgressProvider(final com.kaltura.playkit.Player contentPlayer) {
        this.contentPlayer = contentPlayer;
        mContentProgressProvider = new ContentProgressProvider() {
            @Override
            public VideoProgressUpdate getContentProgress() {

                if (mVideoPlayer.getPlayer().getDuration() <= 0) {
                    return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
                }
                Log.d("XXX", "getDuration " +  contentPlayer.getDuration());
                Log.d("XXX", "getCurrentPosition " +  contentPlayer.getCurrentPosition());
                return new VideoProgressUpdate(contentPlayer.getCurrentPosition(),
                        contentPlayer.getDuration());
            }
        };
    }


    /**
     * Set a listener to be triggered when the content (non-ad) video completes.
     */

    public void setOnContentCompleteListener(OnContentCompleteListener listener) {
        mOnContentCompleteListener = listener;
    }

    /**
     * Set the path of the video to be played as content.
     */
    public void setContentVideoPath(String contentVideoUrl) {
        mContentVideoUrl = contentVideoUrl;
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

    private void initializePlayer(Uri currentSourceUri) {

        if (player == null) {
            player = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector);
            mVideoPlayer.setPlayer(player);
        }

        mVideoPlayer.getPlayer().setPlayWhenReady(true);
        MediaSource mediaSource =  buildMediaSource(currentSourceUri, null, mainHandler, eventLogger);
        mVideoPlayer.getPlayer().stop();
        player.prepare(mediaSource);
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

    public void resumeContentAfterAdPlayback() {
        if (mContentVideoUrl == null || mContentVideoUrl.isEmpty()) {
            Log.w("ImaExample", "No content URL specified.");
            return;
        }
//        mIsAdDisplayed = false;
//        mVideoPlayer.setVideoPath(mContentVideoUrl);
//        mVideoPlayer.enablePlaybackControls();
//        mVideoPlayer.seekTo(mSavedContentPosition);
//        mVideoPlayer.play();
    }

    private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
        return new DefaultDataSourceFactory(getContext(), useBandwidthMeter ? BANDWIDTH_METER : null,
                buildHttpDataSourceFactory(useBandwidthMeter));
    }

    private HttpDataSource.Factory buildHttpDataSourceFactory(boolean useBandwidthMeter) {
        return new DefaultHttpDataSourceFactory(Util.getUserAgent(getContext(), "AdPlayKit"), useBandwidthMeter ? BANDWIDTH_METER : null);
    }

}