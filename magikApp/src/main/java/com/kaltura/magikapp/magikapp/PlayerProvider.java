package com.kaltura.magikapp.magikapp;

import android.app.Activity;
import android.util.Log;

import com.connect.backend.PrimitiveResult;
import com.connect.backend.SessionProvider;
import com.connect.backend.phoenix.APIDefines;
import com.connect.core.OnCompletion;
import com.connect.utils.ResultElement;
import com.google.gson.JsonObject;
import com.kaltura.magikapp.data.ConverterKalturaOvpMediaProvider;
import com.kaltura.magikapp.data.ConverterMedia;
import com.kaltura.magikapp.data.ConverterMediaProvider;
import com.kaltura.magikapp.data.ConverterMockMediaProvider;
import com.kaltura.magikapp.data.ConverterPhoenixMediaProvider;
import com.kaltura.magikapp.data.ConverterPlayerConfig;
import com.kaltura.magikapp.data.ConverterPlugin;
import com.kaltura.magikapp.data.ConverterSessionProvider;
import com.kaltura.magikapp.data.ConverterStandalonePlayer;
import com.kaltura.magikapp.data.JsonConverterHandler;
import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.backend.OnMediaLoadCompletion;
import com.kaltura.playkit.backend.mock.MockMediaProvider;
import com.kaltura.playkit.backend.phoenix.PhoenixMediaProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.kaltura.magikapp.data.ConverterStandalonePlayer.MediaProviderTypes.KALTURA_OVP_MEDIA_PROVIDER;
import static com.kaltura.magikapp.data.ConverterStandalonePlayer.MediaProviderTypes.MOCK_MEDIA_PROVIDER;
import static com.kaltura.magikapp.data.ConverterStandalonePlayer.MediaProviderTypes.PHOENIX_MEDIA_PROVIDER;


/**
 * Created by itanbarpeled on 20/11/2016.
 */

public class PlayerProvider {


    public interface OnPlayerReadyListener {
        void onPlayerReady(Player player, PlayerConfig playerConfig);
    }


    public PlayerProvider() {

    }


    public void getPlayer(String json, Activity context, OnPlayerReadyListener onPlayerReadyListener) {

        ConverterStandalonePlayer converterStandalonePlayer = JsonConverterHandler.getConverterStandalonePlayer(json);
        ConverterStandalonePlayer.MediaProviderTypes mediaProviderType = getMediaProviderType(converterStandalonePlayer);

        if (mediaProviderType == null) {
            onPlayerReadyListener.onPlayerReady(null, null);
            return;
        }

        ConverterPlayerConfig converterPlayerConfig = converterStandalonePlayer.getPlayerConfig();

        switch (mediaProviderType) {

            case PHOENIX_MEDIA_PROVIDER:
                setPhoenixMediaProvider(converterPlayerConfig, (ConverterPhoenixMediaProvider) converterStandalonePlayer.getMediaProvider(), onPlayerReadyListener, context);
                break;

            case KALTURA_OVP_MEDIA_PROVIDER:
                setKalturaOvpMediaProvider(converterPlayerConfig, (ConverterKalturaOvpMediaProvider) converterStandalonePlayer.getMediaProvider(), onPlayerReadyListener, context);
                break;

            case MOCK_MEDIA_PROVIDER:
                setMockMediaProvider(converterPlayerConfig, (ConverterMockMediaProvider) converterStandalonePlayer.getMediaProvider(), onPlayerReadyListener, context);
                break;
        }

    }



    private ConverterStandalonePlayer.MediaProviderTypes getMediaProviderType(ConverterStandalonePlayer converterStandalonePlayer) {

        if (converterStandalonePlayer == null) {
            return null;
        }

        ConverterMediaProvider converterMediaProvider = converterStandalonePlayer.getMediaProvider();

        if (converterMediaProvider instanceof ConverterPhoenixMediaProvider) {
            return PHOENIX_MEDIA_PROVIDER;
        } else if (converterMediaProvider instanceof ConverterKalturaOvpMediaProvider) {
            return KALTURA_OVP_MEDIA_PROVIDER;
        } else if (converterMediaProvider instanceof ConverterMockMediaProvider) {
            return MOCK_MEDIA_PROVIDER;
        } else {
            return null;
        }
    }



