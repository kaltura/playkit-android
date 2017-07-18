package com.kaltura.playkitdemo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.google.gson.JsonObject;
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
import com.kaltura.playkitdemo.animation.ScaleDragBehavior;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by glebgleb on 7/13/17.
 */

public class PlayerFragment extends Fragment {

    private RelativeLayout root;

    private static final PKLog log = PKLog.get("Activity");

    private Player player;
    private MediaEntryProvider mediaProvider;
    private PlaybackControlsView controlsView;
    private boolean nowPlaying;
    ProgressBar progressBar;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.player_fragment, null);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        root = (RelativeLayout)view.findViewById(R.id.root);
        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);
        registerPlugins();
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

    /*private void startMockMediaLoading(OnMediaLoadCompletion completion) {
        mediaProvider = new MockMediaProvider("mock/entries.playkit.json", getActivity(), "hls");
        mediaProvider.load(completion);
    }*/

    private void onMediaLoaded(PKMediaEntry mediaEntry) {
        PKMediaConfig mediaConfig = new PKMediaConfig().setMediaEntry(mediaEntry).setStartPosition(0);
        PKPluginConfigs pluginConfig = new PKPluginConfigs();
        if (player == null) {

            addIMAPluginConfig(pluginConfig);

            player = PlayKitManager.loadPlayer(getActivity(), pluginConfig);

            log.d("Player: " + player.getClass());
            addPlayerListeners(progressBar);
            root.addView(player.getView(), 0);

            controlsView = (PlaybackControlsView) root.findViewById(R.id.playerControls);
            controlsView.setPlayer(player);
        }
        player.prepare(mediaConfig);
        player.play();
    }

    private void addIMAPluginConfig(PKPluginConfigs config) {
        String adTagUrl = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dskippablelinear&correlator=";
        List<String> videoMimeTypes = new ArrayList<>();
        IMAConfig adsConfig = new IMAConfig().setAdTagURL(adTagUrl);
        config.setPluginConfig(IMAPlugin.factory.getName(), adsConfig);
    }

    private void registerPlugins() {
        PlayKitManager.registerPlugins(getActivity(), IMAPlugin.factory);
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
    public void onPause() {
        super.onPause();
        if (controlsView != null) {
            controlsView.release();
        }
        if (player != null) {
            player.onApplicationPaused();
        }
    }

    @Override
    public void onResume() {
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

}
