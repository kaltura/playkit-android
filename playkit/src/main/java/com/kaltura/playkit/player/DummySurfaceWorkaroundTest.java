package com.kaltura.playkit.player;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.kaltura.android.exoplayer2.DefaultRenderersFactory;
import com.kaltura.android.exoplayer2.ExoPlaybackException;
import com.kaltura.android.exoplayer2.ExoPlayerFactory;
import com.kaltura.android.exoplayer2.ExoPlayerLibraryInfo;
import com.kaltura.android.exoplayer2.Player;
import com.kaltura.android.exoplayer2.SimpleExoPlayer;
import com.kaltura.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.kaltura.android.exoplayer2.drm.ExoMediaCrypto;
import com.kaltura.android.exoplayer2.drm.ExoMediaDrm;
import com.kaltura.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.kaltura.android.exoplayer2.drm.MediaDrmCallback;
import com.kaltura.android.exoplayer2.drm.UnsupportedDrmException;
import com.kaltura.android.exoplayer2.mediacodec.MediaCodecRenderer;
import com.kaltura.android.exoplayer2.source.MediaSource;
import com.kaltura.android.exoplayer2.source.dash.DashMediaSource;
import com.kaltura.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.kaltura.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.kaltura.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.kaltura.android.exoplayer2.upstream.DataSource;
import com.kaltura.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.kaltura.playkit.PKDeviceCapabilities;
import com.kaltura.playkit.PlayKitManager;

import org.json.JSONObject;

import java.util.UUID;

public class DummySurfaceWorkaroundTest {

    private static final String TAG = "DummySurfaceTest";

    private static final String PREFS_ENTRY_FINGERPRINT = "Build.FINGERPRINT.DummySurface";
    private static final String URL = "asset:///DRMTest/index.mpd";

    public static boolean workaroundRequired;
    private static boolean reportSent;

    private static MediaDrmCallback fakeDrmCallback = new MediaDrmCallback() {
        @Override
        public byte[] executeProvisionRequest(UUID uuid, ExoMediaDrm.ProvisionRequest request) throws Exception {
            return null;
        }

        @Override
        public byte[] executeKeyRequest(UUID uuid, ExoMediaDrm.KeyRequest request) throws Exception {
            Thread.sleep(10000);
            return null;
        }
    };

    static void executeTest(final Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return;
        }

        DataSource.Factory mediaDataSourceFactory = new DefaultDataSourceFactory(context, "whatever");
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(new AdaptiveTrackSelection.Factory());

        DefaultDrmSessionManager<ExoMediaCrypto> drmSessionManager = getDrmSessionManager();

        if (drmSessionManager == null) {
            return;
        }

        DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(context);
        final SimpleExoPlayer player = new SimpleExoPlayer.Builder(context, renderersFactory).setTrackSelector(trackSelector).build();

        player.addListener(new Player.EventListener() {
            @Override
            public void onPlayerError(ExoPlaybackException error) {
                if (error.getCause() instanceof MediaCodecRenderer.DecoderInitializationException) {
                    workaroundRequired(context, true);
                }
                player.release();
            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    // If we receive player state ready, we can assume that no workaround required.
                    // So set the workaroundRequired flag to false.
                    workaroundRequired(context, false);
                    player.release();
                }
            }
        });

        MediaSource mediaSource = new DashMediaSource.Factory(
                new DefaultDashChunkSource.Factory(mediaDataSourceFactory),
                mediaDataSourceFactory)
                .createMediaSource(Uri.parse(URL));

        player.prepare(mediaSource);
    }

    private static void workaroundRequired(Context context, boolean b) {
        workaroundRequired = b;
        if (b) {
            maybeSendReport(context);
        }
    }

    private static DefaultDrmSessionManager<ExoMediaCrypto> getDrmSessionManager() {
        return new DefaultDrmSessionManager.Builder().build(fakeDrmCallback);
    }

    private static void maybeSendReport(final Context context) {
        if (reportSent) {
            return;
        }

        final SharedPreferences sharedPrefs = context.getSharedPreferences(PKDeviceCapabilities.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        String savedFingerprint = sharedPrefs.getString(PREFS_ENTRY_FINGERPRINT, null);

        // If we already sent this report for this Android build, don't send again.
        if (Build.FINGERPRINT.equals(savedFingerprint)) {
            reportSent = true;
            return;
        }

        // Do everything else in a thread.
        AsyncTask.execute(() -> {
            String reportString;
            try {
                JSONObject jsonObject = new JSONObject()
                        .put("reportType", "DummySurfaceWorkaround")
                        .put("playkitVersion", PlayKitManager.VERSION_STRING)
                        .put("system", PKDeviceCapabilities.systemInfo())
                        .put("exoPlayerVersion", ExoPlayerLibraryInfo.VERSION)
                        .put("dummySurfaceWorkaroundRequired", true);

                reportString = jsonObject.toString();
            } catch (Exception e) {
                Log.e(TAG, "Failed to get report", e);
                reportString = PKDeviceCapabilities.getErrorReport(e);
            }

            if (!PKDeviceCapabilities.sendReport(reportString))
                return;

            // If we got here, save the fingerprint so we don't send again until the OS updates.
            sharedPrefs.edit().putString(PREFS_ENTRY_FINGERPRINT, Build.FINGERPRINT).apply();
            reportSent = true;
        });
    }
}

