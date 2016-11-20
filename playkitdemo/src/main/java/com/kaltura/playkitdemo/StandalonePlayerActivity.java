package com.kaltura.playkitdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.backend.base.OnMediaLoadCompletion;
import com.kaltura.playkit.backend.mock.MockMediaProvider;
import com.kaltura.playkit.backend.phoenix.PhoenixMediaProvider;
import com.kaltura.playkit.backend.phoenix.data.AssetResult;
import com.kaltura.playkit.backend.phoenix.data.ResultAdapter;
import com.kaltura.playkit.connect.ResultElement;
import com.kaltura.playkit.connect.SessionProvider;
import com.kaltura.playkit.plugins.SamplePlugin;
import com.kaltura.playkitdemo.data.JsonTask;
import com.kaltura.playkitdemo.jsonConverters.ConverterKalturaOvpMediaProvider;
import com.kaltura.playkitdemo.jsonConverters.ConverterMedia;
import com.kaltura.playkitdemo.jsonConverters.ConverterPhoenixMediaProvider;
import com.kaltura.playkitdemo.jsonConverters.ConverterPlayerConfig;
import com.kaltura.playkitdemo.jsonConverters.ConverterPlugins;
import com.kaltura.playkitdemo.jsonConverters.ConverterSessionProvider;
import com.kaltura.playkitdemo.jsonConverters.ConverterStandalonePlayer;
import com.kaltura.playkitdemo.jsonConverters.JsonStandalonePlayer;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static android.R.attr.format;
import static com.google.gson.internal.UnsafeAllocator.create;
import static com.kaltura.playkitdemo.MockParams.Format;
import static com.kaltura.playkitdemo.MockParams.KS;
import static com.kaltura.playkitdemo.MockParams.MediaId;
import static com.kaltura.playkitdemo.MockParams.PartnerId;
import static com.kaltura.playkitdemo.MockParams.PhoenixBaseUrl;
import static com.kaltura.playkitdemo.MockParams.sessionProvider;
import static com.kaltura.playkitdemo.jsonConverters.ConverterStandalonePlayer.MediaProviderTypes.KALTURA_OVP_MEDIA_PROVIDER;
import static com.kaltura.playkitdemo.jsonConverters.ConverterStandalonePlayer.MediaProviderTypes.PHOENIX_MEDIA_PROVIDER;


public class StandalonePlayerActivity extends AppCompatActivity {


    private Player mPlayer;
    private PlaybackControlsView controlsView;
    private boolean nowPlaying;

    private void registerPlugins() {
        PlayKitManager.registerPlugins(SamplePlugin.factory);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerPlugins();

        Intent intent = getIntent();
        String url;

        if ((url = intent.getDataString()) != null) { // app was invoked via deep linking
            handleDeepLink(url);
        } else { // app invocation done by the standard way
            MediaEntryProvider mediaEntryProvider = new PhoenixMediaProvider(sessionProvider, MediaId, MockParams.MediaType, Format);
            loadMediaProvider(mediaEntryProvider, null);
        }
    }



    private void handleDeepLink(String url) {

        new JsonTask(StandalonePlayerActivity.this, true, new JsonTask.OnJsonFetchedListener() {

            @Override
            public void onJsonFetched(String json) {

                JsonStandalonePlayer standalonePlayer = new Gson().fromJson(json, JsonStandalonePlayer.class);
                ConverterStandalonePlayer converterStandalonePlayer = standalonePlayer.getStandalonePlayer();

                if (converterStandalonePlayer != null) {

                    ConverterStandalonePlayer.MediaProviderTypes mediaProviderType = getMediaProviderType(converterStandalonePlayer);
                    if (mediaProviderType != null) {

                        ConverterPlayerConfig converterPlayerConfig = converterStandalonePlayer.getPlayerConfig();

                        switch (mediaProviderType) {
                            case PHOENIX_MEDIA_PROVIDER:
                                setPhoenixMediaProvider(converterPlayerConfig, converterStandalonePlayer.getPhoenixMediaProvider());
                                break;
                            case KALTURA_OVP_MEDIA_PROVIDER:
                                setKalturaOvpMediaProvider(converterPlayerConfig, converterStandalonePlayer.getKalturaOvpMediaProvider());
                                break;
                        }
                    }
                }
            }
        }).execute(url);
    }


    private ConverterStandalonePlayer.MediaProviderTypes getMediaProviderType(ConverterStandalonePlayer converterStandalonePlayer) {

        ConverterPhoenixMediaProvider converterPhoenixMediaProvider = converterStandalonePlayer.getPhoenixMediaProvider();
        ConverterKalturaOvpMediaProvider converterKalturaOvpMediaProvider = converterStandalonePlayer.getKalturaOvpMediaProvider();

        // TODO handle wrong json
        if (converterPhoenixMediaProvider != null && converterKalturaOvpMediaProvider != null) { // two mediaProvider was defined
            return null;
        } else if (converterPhoenixMediaProvider == null && converterKalturaOvpMediaProvider == null) { // no mediaProvider was defined
            return null;
        } else if (converterPhoenixMediaProvider != null) {
            return PHOENIX_MEDIA_PROVIDER;
        } else  {
            return KALTURA_OVP_MEDIA_PROVIDER;
        }
    }


