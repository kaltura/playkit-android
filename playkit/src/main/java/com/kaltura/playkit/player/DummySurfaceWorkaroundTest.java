package com.kaltura.playkit.player;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.google.android.kexoplayer2.DefaultRenderersFactory;
import com.google.android.kexoplayer2.ExoPlaybackException;
import com.google.android.kexoplayer2.ExoPlayerFactory;
import com.google.android.kexoplayer2.ExoPlayerLibraryInfo;
import com.google.android.kexoplayer2.Player;
import com.google.android.kexoplayer2.SimpleExoPlayer;
import com.google.android.kexoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.kexoplayer2.drm.ExoMediaDrm;
import com.google.android.kexoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.kexoplayer2.drm.MediaDrmCallback;
import com.google.android.kexoplayer2.drm.UnsupportedDrmException;
import com.google.android.kexoplayer2.mediacodec.MediaCodecRenderer;
import com.google.android.kexoplayer2.source.MediaSource;
import com.google.android.kexoplayer2.source.dash.DashMediaSource;
import com.google.android.kexoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.kexoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.kexoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.kexoplayer2.upstream.DataSource;
import com.google.android.kexoplayer2.upstream.DefaultDataSourceFactory;
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

        DefaultDrmSessionManager<FrameworkMediaCrypto> drmSessionManager = getDrmSessionManager();

        if (drmSessionManager == null) {
            return;
        }

        DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(context);
        final SimpleExoPlayer player = ExoPlayerFactory.newSimpleInstance(context, renderersFactory, trackSelector, drmSessionManager);

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

    private static DefaultDrmSessionManager<FrameworkMediaCrypto> getDrmSessionManager() {
        try {
            return DefaultDrmSessionManager.newWidevineInstance(fakeDrmCallback, null);
        } catch (UnsupportedDrmException e) {
            e.printStackTrace();
            return null;
        }
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
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
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
            }
        });
    }
}

