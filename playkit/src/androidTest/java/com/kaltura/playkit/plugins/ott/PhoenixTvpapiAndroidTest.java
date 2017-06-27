package com.kaltura.playkit.plugins.ott;

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
public class PhoenixTvpapiAndroidTest {

    private Player player;
    private Context context;
    private JsonObject tvpapiPluginConfig;
    private JsonObject phoenixPluginConfig;
    private MessageBus messageBus;
    private PhoenixAnalyticsPlugin phoenixAnalyticsPlugin;
    private TVPAPIAnalyticsPlugin tvpapiAnalyticsPlugin;
    private int duration = 3000;
    private long seek = 100;
    private String entryId = "259295";
    private PlayerEvent event;
    private String fileId = "464302";
    private String siteGuid = "716158";
    private String ks = "djJ8MTk4fEyWxc4-2rcCvN4AzMimxDwdR7kMDrq1eifV9iYcWMdR2ZqjWH1eNP5YQzIW4Eq61o0zVQjQgCSfLpMYf3PqmQwdvwurjloYEeIQhrOvluwu";

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getContext();
        setTvpapiPluginConfigObject();
        setPhoenixPluginConfig();
        setMediaObject();
        messageBus = new MessageBus();

        player = new MockPlayer();
        ((MockPlayer) player).setDuration(duration);
        player.seekTo(seek);

        phoenixAnalyticsPlugin = (PhoenixAnalyticsPlugin) PhoenixAnalyticsPlugin.factory.newInstance();
        tvpapiAnalyticsPlugin = (TVPAPIAnalyticsPlugin) TVPAPIAnalyticsPlugin.factory.newInstance();
    }


    private void setTvpapiPluginConfigObject() {
        tvpapiPluginConfig = new JsonObject();
        tvpapiPluginConfig.addProperty("fileId", fileId);
        tvpapiPluginConfig.addProperty("baseUrl", "http://tvpapi-preprod.ott.kaltura.com/v3_9/gateways/jsonpostgw.aspx?");
        tvpapiPluginConfig.addProperty("timerInterval", 30);

        JsonObject initObj = new JsonObject();
        initObj.addProperty("SiteGuid", siteGuid);
        initObj.addProperty("ApiUser", "tvpapi_198");
        initObj.addProperty("DomainID", "354531");
        initObj.addProperty("UDID", "e8aa934c-eae4-314f-b6a0-f55e96498786");
        initObj.addProperty("ApiPass", "11111");
        initObj.addProperty("Platform", "Cellular");

        JsonObject locale = new JsonObject();
        initObj.addProperty("LocaleUserState", "Unknown");
        initObj.addProperty("LocaleCountry", "");
        initObj.addProperty("LocaleDevice", "");
        initObj.addProperty("LocaleLanguage", "en");

        initObj.add("Locale", locale);

        tvpapiPluginConfig.add("initObj", initObj);
    }

    private void setPhoenixPluginConfig() {
        phoenixPluginConfig = new JsonObject();
        phoenixPluginConfig.addProperty("fileId", "464302");
        phoenixPluginConfig.addProperty("baseUrl", "http://api-preprod.ott.kaltura.com/v4_1/api_v3/");
        phoenixPluginConfig.addProperty("timerInterval", 30);
        phoenixPluginConfig.addProperty("ks", ks);
        phoenixPluginConfig.addProperty("partnerId", 198);
    }

    private void setMediaObject() {
        PKMediaConfig config = new PKMediaConfig();
        PKMediaSource mediaSource = new PKMediaSource().setUrl("http://cdnapi.kaltura.com/p/1774581/sp/177458100/playManifest/entryId/1_mphei4ku/format/applehttp/tags/mbr/protocol/http/f/a.m3u8");
        mediaSource.setId("516109");
        ArrayList<PKMediaSource> sourceList = new ArrayList<>();
        sourceList.add(mediaSource);
        PKMediaEntry mediaEntryProvider = new PKMediaEntry().setId(entryId).setDuration(duration).setSources(sourceList);
        config.setMediaEntry(mediaEntryProvider);
    }

    @Test
    public void testTvpapiPlugin() {
        tvpapiAnalyticsPlugin.onLoad(player, tvpapiPluginConfig, messageBus, context);

        event = new PlayerEvent(PlayerEvent.Type.PAUSE);
        messageBus.listen(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                if (((LogEvent) event).log.contains("PAUSE")) {
                    Assert.assertTrue(((LogEvent) event).log.contains(TVPAPIAnalyticsPlugin.factory.getName()));
                    Assert.assertTrue(((LogEvent) event).request.contains("iLocation\":" + seek));
                    Assert.assertTrue(((LogEvent) event).request.contains("iMediaID\":\"" + entryId));
                    Assert.assertTrue(((LogEvent) event).request.contains( "Action\":\"pause\""));
                    Assert.assertTrue(((LogEvent) event).request.contains("iFileID\":\"" + fileId));
                    Assert.assertTrue(((LogEvent) event).request.contains("SiteGuid\":\"" + siteGuid));
                }
            }
        }, LogEvent.LogType.LogEvent);
        messageBus.post(event);
    }

    @Test
    public void testPhoenixPlugin() {
        phoenixAnalyticsPlugin.onLoad(player, phoenixPluginConfig, messageBus, context);
        event = new PlayerEvent(PlayerEvent.Type.PAUSE);
        messageBus.listen(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                if (((LogEvent) event).log.contains("PAUSE")) {
                    Assert.assertTrue(((LogEvent) event).log.contains(PhoenixAnalyticsPlugin.factory.getName()));
                    Assert.assertTrue(((LogEvent) event).request.contains("position\":" + seek));
                    Assert.assertTrue(((LogEvent) event).request.contains("id\":\"" + entryId));
                    Assert.assertTrue(((LogEvent) event).request.contains( "action\":\"PAUSE\""));
                    Assert.assertTrue(((LogEvent) event).request.contains("fileId\":\"" + fileId));
                    Assert.assertTrue(((LogEvent) event).request.contains("ks\":\"" + ks));
                }
            }
        }, LogEvent.LogType.LogEvent);
        messageBus.post(event);
    }
}