    private void setPhoenixMediaProvider(ConverterPlayerConfig converterPlayerConfig, ConverterPhoenixMediaProvider converterPhoenixMediaProvider,
                                         OnPlayerReadyListener onPlayerReadyListener, Activity context) {

        final ConverterSessionProvider converterSessionProvider = converterPhoenixMediaProvider.getSessionProvider();

        SessionProvider sessionProvider = new SessionProvider() {
            @Override
            public String baseUrl() {
                return converterSessionProvider.getBaseUrl();
            }

            @Override
            public void getSessionToken(OnCompletion<PrimitiveResult> completion) {
                String ks = converterSessionProvider.getKs();
                completion.onComplete(new PrimitiveResult(ks));
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

        MediaEntryProvider phoenixMediaProvider = new PhoenixMediaProvider().setSessionProvider(sessionProvider).setAssetId(assetId).setReferenceType(APIDefines.AssetReferenceType.Media).setFormats(formatVarargs);

        loadMediaProvider(phoenixMediaProvider, converterPlayerConfig, onPlayerReadyListener, context);

    }


    private void setMockMediaProvider(ConverterPlayerConfig converterPlayerConfig, ConverterMockMediaProvider converterMockMediaProvider,
                                      OnPlayerReadyListener onPlayerReadyListener, Activity context) {

        String mediaId = converterMockMediaProvider.getMediaId();
        HashMap<String, JsonObject> entries = converterMockMediaProvider.getEntries();

        JsonObject mediaParams = entries.get(mediaId);
        JsonObject mediaEntry = new JsonObject();
        mediaEntry.add(mediaId, mediaParams);

        MediaEntryProvider mockMediaProvider = new MockMediaProvider(mediaEntry, mediaId);
        loadMediaProvider(mockMediaProvider, converterPlayerConfig, onPlayerReadyListener, context);


    }



    private void setKalturaOvpMediaProvider(ConverterPlayerConfig converterPlayerConfig, ConverterKalturaOvpMediaProvider converterKalturaOvpMediaProvider,
                                            OnPlayerReadyListener onPlayerReadyListener, Activity context) {
        /* TBD -
            1. call loadMediaProvider() method, as done in setPhoenixMediaProvider()
            2. call setSessionProvider()
         */
    }


    private void loadMediaProvider(MediaEntryProvider mediaEntryProvider, final ConverterPlayerConfig converterPlayerConfig,
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
                            Log.e("check", error);
                            onPlayerReadyListener.onPlayerReady(null, null);

                        }
                    }
                });
            }
        });
    }



    private void onMediaLoaded(PKMediaEntry mediaEntry, ConverterPlayerConfig converterPlayerConfig,
                               OnPlayerReadyListener onPlayerReadyListener, Activity context) {


        PlayerConfig playerConfig = new PlayerConfig();

        setPlayerConfig(playerConfig, mediaEntry, converterPlayerConfig);

        returnResult(playerConfig, converterPlayerConfig, onPlayerReadyListener, context);

    }


    private void returnResult(PlayerConfig playerConfig, ConverterPlayerConfig converterPlayerConfig, OnPlayerReadyListener onPlayerReadyListener, Activity context) {

        Player player = PlayKitManager.loadPlayer(playerConfig, context);
        player.prepare(playerConfig.media);

        if (converterPlayerConfig.getMedia().isAutoPlay()) {
            player.play();
        }


        onPlayerReadyListener.onPlayerReady(player, playerConfig);

    }


    private void setPlayerConfig(PlayerConfig playerConfig, PKMediaEntry mediaEntry, ConverterPlayerConfig converterPlayerConfig) {

        configureMedia(playerConfig.media, mediaEntry, converterPlayerConfig != null ? converterPlayerConfig.getMedia() : null);
//        configurePlugins(playerConfig.plugins, converterPlayerConfig != null ? converterPlayerConfig.getPlugins() : null);


    }


    private void configureMedia(PlayerConfig.Media media, PKMediaEntry mediaEntry, ConverterMedia converterMedia) {

        media.setMediaEntry(mediaEntry); // we set mediaEntry regardless if there is Media object in config Json

        if (converterMedia == null) { // Media object in config Json isn't mandatory
            return;
        }

        media.setStartPosition(converterMedia.getStartPosition());
    }


    private void configurePlugins(PlayerConfig.Plugins plugins, List<ConverterPlugin> converterPlugins) {

        if (converterPlugins == null) { // Plugins object in config Json isn't mandatory
            return;
        }

        for (ConverterPlugin converterPlugin : converterPlugins) {
            plugins.setPluginConfig(converterPlugin.getPluginName(), converterPlugin.toJson());
        }
    }

}


