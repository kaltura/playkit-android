package com.kaltura.playkit.plugins.ads.ima;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.ViewGroup;

import com.google.ads.interactivemedia.v3.api.player.ContentProgressProvider;
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.drm.StreamingDrmSessionManager;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.metadata.MetadataRenderer;
import com.google.android.exoplayer2.metadata.id3.Id3Frame;
import com.google.android.exoplayer2.source.AdaptiveMediaSourceEventListener;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelections;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoRendererEventListener;
import com.google.gson.JsonObject;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKAdInfo;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.plugins.ads.AdsConfig;
import com.kaltura.playkit.plugins.ads.AdsProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by gilad.nadav on 17/11/2016.
 */

public class IMAPlugin extends PKPlugin implements AdsProvider, ExoPlayer.EventListener, TrackSelector.EventListener<MappingTrackSelector.MappedTrackInfo> {

    private static final String TAG = "IMAPlugin";

    public interface OnContentCompleteListener {
        public void onContentComplete();
    }

    /////////////////////
    private Player player;
    private Context context;
    private int delay;

    //////////////////////
    private SimpleExoPlayerView mVideoPlayer;
    private boolean mIsAdDisplayed;
    private VideoAdPlayer mVideoAdPlayer;
    private OnContentCompleteListener mOnContentCompleteListener;
    private ViewGroup mAdUiContainer;
    private ContentProgressProvider mContentProgressProvider;
    private final List<VideoAdPlayer.VideoAdPlayerCallback> mAdCallbacks =
            new ArrayList<VideoAdPlayer.VideoAdPlayerCallback>(1);

    private long mSavedAdPosition;
    private long mSavedContentPosition;
    private DataSource.Factory mediaDataSourceFactory;
    private android.os.Handler mainHandler = new Handler();
    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    /////////////////////


