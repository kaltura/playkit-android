package com.kaltura.playkitdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.exoplayer2.util.MimeTypes;
import com.google.gson.JsonObject;
import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.backend.base.OnMediaLoadCompletion;
import com.kaltura.playkit.backend.mock.MockMediaProvider;
import com.kaltura.playkit.connect.ResultElement;
import com.kaltura.playkit.plugins.SamplePlugin;
import com.kaltura.playkit.plugins.ads.AdsConfig;
import com.kaltura.playkit.plugins.ads.ima.IMAEvents;
import com.kaltura.playkit.plugins.ads.ima.IMASimplePlugin;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    
    
    public static final boolean AUTO_PLAY_ON_RESUME = false;

    private static final String TAG = "MainActivity";

    private Player player;
    private MediaEntryProvider mediaProvider;
    private PlaybackControlsView controlsView;
    private boolean nowPlaying;

    private void registerPlugins() {
        PlayKitManager.registerPlugins(SamplePlugin.factory);
        PlayKitManager.registerPlugins(IMASimplePlugin.factory);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerPlugins();

        mediaProvider = new MockMediaProvider("mock/entries.playkit.json", this, "dash");

//        mediaProvider = new PhoenixMediaProvider(MockParams.sessionProvider, MediaId, MockParams.MediaType, Format);

        mediaProvider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(final ResultElement<PKMediaEntry> response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (response.isSuccess()) {
                            onMediaLoaded(response.getResponse());
                        } else {

                            Toast.makeText(MainActivity.this, "failed to fetch media data: " + (response.getError() != null ? response.getError().getMessage() : ""), Toast.LENGTH_LONG).show();
                            Log.e(TAG, "failed to fetch media data: " + (response.getError() != null ? response.getError().getMessage() : ""));
                        }
                    }
                });
            }
        });

    }

    private void onMediaLoaded(PKMediaEntry mediaEntry){

        PlayerConfig config = new PlayerConfig();

        config.media.setMediaEntry(mediaEntry);


        if(player == null){

            configurePlugins(config.plugins);
            
            player = PlayKitManager.loadPlayer(config, this);

            Log.d(TAG, "Player: " + player.getClass());
            addPlayerListeners();

            LinearLayout layout = (LinearLayout) findViewById(R.id.player_root);
            layout.addView(player.getView());

            controlsView = (PlaybackControlsView) this.findViewById(R.id.playerControls);
            controlsView.setPlayer(player);
            //controlsView.setVisibility(View.INVISIBLE);
        }
        player.prepare(config.media);
    }

    private void configurePlugins(PlayerConfig.Plugins config) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("delay", 4200);
        config.setPluginConfig("Sample", jsonObject);
        addIMAPluginConfig(config);
    }

    private void addIMAPluginConfig(PlayerConfig.Plugins config){
        String adTagUrl = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/3274935/preroll&impl=s&gdfp_req=1&env=vp&output=xml_vast2&unviewed_position_start=1&url=[referrer_url]&description_url=[description_url]&correlator=[timestamp]";
        List<String> videoMimeTypes = new ArrayList<>();
        videoMimeTypes.add(MimeTypes.APPLICATION_MP4);
        videoMimeTypes.add(MimeTypes.APPLICATION_M3U8);
        AdsConfig adsConfig = new AdsConfig("en", false, true, 60000, videoMimeTypes, adTagUrl);
        config.setPluginConfig(IMASimplePlugin.factory.getName(), adsConfig.toJSONObject());


    }

    @Override
    protected void onStop() {
        super.onStop();
        controlsView.release();
        player.release();
    }

    private void addPlayerListeners() {
        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                Log.d(TAG, "Ad Event AD_CONTENT_PAUSE_REQUESTED");
            }
        }, IMAEvents.AD_CONTENT_PAUSE_REQUESTED);
        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                nowPlaying = true;
            }
        }, PlayerEvent.PLAY);

        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                nowPlaying = false;
            }
        }, PlayerEvent.PAUSE);

        player.addStateChangeListener(new PlayerState.Listener() {
            @Override
            public void onPlayerStateChanged(Player player, PlayerState newState) {
                if(controlsView != null){
                    controlsView.setPlayerState(newState);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(player != null){
            player.restore();
            if (nowPlaying && AUTO_PLAY_ON_RESUME) {
                player.play();
            }
        }
        if(controlsView != null){
            controlsView.resume();
        }
    }
}
