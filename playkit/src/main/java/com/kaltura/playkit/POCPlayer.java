package com.kaltura.playkit;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.View;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.StreamingDrmSessionManager;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
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
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Noam Tamim @ Kaltura on 13/10/2016.
 */




class POCPlayer implements Player, TrackSelector.EventListener<MappingTrackSelector.MappedTrackInfo>, ExoPlayer.EventListener {

    public static final UUID WIDEVINE_UUID = new UUID(0xEDEF8BA979D64ACEL, 0xA3C827DCD51D21EDL);
    private static final com.google.android.exoplayer2.upstream.DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    private final Context mContext;

    private DefaultTrackSelector mTrackSelector;
    private android.os.Handler mainHandler = new Handler();
    private SimpleExoPlayer player;
    private SimpleExoPlayerView simpleExoPlayerView;
    private boolean shouldAutoPlay;
    private boolean playerNeedsSource;
    private Uri mCurrentSourceUri;
    private DataSource.Factory mediaDataSourceFactory;


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
    
    private EventLogger mEventLogger = new EventLogger();

    private static class TrackSelectionHelper {

        public TrackSelectionHelper(DefaultTrackSelector trackSelector, TrackSelection.Factory videoTrackSelectionFactory) {
            
        }
    }
    private TrackSelectionHelper mTrackSelectionHelper;
    
    public POCPlayer(Context context) {
        mContext = context;
        simpleExoPlayerView = new SimpleExoPlayerView(context);
        mediaDataSourceFactory = buildDataSourceFactory(true);
    }
    
    @Override
    public void load(@NonNull PlayerConfig playerConfig) {
        mCurrentSourceUri = Uri.parse(playerConfig.getEntry().getSources().get(0).getUrl());
        shouldAutoPlay = playerConfig.shouldAutoPlay();
        initializePlayer();
    }

    @Override
    public void apply(@NonNull PlayerConfig playerConfig) {
        shouldAutoPlay = playerConfig.shouldAutoPlay();
    }

    @Override
    public View getView() {
        return simpleExoPlayerView;
    }

    @Override
    public long getCurrentPosition() {
        return 0;
    }

    @Override
    public void seekTo(long position) {

    }

    @Override
    public boolean getAutoPlay() {
        return shouldAutoPlay;
    }

    @Override
    public void setAutoPlay(boolean autoPlay) {
        shouldAutoPlay = autoPlay;
        player.setPlayWhenReady(autoPlay);
    }

    @Override
    public void play() {
        setAutoPlay(true);
    }

    @Override
    public void pause() {
        setAutoPlay(false);
    }

    @Override
    public void prepareNext(@NonNull PlayerConfig playerConfig) {

    }

    @Override
    public void loadNext() {

    }

    @Override
    public void addEventListener(@NonNull PlayerEvent.Listener listener, @Nullable PlayerEvent... events) {
        Log.e("POC", "events=" + Arrays.toString(events));
    }

    @Override
    public void addStateChangeListener(@NonNull PlayerState.Listener listener) {
        
    }

    @Override
    public void addBoundaryTimeListener(@NonNull TimeListener listener, boolean wait, @NonNull RelativeTime... times) {
        
    }

    @Override
    public void addPeriodicTimeListener(@NonNull TimeListener listener, long interval) {

    }

    // ExoPlayer

    private void initializePlayer() {
        
        if (player == null) {

            TrackSelection.Factory videoTrackSelectionFactory =
                    new AdaptiveVideoTrackSelection.Factory(BANDWIDTH_METER);
            mTrackSelector = new DefaultTrackSelector(mainHandler, videoTrackSelectionFactory);
            mTrackSelector.addListener(this);
            mTrackSelector.addListener(mEventLogger);
            mTrackSelectionHelper = new TrackSelectionHelper(mTrackSelector, videoTrackSelectionFactory);
            player = ExoPlayerFactory.newSimpleInstance(mContext, mTrackSelector, new DefaultLoadControl(),
                    null, false);
            player.addListener(this);
            player.addListener(mEventLogger);
            player.setAudioDebugListener(mEventLogger);
            player.setVideoDebugListener(mEventLogger);
            player.setId3Output(mEventLogger);
            
            
            simpleExoPlayerView.setPlayer(player);

            player.setPlayWhenReady(shouldAutoPlay);
            playerNeedsSource = true;
        }
        if (playerNeedsSource) {

            MediaSource mediaSource = buildMediaSource(mCurrentSourceUri, null);
            
            player.prepare(mediaSource);
            playerNeedsSource = false;
        }
    }

    private com.google.android.exoplayer2.source.MediaSource buildMediaSource(Uri uri, String overrideExtension) {
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

    private DrmSessionManager<FrameworkMediaCrypto> buildDrmSessionManager(UUID uuid,
                                                                           String licenseUrl, Map<String, String> keyRequestProperties) throws UnsupportedDrmException {
        if (Util.SDK_INT < 18) {
            return null;
        }
        HttpMediaDrmCallback drmCallback = new HttpMediaDrmCallback(licenseUrl,
                buildHttpDataSourceFactory(false), keyRequestProperties);
        return new StreamingDrmSessionManager<>(uuid,
                FrameworkMediaDrm.newInstance(uuid), drmCallback, null, mainHandler, mEventLogger);
    }

    private void releasePlayer() {
        if (player != null) {
            shouldAutoPlay = player.getPlayWhenReady();
            player.release();
            player = null;
            mTrackSelector = null;
            mTrackSelectionHelper = null;
            mEventLogger = null;
        }
    }

    /**
     * Returns a new DataSource factory.
     *
     * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
     *     DataSource factory.
     * @return A new DataSource factory.
     */
    private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
        return new DefaultDataSourceFactory(mContext, useBandwidthMeter ? BANDWIDTH_METER : null,
                buildHttpDataSourceFactory(useBandwidthMeter));
    }

    /**
     * Returns a new HttpDataSource factory.
     *
     * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
     *     DataSource factory.
     * @return A new HttpDataSource factory.
     */
    private HttpDataSource.Factory buildHttpDataSourceFactory(boolean useBandwidthMeter) {
        return new DefaultHttpDataSourceFactory(Util.getUserAgent(mContext, "PlayKit"), useBandwidthMeter ? BANDWIDTH_METER : null);
    }

    @Override
    public void onTrackSelectionsChanged(TrackSelections<? extends MappingTrackSelector.MappedTrackInfo> trackSelections) {
        
    }
}
