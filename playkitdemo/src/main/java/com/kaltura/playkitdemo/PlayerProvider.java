package com.kaltura.playkitdemo;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.backend.base.OnMediaLoadCompletion;
import com.kaltura.playkit.backend.phoenix.PhoenixMediaProvider;
import com.kaltura.playkit.connect.ResultElement;
import com.kaltura.playkit.connect.SessionProvider;
import com.kaltura.playkitdemo.MainActivity;
import com.kaltura.playkitdemo.StandalonePlayerActivity;
import com.kaltura.playkitdemo.data.JsonFetchTask;
import com.kaltura.playkitdemo.jsonConverters.ConverterKalturaOvpMediaProvider;
import com.kaltura.playkitdemo.jsonConverters.ConverterMedia;
import com.kaltura.playkitdemo.jsonConverters.ConverterPhoenixMediaProvider;
import com.kaltura.playkitdemo.jsonConverters.ConverterPlayerConfig;
import com.kaltura.playkitdemo.jsonConverters.ConverterPlugins;
import com.kaltura.playkitdemo.jsonConverters.ConverterSessionProvider;
import com.kaltura.playkitdemo.jsonConverters.ConverterStandalonePlayer;
import com.kaltura.playkitdemo.jsonConverters.JsonStandalonePlayer;

import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;
import static com.kaltura.playkitdemo.jsonConverters.ConverterStandalonePlayer.MediaProviderTypes.KALTURA_OVP_MEDIA_PROVIDER;
import static com.kaltura.playkitdemo.jsonConverters.ConverterStandalonePlayer.MediaProviderTypes.PHOENIX_MEDIA_PROVIDER;

/**
 * Created by itanbarpeled on 20/11/2016.
 */

public class PlayerProvider {


    public interface OnPlayerReadyListener {
        void onPlayerRead(Player player);
    }


    public static void getPlayer(String playerConfigLink, Activity context, OnPlayerReadyListener onPlayerReadyListener) {

        fetchJsonData(playerConfigLink, onPlayerReadyListener, context);

    }


    public static void getPlayer(MediaEntryProvider mediaEntryProvider, Activity context, OnPlayerReadyListener onPlayerReadyListener) {

        loadMediaProvider(mediaEntryProvider, null, onPlayerReadyListener, context);

    }



    private static void fetchJsonData(String playerConfigLink, final OnPlayerReadyListener onPlayerReadyListener, final Activity context) {

        new JsonFetchTask(context, true, new JsonFetchTask.OnJsonFetchedListener() {

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
                                setPhoenixMediaProvider(converterPlayerConfig, converterStandalonePlayer.getPhoenixMediaProvider(), onPlayerReadyListener, context);
                                break;
                            case KALTURA_OVP_MEDIA_PROVIDER:
                                setKalturaOvpMediaProvider(converterPlayerConfig, converterStandalonePlayer.getKalturaOvpMediaProvider(), onPlayerReadyListener, context);
                                break;
                        }
                    }
                }
            }
        }).execute(playerConfigLink);
    }



    private static ConverterStandalonePlayer.MediaProviderTypes getMediaProviderType(ConverterStandalonePlayer converterStandalonePlayer) {

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



    private static void setPhoenixMediaProvider(ConverterPlayerConfig converterPlayerConfig, ConverterPhoenixMediaProvider converterPhoenixMediaProvider,
                                         OnPlayerReadyListener onPlayerReadyListener, Activity context) {

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
        loadMediaProvider(phoenixMediaProvider, converterPlayerConfig, onPlayerReadyListener, context);

    }


    private static void setKalturaOvpMediaProvider(ConverterPlayerConfig converterPlayerConfig, ConverterKalturaOvpMediaProvider converterKalturaOvpMediaProvider,
                                            OnPlayerReadyListener onPlayerReadyListener, Activity context) {
        // TBD - call loadMediaProvider() method, as done in setPhoenixMediaProvider()
    }


    private static void loadMediaProvider(MediaEntryProvider mediaEntryProvider, final ConverterPlayerConfig converterPlayerConfig,
                                   final OnPlayerReadyListener onPlayerReadyListener, final Activity context) {

        mediaEntryProvider.load(new OnMediaLoadCompletion() {

            @Override
            public void onComplete(final ResultElement<PKMediaEntry> response) {

                context.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        if (response.isSuccess()) {
                            onMediaLoaded(response.getResponse(), converterPlayerConfig, onPlayerReadyListener, context);

                        } else {
                            String error = "failed to fetch media data: " + (response.getError() != null ? response.getError().getMessage() : "");
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show();
                            Log.e(MainActivity.TAG, error);
                            onPlayerReadyListener.onPlayerRead(null);
                        }
                    }
                });
            }
        });
    }



    private static void onMediaLoaded(PKMediaEntry mediaEntry, ConverterPlayerConfig converterPlayerConfig,
                                      OnPlayerReadyListener onPlayerReadyListener, Activity context) {

        PlayerConfig config = new PlayerConfig();

        configureMedia(config.media, mediaEntry, converterPlayerConfig != null ? converterPlayerConfig.getMedia() : null);
        configurePlugins(config.plugins, converterPlayerConfig != null ? converterPlayerConfig.getPlugins() : null);

        Player player = PlayKitManager.loadPlayer(config, context);

        addPlayerListeners(player);

        onPlayerReadyListener.onPlayerRead(player);


        /*
        if (mPlayer == null) {
        } else {
            mPlayer.prepare(config.media);
        }
        */
    }


    private static void configureMedia(PlayerConfig.Media media, PKMediaEntry mediaEntry, ConverterMedia converterMedia) {

        media.setMediaEntry(mediaEntry); // we set mediaEntry regardless if there is Media object in config Json

        if (converterMedia == null) { // Media object in config Json isn't mandatory
            return;
        }

        media.setStartPosition(converterMedia.getStartPosition());
        media.setAutoPlay(converterMedia.isAutoPlay());

    }


    private static void configurePlugins(PlayerConfig.Plugins plugins, List<ConverterPlugins> converterPlugins) {


        if (converterPlugins == null) { // Plugins object in config Json isn't mandatory
            return;
        }

        for (ConverterPlugins converterPlugin : converterPlugins) {

        }


        /*
        try {
            config.setPluginConfig("Sample", new JSONObject().put("delay", 4200));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        */
    }



    private static void addPlayerListeners(Player player) {


        /*
        player.addEventListener(new PlayerEvent.Listener() {
            @Override
            public void onPlayerEvent(Player player, PlayerEvent event) {


            }
        }, PlayerEvent.DURATION_CHANGE, PlayerEvent.CAN_PLAY);
        */


        player.addEventListener(new PlayerEvent.Listener() {

            @Override
            public void onPlayerEvent(Player player, PlayerEvent event) {
                Log.v(MainActivity.TAG, "" + event.name());
            }

        }, PlayerEvent.PLAYING, PlayerEvent.PAUSE, PlayerEvent.CAN_PLAY, PlayerEvent.SEEKING, PlayerEvent.SEEKED);


        /*
        player.addStateChangeListener(new PlayerState.Listener() {
            @Override
            public void onPlayerStateChanged(Player player, PlayerState newState) {
                if (mControlsView != null) {
                    mControlsView.setPlayerState(newState);
                }
            }
        });
        */


    }
}