   // @Override
   // protected PlayerDecorator getPlayerDecorator() {
   //     return new AdEnabledPlayerController(this);
   // }

    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "IMAPlugin";
        }

        @Override
        public PKPlugin newInstance() {
            return new IMAPlugin();
        }
    };

    ////////PKPlugin

    @Override
    protected void onLoad(Player player, PlayerConfig.Media mediaConfig, JsonObject pluginConfig, final MessageBus messageBus, Context context) {
        this.player = player;
        this.context = context;
        delay = pluginConfig.getAsJsonPrimitive("delay").getAsInt();

//        try {
//            String adTagUrl = pluginConfig.getString("adTagUrl");
//            String videoMimeType = pluginConfig.getString("videoMimeType");
//
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {

            }
        }, PlayerEvent.ENDED);

        messageBus.listen(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                Log.d(TAG, "PlayerEvent:" + event);


            }
        });
    }

    @Override
    protected void onUpdateMedia(PlayerConfig.Media mediaConfig) {

    }

    @Override
    protected void onUpdateConfig(String key, Object value) {

    }

    @Override
    public void onDestroy() {

    }



    ///////////END PKPlugin



    ////////Ads Plugin

    @Override
    public String getPluginName() {
        return IMAPlugin.factory.getName();
    }

    @Override
    public AdsConfig getAdsConfig() {
        return null;
    }

    @Override
    public void requestAd() {

    }

    @Override
    public boolean start(boolean showLoadingView) {
        return false;
    }

    @Override
    public void resume() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void contentCompleted() {

    }

    @Override
    public PKAdInfo getAdInfo() {
        return null;
    }

    @Override
    public boolean isAdDisplayed() {
        return false;
    }

    @Override
    public boolean isAdPaused() {
        return false;
    }

    @Override
    public boolean isAdRequested() {
        return false;
    }

    ///////////END Ads Plugin


    //////////ExoPlayer.EventListener

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if (playbackState == ExoPlayer.STATE_READY && playWhenReady == true) {
            if (mIsAdDisplayed) {
                for (VideoAdPlayer.VideoAdPlayerCallback callback : mAdCallbacks) {
                    callback.onPlay();
                }
            }
        }

        if (playbackState == ExoPlayer.STATE_READY && playWhenReady == false) {
            if (mIsAdDisplayed) {
                for (VideoAdPlayer.VideoAdPlayerCallback callback : mAdCallbacks) {
                    callback.onPause();
                }
            }
        }

        if (playbackState == ExoPlayer.STATE_ENDED) {
            if (mIsAdDisplayed) {
                for (VideoAdPlayer.VideoAdPlayerCallback callback : mAdCallbacks) {
                    callback.onEnded();
                }
            } else {
                // Alert an external listener that our content video is complete.
                if (mOnContentCompleteListener != null) {
                    mOnContentCompleteListener.onContentComplete();
                }
            }
        }
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {

    }

    @Override
    public void onPositionDiscontinuity() {

    }

    //////////END ExoPlayer.EventListener

    ///////////TrackSelector.EventListener
    @Override
    public void onTrackSelectionsChanged(TrackSelections<? extends MappingTrackSelector.MappedTrackInfo> trackSelections) {

    }

    ////////////END TrackSelector.EventListener


    public void setOnContentCompleteListener(OnContentCompleteListener listener) {
        mOnContentCompleteListener = listener;
    }

    public void pauseContentForAdPlayback() {
        //mVideoPlayer.getPlayer().disablePlaybackControls();
        //savePosition();
       // mVideoPlayer.getPlayer().stop();
    }

    public void resumeContentAfterAdPlayback() {
//        if (mContentVideoUrl == null || mContentVideoUrl.isEmpty()) {
//            Log.w("ImaExample", "No content URL specified.");
//            return;
//        }
//        mIsAdDisplayed = false;
//        mVideoPlayer.setVideoPath(mContentVideoUrl);
//        mVideoPlayer.getPlayer().enablePlaybackControls();
//        mVideoPlayer.seekTo(mSavedContentPosition);
//        mVideoPlayer.play();
//        mIsAdDisplayed = false;
//        mVideoPlayer.getPlayer().prepare(buildMediaSource(Uri.parse(mContentVideoUrl), "mp4"));
//        restorePosition();
//        play();
    }

    public void setContentVideoPath(String contentVideoUrl) {
        //mContentVideoUrl = contentVideoUrl;
    }

    public void savePosition() {
//        if (mIsAdDisplayed) {
//            mSavedAdPosition = mVideoPlayer.getPlayer().getCurrentPosition();
//        } else {
//            mSavedContentPosition = mVideoPlayer.getPlayer().getCurrentPosition();
//        }
    }

    public void restorePosition() {
//        if (mIsAdDisplayed) {
//            mVideoPlayer.getPlayer().seekTo(mSavedAdPosition);
//        } else {
//            mVideoPlayer.getPlayer().seekTo(mSavedContentPosition);
//        }
    }

    public boolean getIsAdDisplayed() {
        return mIsAdDisplayed;
    }

    /**
     * Plays the content video.
     */
    public void play() {
        //mVideoPlayer.getPlayer().setPlayWhenReady(true);
    }

    /**
     * Seeks the content video.
     */
    public void seek(int time) {
        //if (mIsAdDisplayed) {
        //    // When ad is playing, set the content video position to seek to when ad finishes.
        //    mSavedContentPosition = time;
        //} else {
        //    mVideoPlayer.getPlayer().seekTo(time);
        //}
    }

    public long getCurrentContentTime() {
 //       if (mIsAdDisplayed) {
 //           return mSavedContentPosition;
 //       } else {
 //           return mVideoPlayer.getPlayer().getCurrentPosition();
 //       }
        return 0;
    }

    public VideoAdPlayer getVideoAdPlayer() {
        return mVideoAdPlayer;
    }

    public ViewGroup getAdUiContainer() {
        return mAdUiContainer;
    }

    public ContentProgressProvider getContentProgressProvider() {
        return mContentProgressProvider;
    }

    private void init() {
        mIsAdDisplayed = false;
        mSavedAdPosition = 0;
        mSavedContentPosition = 0;

        // Define VideoAdPlayer connector.
        mVideoAdPlayer = new VideoAdPlayer() {
            @Override
            public void playAd() {
                mIsAdDisplayed = true;
                mVideoPlayer.getPlayer().setPlayWhenReady(true);
            }

            @Override
            public void loadAd(String url) {
                mIsAdDisplayed = true;
                //mVideoPlayer.getPlayer().prepare(buildMediaSource(Uri.parse(mContentVideoUrl), "mp4"));
                initializePlayer(Uri.parse(url));
            }

            @Override
            public void stopAd() {
                mVideoPlayer.getPlayer().stop();
            }

            @Override
            public void pauseAd() {
                mVideoPlayer.getPlayer().setPlayWhenReady(false);
            }

            @Override
            public void resumeAd() {
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
                if (!mIsAdDisplayed || player.getDuration() <= 0) {
                    return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
                }
                return new VideoProgressUpdate(player.getCurrentPosition(),
                        player.getDuration());
            }
        };

        mContentProgressProvider = new ContentProgressProvider() {
            @Override
            public VideoProgressUpdate getContentProgress() {
                if (mIsAdDisplayed || player.getDuration() <= 0) {
                    return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
                }
                return new VideoProgressUpdate(player.getCurrentPosition(),
                        player.getDuration());
            }
        };

//TODO        player.addListener(this);
    }

    private void initializePlayer(Uri currentSourceUri) {
//TODO        mVideoPlayer.setPlayer(player);

        mVideoPlayer.getPlayer().setPlayWhenReady(true); //TODO

        MediaSource mediaSource = buildMediaSource(currentSourceUri, null);

        mVideoPlayer.getPlayer().prepare(mediaSource);
    }

    private com.google.android.exoplayer2.source.MediaSource buildMediaSource(Uri uri, String overrideExtension) {
        mediaDataSourceFactory = buildDataSourceFactory(true);
        int type = Util.inferContentType(!TextUtils.isEmpty(overrideExtension) ? "." + overrideExtension
                : uri.getLastPathSegment());
        switch (type) {
            case C.TYPE_SS:
                return new SsMediaSource(uri, buildDataSourceFactory(false),
                        new DefaultSsChunkSource.Factory(mediaDataSourceFactory), mainHandler, mEventLogger);
            case C.TYPE_DASH:
                return new DashMediaSource(uri, buildDataSourceFactory(false),
                        new DefaultDashChunkSource.Factory(mediaDataSourceFactory), mainHandler, mEventLogger);
            case C.TYPE_HLS:
                return new HlsMediaSource(uri, mediaDataSourceFactory, mainHandler, mEventLogger);
            case C.TYPE_OTHER:
                return new ExtractorMediaSource(uri, mediaDataSourceFactory, new DefaultExtractorsFactory(),
                        mainHandler, mEventLogger);
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }

    private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
        return new DefaultDataSourceFactory(context, useBandwidthMeter ? BANDWIDTH_METER : null,
                buildHttpDataSourceFactory(useBandwidthMeter));
    }

    private HttpDataSource.Factory buildHttpDataSourceFactory(boolean useBandwidthMeter) {
        return new DefaultHttpDataSourceFactory(Util.getUserAgent(context, "AdPlayKit"), useBandwidthMeter ? BANDWIDTH_METER : null);
    }

    private EventLogger mEventLogger = new EventLogger();
    private static class EventLogger implements TrackSelector.EventListener<MappingTrackSelector.MappedTrackInfo>, ExoPlayer.EventListener, AudioRendererEventListener, VideoRendererEventListener, MetadataRenderer.Output<List<Id3Frame>>, AdaptiveMediaSourceEventListener, ExtractorMediaSource.EventListener, StreamingDrmSessionManager.EventListener {

        @Override
        public void onTrackSelectionsChanged(TrackSelections<? extends MappingTrackSelector.MappedTrackInfo> trackSelections) {

        }

        @Override
        public void onLoadingChanged(boolean isLoading) {

        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

        }

        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest) {

        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {

        }

        @Override
        public void onPositionDiscontinuity() {

        }

        @Override
        public void onAudioEnabled(DecoderCounters counters) {

        }

        @Override
        public void onAudioSessionId(int audioSessionId) {

        }

        @Override
        public void onAudioDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {

        }

        @Override
        public void onAudioInputFormatChanged(Format format) {

        }

        @Override
        public void onAudioTrackUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {

        }

        @Override
        public void onAudioDisabled(DecoderCounters counters) {

        }

        @Override
        public void onVideoEnabled(DecoderCounters counters) {

        }

        @Override
        public void onVideoDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {

        }

        @Override
        public void onVideoInputFormatChanged(Format format) {

        }

        @Override
        public void onDroppedFrames(int count, long elapsedMs) {

        }

        @Override
        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {

        }

        @Override
        public void onRenderedFirstFrame(Surface surface) {

        }

        @Override
        public void onVideoDisabled(DecoderCounters counters) {

        }

        @Override
        public void onMetadata(List<Id3Frame> metadata) {

        }

        @Override
        public void onLoadStarted(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs) {

        }

        @Override
        public void onLoadCompleted(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded) {

        }

        @Override
        public void onLoadCanceled(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded) {

        }

        @Override
        public void onLoadError(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded, IOException error, boolean wasCanceled) {

        }

        @Override
        public void onUpstreamDiscarded(int trackType, long mediaStartTimeMs, long mediaEndTimeMs) {

        }

        @Override
        public void onDownstreamFormatChanged(int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaTimeMs) {

        }

        @Override
        public void onLoadError(IOException error) {

        }

        @Override
        public void onDrmKeysLoaded() {

        }

        @Override
        public void onDrmSessionManagerError(Exception e) {

        }
    }


}
