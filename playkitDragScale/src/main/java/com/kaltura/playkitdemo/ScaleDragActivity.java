package com.kaltura.playkitdemo;

import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.kaltura.netkit.connect.response.ResultElement;
import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.PKPluginConfigs;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.mediaproviders.base.OnMediaLoadCompletion;
import com.kaltura.playkit.mediaproviders.mock.MockMediaProvider;
import com.kaltura.playkit.plugins.SamplePlugin;
import com.kaltura.playkit.plugins.ads.AdCuePoints;
import com.kaltura.playkit.plugins.ads.AdEvent;
import com.kaltura.playkit.plugins.ads.ima.IMAConfig;
import com.kaltura.playkit.plugins.ads.ima.IMAPlugin;
import com.kaltura.playkitdemo.animation.FollowBehavior;
import com.kaltura.playkitdemo.animation.ScaleDragBehavior;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by glebgleb on 7/13/17.
 */

public class ScaleDragActivity extends AppCompatActivity {

    private CoordinatorLayout root;
    private Button show;
    private Button reset;

    private View media;
    private View description;
    private Button wipe;
    private static final PKLog log = PKLog.get("Activity");

    private Player player;
    private MediaEntryProvider mediaProvider;
    private PlaybackControlsView controlsView;
    private boolean nowPlaying;
    ProgressBar progressBar;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scale_drag_activity);
        //show = (Button)findViewById(R.id.show);
        root = (CoordinatorLayout)findViewById(R.id.root);
        //reset = (Button)findViewById(R.id.reset);
        media = /*(RelativeLayout)*/ findViewById(R.id.media);
        media.post(new Runnable() {
            @Override
            public void run() {
                ScaleDragBehavior.fromView(media).setListener(scaleDragListener);
            }
        });
       /* media.post(new Runnable() {
            @Override
            public void run() {
                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams)media.getLayoutParams();
                ScaleDragBehavior behavior = new ScaleDragBehavior(ScaleDragActivity.this, params.get);
                behavior.setShrinkRate(0.4f);
                behavior.setState(ScaleDragBehavior.State.STATE_SHRINK);

                params.setBehavior(behavior);
                media.requestLayout();
            }
        });*/
        /*description = findViewById(R.id.scroll);
        *//*description.post(new Runnable() {
            @Override
            public void run() {
                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams)description.getLayoutParams();
                FollowBehavior behavior = new FollowBehavior(ScaleDragActivity.this, null);
                behavior.setShrinkRate(0.4f);
                behavior.setMediaHeight(200);
                params.setBehavior(behavior);
                description.requestLayout();
            }
        });*//*
        show.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wipe = (Button)description.findViewById(R.id.wipe);
                wipe.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ScaleDragBehavior behavior = ScaleDragBehavior.fromView(media);
                        behavior.setListener(ScaleDragActivity.this);
                        behavior.updateState(ScaleDragBehavior.State.STATE_SHRINK);
                    }
                });
            }
        });

        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ScaleDragBehavior behavior = ScaleDragBehavior.fromView(media);
                behavior.updateState(ScaleDragBehavior.State.STATE_EXPANDED);
            }
        });*/
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);
        registerPlugins();

        /*OnMediaLoadCompletion playLoadedEntry = new OnMediaLoadCompletion() {
            @Override
            public void onComplete(final ResultElement<PKMediaEntry> response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (response.isSuccess()) {
                            onMediaLoaded(response.getResponse());
                        } else {
                            Toast.makeText(ScaleDragActivity.this, "failed to fetch media data: " + (response.getError() != null ? response.getError().getMessage() : ""), Toast.LENGTH_LONG).show();
                            log.e("failed to fetch media data: " + (response.getError() != null ? response.getError().getMessage() : ""));
                        }
                    }
                });
            }
        };*/

        //startMockMediaLoading(playLoadedEntry);
        onMediaLoaded(createMediaEntry());
    }

    private PKMediaEntry createMediaEntry() {
        PKMediaEntry entry = new PKMediaEntry();
        PKMediaSource source = new PKMediaSource();
        source.setId("0_uka1msg4");
        source.setUrl("http://api-preprod.ott.kaltura.com/v4_2/api_v3/service/assetFile/action/playManifest/partnerId/198/assetId/259295/assetType/media/assetFileId/516109/contextType/PLAYBACK/a.m3u8");
        source.setMediaFormat(PKMediaFormat.hls);
        entry.setId("0_uka1msg4");
        entry.setDuration(102000);
        entry.setMediaType(PKMediaEntry.MediaEntryType.Unknown);
        ArrayList<PKMediaSource> sourceList = new ArrayList<>();
        sourceList.add(source);
        entry.setSources(sourceList);
        return entry;
    }

    private void startMockMediaLoading(OnMediaLoadCompletion completion) {
        mediaProvider = new MockMediaProvider("mock/entries.playkit.json", getApplicationContext(), "hls");
        mediaProvider.load(completion);
    }

    private void onMediaLoaded(PKMediaEntry mediaEntry) {
        PKMediaConfig mediaConfig = new PKMediaConfig().setMediaEntry(mediaEntry).setStartPosition(0);
        PKPluginConfigs pluginConfig = new PKPluginConfigs();
        if (player == null) {

            configurePlugins(pluginConfig);

            player = PlayKitManager.loadPlayer(this, pluginConfig);

            log.d("Player: " + player.getClass());
            addPlayerListeners(progressBar);
            //media.addView(player.getView(), 0);

            controlsView = (PlaybackControlsView) this.findViewById(R.id.playerControls);
            controlsView.setPlayer(player);
        }
        player.prepare(mediaConfig);
        //player.play();
    }

    private void configurePlugins(PKPluginConfigs config) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("delay", 1200);
        config.setPluginConfig("Sample", jsonObject);
    }

    private void registerPlugins() {
        PlayKitManager.registerPlugins(this, SamplePlugin.factory);
        PlayKitManager.registerPlugins(this, IMAPlugin.factory);
    }

    private void addPlayerListeners(final ProgressBar appProgressBar) {
        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                log.d("AD_CONTENT_PAUSE_REQUESTED");
                appProgressBar.setVisibility(View.VISIBLE);
            }
        }, AdEvent.Type.CONTENT_PAUSE_REQUESTED);
        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                AdEvent.AdCuePointsUpdateEvent cuePointsList = (AdEvent.AdCuePointsUpdateEvent) event;
                AdCuePoints adCuePoints = cuePointsList.cuePoints;
                if (adCuePoints != null) {
                    log.d("Has Postroll = " + adCuePoints.hasPostRoll());
                }
            }
        }, AdEvent.Type.CUEPOINTS_CHANGED);
        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                log.d("AD_STARTED");
                appProgressBar.setVisibility(View.INVISIBLE);
            }
        }, AdEvent.Type.STARTED);
        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                log.d("Ad Event AD_RESUMED");
                nowPlaying = true;
                appProgressBar.setVisibility(View.INVISIBLE);
            }
        }, AdEvent.Type.RESUMED);
        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                log.d("Ad Event AD_ALL_ADS_COMPLETED");
                appProgressBar.setVisibility(View.INVISIBLE);
            }
        }, AdEvent.Type.ALL_ADS_COMPLETED);
        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                nowPlaying = true;
            }
        }, PlayerEvent.Type.PLAY);

        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                nowPlaying = false;
            }
        }, PlayerEvent.Type.PAUSE);

        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                nowPlaying = true;
            }
        }, AdEvent.Type.SKIPPED);

        player.addStateChangeListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                if (event instanceof PlayerEvent.StateChanged) {
                    PlayerEvent.StateChanged stateChanged = (PlayerEvent.StateChanged) event;
                    log.d("State changed from " + stateChanged.oldState + " to " + stateChanged.newState);

                    if(controlsView != null){
                        controlsView.setPlayerState(stateChanged.newState);
                    }
                }
            }
        });
        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                //When the track data available, this event occurs. It brings the info object with it.
                PlayerEvent.TracksAvailable tracksAvailable = (PlayerEvent.TracksAvailable) event;

            }
        }, PlayerEvent.Type.TRACKS_AVAILABLE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (controlsView != null) {
            controlsView.release();
        }
        if (player != null) {
            player.onApplicationPaused();
        }
    }

    @Override
    protected void onResume() {
        log.d("Ad Event onResume");
        super.onResume();
        if (player != null) {
            player.onApplicationResumed();
            if (nowPlaying) {
                player.play();
            }
        }
        if (controlsView != null) {
            controlsView.resume();
        }
    }

    private ScaleDragBehavior.OnBehaviorStateListener scaleDragListener = new ScaleDragBehavior.OnBehaviorStateListener() {

        private int w;
        private int h;

        @Override
        public void onBehaviorStateChanged(ScaleDragBehavior.State state) {
            if (state == ScaleDragBehavior.State.STATE_TO_RIGHT || state == ScaleDragBehavior.State.STATE_TO_LEFT) {
                root.removeView(media);
                root.removeView(description);
            }
        }
    };


}
