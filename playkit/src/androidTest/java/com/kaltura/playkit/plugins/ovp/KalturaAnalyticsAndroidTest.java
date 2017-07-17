package com.kaltura.playkit.plugins.ovp;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.google.gson.JsonObject;
import com.kaltura.playkit.LogEvent;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.plugins.MockPlayer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

/**
 * Created by zivilan on 12/12/2016.
 */
@RunWith(AndroidJUnit4.class)
public class KalturaAnalyticsAndroidTest {

    private Player player;
    private Context context;
    private JsonObject pluginConfig;
    private MessageBus messageBus;
    private KalturaAnalyticsPlugin plugin;
    private int duration = 3000;
    private long seek = 100;
    private String entryId = "259295";
    private int partnerId = 1281471;
    private int uiconfId = 24997472;
    private PlayerEvent event;

    @Before
    public void setUp(){
        context = InstrumentationRegistry.getContext();
        setPluginConfigObject();
        setMediaObject();
        messageBus = new MessageBus();

        player = new MockPlayer();
        ((MockPlayer) player).setDuration(duration);
        player.seekTo(seek);

        plugin = (KalturaAnalyticsPlugin) KalturaAnalyticsPlugin.factory.newInstance();
        plugin.onLoad(player, pluginConfig, messageBus, context);
    }


    private void setPluginConfigObject(){
        pluginConfig = new JsonObject();
        pluginConfig.addProperty("sessionId", "b3460681-b994-6fad-cd8b-f0b65736e837");
        pluginConfig.addProperty("uiconfId", uiconfId);
        pluginConfig.addProperty("baseUrl", "https://analytics.kaltura.com/api_v3/index.php");
        pluginConfig.addProperty("partnerId", partnerId);
        pluginConfig.addProperty("timerInterval", 30);
    }

    private void setMediaObject(){
        PKMediaConfig config = new PKMediaConfig();
        PKMediaSource mediaSource = new PKMediaSource().setUrl("http://cdnapi.kaltura.com/p/1774581/sp/177458100/playManifest/entryId/1_mphei4ku/format/applehttp/tags/mbr/protocol/http/f/a.m3u8");
        mediaSource.setId("516109");
        ArrayList<PKMediaSource> sourceList = new ArrayList<>();
        sourceList.add(mediaSource);
        PKMediaEntry mediaEntryProvider = new PKMediaEntry().setId(entryId).setDuration(duration).setSources(sourceList);
        config.setMediaEntry(mediaEntryProvider);
    }

    @Test
    public void testSeekPlugin(){
        event = new PlayerEvent(PlayerEvent.Type.SEEKED);
        messageBus.listen(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                Assert.assertTrue(((LogEvent)event).log.contains(KalturaAnalyticsPlugin.factory.getName()));
                Assert.assertTrue(((LogEvent)event).log.contains("SEEK"));
                Assert.assertTrue(((LogEvent)event).request.contains("action=trackEvent"));
                Assert.assertTrue(((LogEvent)event).request.contains("eventType=" + KalturaAnalyticsPlugin.KAnalonyEvents.SEEK.getValue()));
                Assert.assertTrue(((LogEvent)event).request.contains("playbackType=live"));
                Assert.assertTrue(((LogEvent)event).request.contains("clientVer=" + PlayKitManager.CLIENT_TAG));
                Assert.assertTrue(((LogEvent)event).request.contains("entryId=" + entryId));
                Assert.assertTrue(((LogEvent)event).request.contains("uiconfId=" + uiconfId));
                Assert.assertTrue(((LogEvent)event).request.contains("position=" + seek));
                Assert.assertTrue(((LogEvent)event).request.contains("eventIndex=" + 0));
            }
        }, LogEvent.LogType.LogEvent);
        messageBus.post(event);
    }

    @Test
    public void testPlayPlugin(){
        event = new PlayerEvent(PlayerEvent.Type.PLAY);
        messageBus.listen(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                Assert.assertTrue(((LogEvent)event).log.contains(KalturaAnalyticsPlugin.factory.getName()));
                Assert.assertTrue(((LogEvent)event).log.contains("PLAY"));
                Assert.assertTrue(((LogEvent)event).request.contains("action=trackEvent"));
                Assert.assertTrue(((LogEvent)event).request.contains("eventType=" + KalturaAnalyticsPlugin.KAnalonyEvents.PLAY_REQUEST.getValue()));
                Assert.assertTrue(((LogEvent)event).request.contains("playbackType=live"));
                Assert.assertTrue(((LogEvent)event).request.contains("clientVer=" + PlayKitManager.CLIENT_TAG));
                Assert.assertTrue(((LogEvent)event).request.contains("entryId=" + entryId));
                Assert.assertTrue(((LogEvent)event).request.contains("uiconfId=" + uiconfId));
                Assert.assertTrue(((LogEvent)event).request.contains("position=" + seek));
                Assert.assertTrue(((LogEvent)event).request.contains("eventIndex=" + 0));
            }
        }, LogEvent.LogType.LogEvent);
        messageBus.post(event);
    }
}
