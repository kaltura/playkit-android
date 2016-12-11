package com.kaltura.playkit.plugins;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.google.gson.JsonObject;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.player.PlayerController;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

/**
 * Created by zivilan on 11/12/2016.
 */

@RunWith(AndroidJUnit4.class)
public class KalturaStatsAndroidTest {

    private Player player;
    private Context context;
    private JsonObject pluginConfig;
    private PlayerConfig.Media mediaConfig;
    private MessageBus messageBus;
    private KalturaStatsPlugin plugin;

    @Before
    public void setUp(){
        context = InstrumentationRegistry.getContext();
        setPluginConfigObject();
        setMediaObject();
        messageBus = new MessageBus(context);

        player = new PlayerController(context, mediaConfig);

        plugin = (KalturaStatsPlugin) KalturaStatsPlugin.factory.newInstance();
        plugin.onLoad(player, mediaConfig, pluginConfig, messageBus, context);



//        PlayKitManager.registerPlugins(KalturaStatsPlugin.factory);
//        PlayerConfig config = new PlayerConfig();
//        player = PlayKitManager.loadPlayer(config, context);
//        configurePlugins(config.plugins, "KalturaStats", pluginConfig);

    }

    private void configurePlugins(PlayerConfig.Plugins plugins, String pluginName, JsonObject pluginConfig){
        plugins.setPluginConfig(pluginName, pluginConfig);
    }

    private void setPluginConfigObject(){
        pluginConfig = new JsonObject();
        pluginConfig.addProperty("sessionId", "b3460681-b994-6fad-cd8b-f0b65736e837");
        pluginConfig.addProperty("uiconfId", 24997472);
        pluginConfig.addProperty("baseUrl", "stats.kaltura.com");
        pluginConfig.addProperty("partnerId", 1281471);
        pluginConfig.addProperty("timerInterval", 30000);
    }

    private void setMediaObject(){
        PlayerConfig config = new PlayerConfig();
        config.media.setAutoPlay(false);
        PKMediaSource mediaSource = new PKMediaSource().setUrl("http://cdnapi.kaltura.com/p/1774581/sp/177458100/playManifest/entryId/1_mphei4ku/format/applehttp/tags/mbr/protocol/http/f/a.m3u8");
        mediaSource.setId("516109");
        ArrayList<PKMediaSource> sourceList = new ArrayList<>();
        sourceList.add(mediaSource);
        PKMediaEntry mediaEntryProvider = new PKMediaEntry().setId("259295").setDuration(516109).setSources(sourceList);
        config.media.setMediaEntry(mediaEntryProvider);
        mediaConfig = config.media;
    }

    @Test
    public void testPlugin(){
        player.g
        messageBus.post(new PlayerEvent(PlayerEvent.Type.PLAY));
        Assert.assertTrue(player.isPlaying());
    }
}