    private void setPhoenixMediaProvider(ConverterPlayerConfig converterPlayerConfig, ConverterPhoenixMediaProvider converterPhoenixMediaProvider) {

        final ConverterSessionProvider converterSessionProvider = converterPhoenixMediaProvider.getSessionProvider();


        SessionProvider sessionProvider = new SessionProvider() {
            @Override
            public String baseUrl() {
                return converterSessionProvider.getBaseUrl();
            }

            @Override
            public String getKs() {
                return converterSessionProvider.getKs();
            }

            @Override
            public int partnerId() {
                return converterSessionProvider.getPartnerId();
            }
        };


        String assetId = converterPhoenixMediaProvider.getAssetId();
        String referenceType = converterPhoenixMediaProvider.getReferenceType();
        List<String> format = new ArrayList<>(converterPhoenixMediaProvider.getFormats());
        String[] formatVarargs = format.toArray(new String[format.size()]); // convert to varargs, String...

        MediaEntryProvider phoenixMediaProvider = new PhoenixMediaProvider(sessionProvider, assetId, referenceType, formatVarargs);
        loadMediaProvider(phoenixMediaProvider, converterPlayerConfig);


        /*
        Log.v(MainActivity.TAG, "setPhoenixMediaProvider " + converterPhoenixMediaProvider.getAssetId());
        Log.v(MainActivity.TAG, "setPhoenixMediaProvider " + converterPhoenixMediaProvider.getFormats());
        */

    }


    private void setKalturaOvpMediaProvider(ConverterPlayerConfig converterPlayerConfig, ConverterKalturaOvpMediaProvider converterKalturaOvpMediaProvider) {

        /*
        Log.v(MainActivity.TAG, "setKalturaOvpMediaProvider " + converterKalturaOvpMediaProvider.getAssetId());
        Log.v(MainActivity.TAG, "setKalturaOvpMediaProvider " + converterKalturaOvpMediaProvider.getKs());
        Log.v(MainActivity.TAG, "setKalturaOvpMediaProvider " + converterKalturaOvpMediaProvider.getReferenceType());
        */
    }



    private void loadMediaProvider(MediaEntryProvider mediaEntryProvider, final ConverterPlayerConfig converterPlayerConfig) {
        mediaEntryProvider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(final ResultElement<PKMediaEntry> response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (response.isSuccess()) {
                            onMediaLoaded(response.getResponse(), converterPlayerConfig);
                        } else {
                            String error = "failed to fetch media data: " + (response.getError() != null ? response.getError().getMessage() : "");
                            Toast.makeText(StandalonePlayerActivity.this, error, Toast.LENGTH_LONG).show();
                            Log.e(MainActivity.TAG, error);
                        }
                    }
                });
            }
        });
    }



    private void configureMedia(PlayerConfig.Media media, PKMediaEntry mediaEntry, ConverterMedia converterMedia) {

        media.setMediaEntry(mediaEntry);

        if (converterMedia == null) { // Media object in Json isn't mandatory
            return;
        }

        media.setStartPosition(converterMedia.getStartPosition());
        media.setAutoPlay(converterMedia.isAutoPlay());

        /*
        Log.v(MainActivity.TAG, "configureMedia " + converterMedia.getStartPosition());
        Log.v(MainActivity.TAG, "configureMedia " + converterMedia.isAutoPlay());
        */

    }



    private void onMediaLoaded(PKMediaEntry mediaEntry, ConverterPlayerConfig converterPlayerConfig) {

        PlayerConfig config = new PlayerConfig();

        configureMedia(config.media, mediaEntry, converterPlayerConfig.getMedia());

        if (mPlayer == null) {

            configurePlugins(config.plugins, converterPlayerConfig.getPlugins());

            mPlayer = PlayKitManager.loadPlayer(config, this);

            Log.d(MainActivity.TAG, "Player: " + mPlayer.getClass());
            addPlayerListeners();

            LinearLayout layout = (LinearLayout) findViewById(R.id.player_root);
            layout.addView(mPlayer.getView());

            mPlayer.play();


            /*
            controlsView = (PlaybackControlsView) this.findViewById(R.id.playerControls);
            controlsView.setPlayer(mPlayer);
            */

        } else {
            mPlayer.prepare(config.media);
        }
    }



    private void configurePlugins(PlayerConfig.Plugins plugins, List<ConverterPlugins> converterPlugins) {


        for (ConverterPlugins converterPlugin : converterPlugins) {
            /*
            Log.v(MainActivity.TAG, "configurePlugins " + converterPlugin.getPluginName());
            Log.v(MainActivity.TAG, "configurePlugins " + converterPlugin.getAdTagUrl());
            Log.v(MainActivity.TAG, "configurePlugins " + converterPlugin.isAdsAnalytics());
            */
        }


        /*
        try {
            config.setPluginConfig("Sample", new JSONObject().put("delay", 4200));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        */
    }


    /*
    @Override
    protected void onStop() {
        super.onStop();
        controlsView.release();
        if (mPlayer != null) {
            mPlayer.release();
        }
    }
    */

    private void addPlayerListeners() {
        mPlayer.addEventListener(new PlayerEvent.Listener() {
            @Override
            public void onPlayerEvent(Player player, PlayerEvent event) {

            }
        }, PlayerEvent.DURATION_CHANGE, PlayerEvent.CAN_PLAY);

        mPlayer.addEventListener(new PlayerEvent.Listener() {
            @Override
            public void onPlayerEvent(Player player, PlayerEvent event) {
                switch (event) {
                    case PLAY:
                        nowPlaying = true;
                        break;
                    case PAUSE:
                        nowPlaying = false;
                        break;
                }
            }
        }, PlayerEvent.PLAYING);

        mPlayer.addStateChangeListener(new PlayerState.Listener() {
            @Override
            public void onPlayerStateChanged(Player player, PlayerState newState) {
                if (controlsView != null) {
                    controlsView.setPlayerState(newState);
                }
            }
        });
    }

    /*
    @Override
    protected void onResume() {
        super.onResume();
        if (mPlayer != null) {
            mPlayer.restore();
            if (nowPlaying) {
                mPlayer.play();
            }
        }

        if(controlsView != null){
            controlsView.resume();
        }

    }
    */
}

