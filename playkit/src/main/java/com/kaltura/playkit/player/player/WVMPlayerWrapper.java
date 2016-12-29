package com.kaltura.playkit.player.player;//package com.kaltura.playkit.player;
//
//import android.content.Context;
//import android.content.pm.PackageInfo;
//import android.content.pm.PackageManager;
//import android.media.MediaPlayer;
//import android.media.session.PlaybackState;
//import android.net.Uri;
//import android.os.Build;
//import android.os.Handler;
//import android.text.TextUtils;
//import android.util.AttributeSet;
//import android.view.View;
//import android.widget.MediaController;
//import android.widget.VideoView;
//
//import com.google.android.exoplayer2.C;
//import com.google.android.exoplayer2.DefaultLoadControl;
//import com.google.android.exoplayer2.ExoPlayerFactory;
//import com.google.android.exoplayer2.ExoPlayerLibraryInfo;
//import com.google.android.exoplayer2.Timeline;
//import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
//import com.google.android.exoplayer2.source.ExtractorMediaSource;
//import com.google.android.exoplayer2.source.MediaSource;
//import com.google.android.exoplayer2.source.dash.DashMediaSource;
//import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
//import com.google.android.exoplayer2.source.hls.HlsMediaSource;
//import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
//import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
//import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
//import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
//import com.google.android.exoplayer2.trackselection.TrackSelection;
//import com.google.android.exoplayer2.upstream.DataSource.Factory;
//import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
//import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
//import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
//import com.google.android.exoplayer2.upstream.HttpDataSource;
//import com.google.android.exoplayer2.util.Util;
//import com.kaltura.playkit.BuildConfig;
//import com.kaltura.playkit.PKLog;
//import com.kaltura.playkit.PKMediaSource;
//import com.kaltura.playkit.PKTracks;
//import com.kaltura.playkit.PlaybackParamsInfo;
//import com.kaltura.playkit.PlayerEvent;
//import com.kaltura.playkit.PlayerState;
//import com.kaltura.playkit.drm.DeferredDrmSessionManager;
//import com.kaltura.playkit.player.PlayerController.EventListener;
//import com.kaltura.playkit.player.PlayerController.StateChangedListener;
//import com.kaltura.playkit.utils.Consts;
//import com.kaltura.playkit.utils.EventLogger;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import static com.google.ads.interactivemedia.v3.b.b.a.n.D;
//import static com.kaltura.playkit.PlayerState.IDLE;
//
//
//class WVMPlayerWrapper extends VideoView implements PlayerEngine {
//
//    private static final PKLog log = PKLog.get("WVMPlayerWrapper");
//
//    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
//    private EventLogger eventLogger;
//    private EventListener eventListener;
//    private StateChangedListener stateChangedListener;
//
//    private Context context;
//    private CustomExoPlayerView exoPlayerView;
//
//
//    private DeferredDrmSessionManager drmSessionManager;
//
//    private PlayerEvent.Type currentEvent;
//    private PlayerState currentState = IDLE, previousState;
//
//    private Factory mediaDataSourceFactory;
//    private Handler mainHandler = new Handler();
//
//    private boolean isSeeking = false;
//
//    private int playerWindow;
//    private long playerPosition;
//    private Uri lastPlayedSource;
//    private Timeline.Window window;
//    private boolean shouldGetTracksInfo;
//    private boolean shouldResetPlayerPosition;
//    private long prevDuration = Consts.TIME_UNSET;
//
//
//    WVMPlayerWrapper(Context context, AttributeSet attrs, int defStyle) {
//        super(context, attrs, defStyle);
//        this.context = context;
//        mediaDataSourceFactory = buildDataSourceFactory(true);
//        exoPlayerView = new CustomExoPlayerView(context);
//        window = new Timeline.Window();
//    }
//
//
//    private void init() {
//        currentState = IDLE;
//
//
//        // Set OnCompletionListener to notify our callbacks when the video is completed.
//        super.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//
//            @Override
//            public void onCompletion(MediaPlayer mediaPlayer) {
//                // Reset the MediaPlayer.
//                // This prevents a race condition which occasionally results in the media
//                // player crashing when switching between videos.
//
//                mediaPlayer.reset();
//                mediaPlayer.setDisplay(getHolder());
//                currentState = IDLE;
//
//            }
//        });
//
//        // Set OnErrorListener to notify our callbacks if the video errors.
//        super.setOnErrorListener(new MediaPlayer.OnErrorListener() {
//
//            @Override
//            public boolean onError(MediaPlayer mp, int what, int extra) {
//                 currentState = IDLE;
//
//
//                // Returning true signals to MediaPlayer that we handled the error. This will
//                // prevent the completion handler from being called.
//                return true;
//            }
//        });
//    }
//
//
//
//     /**
//     * Returns a new DataSource factory.
//     *
//     * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
//     *                          DataSource factory.
//     * @return A new DataSource factory.
//     */
//    private Factory buildDataSourceFactory(boolean useBandwidthMeter) {
//        return new DefaultDataSourceFactory(context, useBandwidthMeter ? BANDWIDTH_METER : null,
//                buildHttpDataSourceFactory(useBandwidthMeter));
//    }
//
//    /**
//     * Returns a new HttpDataSource factory.
//     *
//     * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
//     *                          DataSource factory.
//     * @return A new HttpDataSource factory.
//     */
//    private HttpDataSource.Factory buildHttpDataSourceFactory(boolean useBandwidthMeter) {
//        return new DefaultHttpDataSourceFactory(getUserAgent(context), useBandwidthMeter ? BANDWIDTH_METER : null);
//    }
//
//
//    private static String getUserAgent(Context context) {
//        String applicationName;
//        try {
//            String packageName = context.getPackageName();
//            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
//            applicationName = packageName + "/" + info.versionName;
//        } catch (PackageManager.NameNotFoundException e) {
//            applicationName = "?";
//        }
//
//        String sdkName = "PlayKit/" + BuildConfig.VERSION_NAME;
//
//        return sdkName + " " + applicationName + " (Linux;Android " + Build.VERSION.RELEASE
//                + ") " + "ExoPlayerLib/" + ExoPlayerLibraryInfo.VERSION;
//    }
//
//
//    @Override
//    public void load(PKMediaSource mediaSource) {
//
//    }
//
//    @Override
//    public View getView() {
//        return this;
//    }
//
//    @Override
//    public void play() {
//        super.start();
//    }
//
//    @Override
//    public void replay() {
//        super.seekTo(0);
//        play();
//    }
//
//    @Override
//    public long getBufferedPosition() {
//        return 0;
//    }
//
//    @Override
//    public float getVolume() {
//        return 0;
//    }
//
//    @Override
//    public PKTracks getPKTracks() {
//        return null;
//    }
//
//    @Override
//    public void changeTrack(String uniqueId) {
//
//    }
//
//    @Override
//    public void seekTo(long position) {
//        super.seekTo((int) position);
//    }
//
//    @Override
//    public void startFrom(long position) {
//        super.seekTo((int) position);
//    }
//
//    @Override
//    public void setVolume(float volume) {
//
//    }
//
//    @Override
//    public void setEventListener(EventListener eventTrigger) {
//
//    }
//
//    @Override
//    public void setStateChangedListener(StateChangedListener stateChangedTrigger) {
//
//    }
//
//    @Override
//    public void release() {
//
//    }
//
//    @Override
//    public void restore() {
//
//    }
//
//    @Override
//    public void destroy() {
//
//    }
//
//    @Override
//    public PlaybackParamsInfo getPlaybackParamsInfo() {
//        return null;
//    }
//
//    private void changeState(PlayerState newState) {
//        previousState = currentState;
//        if (newState.equals(currentState)) {
//            return;
//        }
//        this.currentState = newState;
//        if (stateChangedListener != null) {
//            stateChangedListener.onStateChanged(previousState, currentState);
//        }
//    }
//
//    private void sendDistinctEvent(PlayerEvent.Type newEvent) {
//        if (newEvent.equals(currentEvent)) {
//            return;
//        }
//
//        sendEvent(newEvent);
//    }
//
//    private void sendEvent(PlayerEvent.Type event) {
//        currentEvent = event;
//        if (eventListener != null) {
//            eventListener.onEvent(currentEvent);
//        }
//    }
//
//}
//
//
