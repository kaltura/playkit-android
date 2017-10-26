package com.kaltura.playkitdemo;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.kaltura.netkit.connect.response.PrimitiveResult;
import com.kaltura.netkit.connect.response.ResultElement;
import com.kaltura.netkit.utils.OnCompletion;
import com.kaltura.netkit.utils.SessionProvider;
import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.PKPluginConfigs;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.ads.AdCuePoints;
import com.kaltura.playkit.ads.AdEvent;
import com.kaltura.playkit.ads.PKAdEndedReason;
import com.kaltura.playkit.api.ovp.SimpleOvpSessionProvider;
import com.kaltura.playkit.api.phoenix.APIDefines;
import com.kaltura.playkit.mediaproviders.base.OnMediaLoadCompletion;
import com.kaltura.playkit.mediaproviders.mock.MockMediaProvider;
import com.kaltura.playkit.mediaproviders.ott.PhoenixMediaProvider;
import com.kaltura.playkit.mediaproviders.ovp.KalturaOvpMediaProvider;
import com.kaltura.playkit.player.AudioTrack;
import com.kaltura.playkit.player.BaseTrack;
import com.kaltura.playkit.player.PKTracks;
import com.kaltura.playkit.player.TextTrack;
import com.kaltura.playkit.player.VideoTrack;
import com.kaltura.playkit.plugins.SamplePlugin;
import com.kaltura.playkit.plugins.playback.KalturaPlaybackRequestAdapter;
import com.kaltura.playkit.utils.Consts;

import java.util.Collections;
import java.util.List;

import static com.kaltura.playkitdemo.MockParams.Format;
import static com.kaltura.playkitdemo.MockParams.Format2;
import static com.kaltura.playkitdemo.MockParams.Format_HD_Dash;
import static com.kaltura.playkitdemo.MockParams.Format_SD_Dash;
import static com.kaltura.playkitdemo.MockParams.OvpUserKS;
import static com.kaltura.playkitdemo.MockParams.PnxKS;
import static com.kaltura.playkitdemo.MockParams.SingMediaId;


public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener,
        OrientationManager.OrientationListener {


    public static final boolean AUTO_PLAY_ON_RESUME = false;

    private static final PKLog log = PKLog.get("MainActivity");

    private Player player;
    private MediaEntryProvider mediaProvider;
    private PlaybackControlsView controlsView;
    private boolean nowPlaying;
    private boolean isFullScreen;
    ProgressBar progressBar;
    private RelativeLayout playerContainer;
    private RelativeLayout spinerContainer;
    private AppCompatImageView fullScreenBtn;

    private Spinner videoSpinner, audioSpinner, textSpinner;

    private OrientationManager mOrientationManager;

    private void registerPlugins() {

        PlayKitManager.registerPlugins(this, SamplePlugin.factory);
        //PlayKitManager.registerPlugins(this, TVPAPIAnalyticsPlugin.factory);
        //PlayKitManager.registerPlugins(this, PhoenixAnalyticsPlugin.factory);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mOrientationManager = new OrientationManager(this, SensorManager.SENSOR_DELAY_NORMAL, this);
        mOrientationManager.enable();
        setContentView(R.layout.activity_main);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(this, "Please tap ALLOW", Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    1);
        }

        log.i("PlayKitManager: " + PlayKitManager.CLIENT_TAG);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);
        registerPlugins();

        OnMediaLoadCompletion playLoadedEntry = new OnMediaLoadCompletion() {
            @Override
            public void onComplete(final ResultElement<PKMediaEntry> response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (response.isSuccess()) {
                            onMediaLoaded(response.getResponse());
                        } else {
                            Toast.makeText(MainActivity.this, "failed to fetch media data: " + (response.getError() != null ? response.getError().getMessage() : ""), Toast.LENGTH_LONG).show();
                            log.e("failed to fetch media data: " + (response.getError() != null ? response.getError().getMessage() : ""));
                        }
                    }
                });
            }
        };

        startMockMediaLoading(playLoadedEntry);
//      startOvpMediaLoading(playLoadedEntry);
//      startOttMediaLoading(playLoadedEntry);
//      startSimpleOvpMediaLoading(playLoadedEntry);
//      LocalAssets.start(this, playLoadedEntry);
        playerContainer = (RelativeLayout)findViewById(R.id.player_container);
//        ViewGroup companionAdSlot = (ViewGroup) findViewById(R.id.companionAdSlot);
//        ImageView imageView1 = (ImageView) findViewById(R.id.imageViewCompanion);
//        Glide.with(getApplicationContext())
//                .load("http://kimg.voot.com/kimg/224332525b1c428d9800cf69b1688913_1280X720.jpg")
//                .into(imageView1);
        spinerContainer = (RelativeLayout)findViewById(R.id.spiner_container);
        fullScreenBtn = (AppCompatImageView)findViewById(R.id.full_screen_switcher);
        fullScreenBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int orient;
                if (isFullScreen) {
                    orient = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                }
                else {
                    orient = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                }
                setRequestedOrientation(orient);
            }
        });
    }

    private PKMediaEntry simpleMediaEntry(String id, String contentUrl, String licenseUrl, PKDrmParams.Scheme scheme) {
        return new PKMediaEntry()
                    .setSources(Collections.singletonList(new PKMediaSource()
                            .setUrl(contentUrl)
                            .setDrmData(Collections.singletonList(
                                    new PKDrmParams(licenseUrl, scheme)
                            )
                        )))
                    .setId(id);
    }

    private PKMediaEntry simpleMediaEntry(String id, String contentUrl) {
        return new PKMediaEntry()
                .setSources(Collections.singletonList(new PKMediaSource()
                        .setUrl(contentUrl)
                ))
                .setId(id);
    }

    private void startSimpleOvpMediaLoading(OnMediaLoadCompletion completion) {
        new KalturaOvpMediaProvider()
                .setSessionProvider(new SimpleOvpSessionProvider("https://cdnapisec.kaltura.com", 2222401, null))
                .setEntryId("1_f93tepsn")
                .load(completion);
    }

    private void startMockMediaLoading(OnMediaLoadCompletion completion) {

        mediaProvider = new MockMediaProvider("mock/entries.playkit.json", getApplicationContext(), "hls");

        mediaProvider.load(completion);
    }
    
    private void startOttMediaLoading(final OnMediaLoadCompletion completion) {
        SessionProvider ksSessionProvider = new SessionProvider() {
            @Override
            public String baseUrl() {
                return MockParams.PhoenixBaseUrl;
            }

            @Override
            public void getSessionToken(OnCompletion<PrimitiveResult> completion) {
                if (completion != null) {
                    completion.onComplete(new PrimitiveResult(PnxKS));
                }
            }

            @Override
            public int partnerId() {
                return MockParams.OttPartnerId;
            }
        };

        mediaProvider = new PhoenixMediaProvider()
                .setSessionProvider(ksSessionProvider)
                .setAssetId(SingMediaId) //bunny no horses id = "485380"
                .setAssetType(APIDefines.KalturaAssetType.Media)
                .setFormats(Format_SD_Dash, Format_HD_Dash, Format, Format2);

        mediaProvider.load(completion);
    }

    private void startOvpMediaLoading(final OnMediaLoadCompletion completion) {
        SessionProvider ksSessionProvider = new SessionProvider() {
            @Override
            public String baseUrl() {
                return MockParams.OvpBaseUrl;
            }

            @Override
            public void getSessionToken(OnCompletion<PrimitiveResult> completion) {
                if (completion != null) {
                    completion.onComplete(new PrimitiveResult(OvpUserKS));
                }
            }

            @Override
            public int partnerId() {
                return MockParams.OvpPartnerId;
            }
        };

        mediaProvider = new KalturaOvpMediaProvider().setSessionProvider(ksSessionProvider).setEntryId(MockParams.DRMEntryIdAnm);
        mediaProvider.load(completion);
    }

    private void onMediaLoaded(PKMediaEntry mediaEntry) {

        PKMediaConfig mediaConfig = new PKMediaConfig().setMediaEntry(mediaEntry).setStartPosition(0);
        PKPluginConfigs pluginConfig = new PKPluginConfigs();
        FrameLayout layout = (FrameLayout) findViewById(R.id.player_root);
        RelativeLayout adSkin = (RelativeLayout) findViewById(R.id.ad_skin);

        ((View) adSkin).findViewById(R.id.skip_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                player.getAdController().skipAd();
            }
        });

        ((View) adSkin).findViewById(R.id.learn_more_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                player.getAdController().openLearnMore();
            }
        });

        LinearLayout companionAdPlaceHolder = (LinearLayout)adSkin.findViewById(R.id.companionAdSlot);
        ((View)companionAdPlaceHolder).findViewById(R.id.imageViewCompanion).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                player.getAdController().openCompanionAdLearnMore();
            }
        });





        configurePlugins(pluginConfig, layout, adSkin);
        if (player == null) {
            player = PlayKitManager.loadPlayer(this, pluginConfig);
            if (layout != null) {
                layout.addView(player.getView());
            }

            KalturaPlaybackRequestAdapter.install(player, "myApp"); // in case app developer wants to give customized referrer instead the default referrer in the playmanifest
                    
            log.d("Player: " + player.getClass());
            addPlayerListeners(progressBar);



            controlsView = (PlaybackControlsView) this.findViewById(R.id.playerControls);
            controlsView.setPlayer(player);
            initSpinners();
        }

        player.prepare(mediaConfig);

        player.play();
    }

    private void initSpinners() {
        videoSpinner = (Spinner) this.findViewById(R.id.videoSpinner);
        audioSpinner = (Spinner) this.findViewById(R.id.audioSpinner);
        textSpinner = (Spinner) this.findViewById(R.id.subtitleSpinner);

        textSpinner.setOnItemSelectedListener(this);
        audioSpinner.setOnItemSelectedListener(this);
        videoSpinner.setOnItemSelectedListener(this);
    }

    private void configurePlugins(PKPluginConfigs config, FrameLayout layout, RelativeLayout adSkin) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("delay", 1200);
        config.setPluginConfig("Sample", jsonObject);

        //addPhoenixAnalyticsPluginConfig(config);
        //addTVPAPIAnalyticsPluginConfig(config);
        //config.setPluginConfig("KalturaStatistics", jsonObject);
        //config.setPluginConfig("PhoenixAnalytics", jsonObject);
        //config.setPluginConfig("Youbora", jsonObject);

    }

//    private void addPhoenixAnalyticsPluginConfig(PKPluginConfigs config) {
//        String ks = "djJ8MTk4fHFftqeAPxdlLVzZBk0Et03Vb8on1wLsKp7cbOwzNwfOvpgmOGnEI_KZDhRWTS-76jEY7pDONjKTvbWyIJb5RsP4NL4Ng5xuw6L__BeMfLGAktkVliaGNZq9SXF5n2cMYX-sqsXLSmWXF9XN89io7-k=";
//        PhoenixAnalyticsConfig phoenixAnalyticsConfig = new PhoenixAnalyticsConfig(198, "http://api-preprod.ott.kaltura.com/v4_2/api_v3/", ks, 30);
//        config.setPluginConfig(PhoenixAnalyticsPlugin.factory.getName(),phoenixAnalyticsConfig);
//    }
//
//    private void addTVPAPIAnalyticsPluginConfig(PKPluginConfigs config) {
//        TVPAPILocale locale = new TVPAPILocale("","","", "");
//        TVPAPIInitObject tvpapiInitObject = new TVPAPIInitObject(716158, "tvpapi_198", 354531, "e8aa934c-eae4-314f-b6a0-f55e96498786", "11111", "Cellular", locale);
//        TVPAPIAnalyticsConfig tvpapiAnalyticsConfig = new TVPAPIAnalyticsConfig("http://tvpapi-preprod.ott.kaltura.com/v4_2/gateways/jsonpostgw.aspx?", 30, tvpapiInitObject);
//        config.setPluginConfig(TVPAPIAnalyticsPlugin.factory.getName(),tvpapiAnalyticsConfig);
//    }

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

    private void addPlayerListeners(final ProgressBar appProgressBar) {
        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                log.d("received NEW AD_BREAK_STARTED");
            }
        }, AdEvent.Type.AD_BREAK_STARTED);

        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                log.d("received NEW ADS_PLAYBACK_ENDED");
            }
        }, AdEvent.Type.ADS_PLAYBACK_ENDED);

        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                AdEvent.AdRequestedEvent adRequestEvent = (AdEvent.AdRequestedEvent) event;
                log.d("received NEW AD_REQUESTED adtag = " + adRequestEvent.adTagUrl);
            }
        }, AdEvent.Type.AD_REQUESTED);


        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                AdEvent.AdProgressUpdateEvent adPluginEventProress = (AdEvent.AdProgressUpdateEvent) event;
                //log.d("received NEW AD_PROGRESS_UPDATE " + adPluginEventProress.currentPosition + "/" +  adPluginEventProress.duration);
            }
        }, AdEvent.Type.AD_POSITION_UPDATED);


        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                AdEvent.Error adError = (AdEvent.Error) event;
                log.d("received NEW AD_ERROR " + adError.type + " "  + adError.error.message);
            }
        }, AdEvent.Type.ERROR);


        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                log.d("AD_BREAK_STARTED");
                appProgressBar.setVisibility(View.VISIBLE);
            }
        }, AdEvent.Type.AD_BREAK_STARTED);

        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                AdEvent.AdCuePointsChangedEvent cuePointsList = (AdEvent.AdCuePointsChangedEvent) event;
                AdCuePoints adCuePoints = cuePointsList.adCuePoints;
                if (adCuePoints != null) {
                    log.d("Has Postroll = " + adCuePoints.hasPostRoll());
                }
            }
        }, AdEvent.Type.AD_CUEPOINTS_UPDATED);

        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                log.d("AD_STARTED");
                appProgressBar.setVisibility(View.INVISIBLE);
            }
        }, AdEvent.Type.AD_STARTED);

        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                log.d("Ad Event AD_RESUMED");
                nowPlaying = true;
                appProgressBar.setVisibility(View.INVISIBLE);
            }
        }, AdEvent.Type.AD_RESUMED);
        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                log.d("Ad Event ALL_ADS_COMPLETED");
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
                AdEvent.AdEndedEvent adEndedEvent = (AdEvent.AdEndedEvent) event;
                if (adEndedEvent.adEndedReason == PKAdEndedReason.SKIPPED) {
                    log.d("Ad Event SKIPPED");
                    nowPlaying = true;
                } else if (adEndedEvent.adEndedReason == PKAdEndedReason.COMPLETED) {
                    log.d("Ad Event AD_ENDED");
                }
            }
        }, AdEvent.Type.AD_ENDED);

        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                log.d("Ad Event CLICKED");
                nowPlaying = true;
            }
        }, AdEvent.Type.AD_CLICKED);

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
                populateSpinnersWithTrackInfo(tracksAvailable.tracksInfo);

            }
        }, PlayerEvent.Type.TRACKS_AVAILABLE);

        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                //When the track data available, this event occurs. It brings the info object with it.
                PlayerEvent.PlayheadUpdated playheadUpdated = (PlayerEvent.PlayheadUpdated) event;
                //log.d("playheadUpdated event  position = " + playheadUpdated.position + " duration = " + playheadUpdated.duration);
            }
        }, PlayerEvent.Type.PLAYHEAD_UPDATED);
    }

    @Override
    protected void onResume() {
        log.d("Ad Event onResume");
        super.onResume();
        if (player != null) {
            player.onApplicationResumed();
            if (nowPlaying && AUTO_PLAY_ON_RESUME) {
                player.play();
            }
        }
        if (controlsView != null) {
            controlsView.resume();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.v("orientation", "state = "+newConfig.orientation);

        super.onConfigurationChanged(newConfig);
        setFullScreen(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE);
    }

    private void setFullScreen(boolean isFullScreen) {
        if (player != null) {
            player.getAdController().screenOrientationChanged(isFullScreen);
        }
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)playerContainer.getLayoutParams();
        // Checks the orientation of the screen
        this.isFullScreen = isFullScreen;
        if (isFullScreen) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            fullScreenBtn.setImageResource(R.drawable.ic_no_fullscreen);
            spinerContainer.setVisibility(View.GONE);
            params.height = RelativeLayout.LayoutParams.MATCH_PARENT;
            params.width = RelativeLayout.LayoutParams.MATCH_PARENT;

        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            fullScreenBtn.setImageResource(R.drawable.ic_fullscreen);
            spinerContainer.setVisibility(View.VISIBLE);
            params.height = (int)getResources().getDimension(R.dimen.player_height);
            params.width = RelativeLayout.LayoutParams.MATCH_PARENT;
        }
        playerContainer.requestLayout();
    }

    /**
     * populating spinners with track info.
     *
     * @param tracksInfo - the track info.
     */
    private void populateSpinnersWithTrackInfo(PKTracks tracksInfo) {

        //Retrieve info that describes available tracks.(video/audio/subtitle).
        TrackItem[] videoTrackItems = obtainRelevantTrackInfo(Consts.TRACK_TYPE_VIDEO, tracksInfo.getVideoTracks());
        //populate spinner with this info.
        applyAdapterOnSpinner(videoSpinner, videoTrackItems);

        TrackItem[] audioTrackItems = obtainRelevantTrackInfo(Consts.TRACK_TYPE_AUDIO, tracksInfo.getAudioTracks());
        applyAdapterOnSpinner(audioSpinner, audioTrackItems);

        TrackItem[] subtitlesTrackItems = obtainRelevantTrackInfo(Consts.TRACK_TYPE_TEXT, tracksInfo.getTextTracks());
        applyAdapterOnSpinner(textSpinner, subtitlesTrackItems);
    }


    /**
     * Obtain info that user is interested in.
     * For example if user want to display in UI bitrate of the available tracks,
     * he can do it, by obtaining the tackType of video, and getting the getBitrate() from videoTrackInfo.
     *
     * @param trackType  - tyoe of the track you are interested in.
     * @param trackInfos - all availables tracks.
     * @return
     */
    private TrackItem[] obtainRelevantTrackInfo(int trackType, List<? extends BaseTrack> trackInfos) {
        TrackItem[] trackItems = new TrackItem[trackInfos.size()];
        switch (trackType) {
            case Consts.TRACK_TYPE_VIDEO:
                TextView tvVideo = (TextView) this.findViewById(R.id.tvVideo);
                changeSpinnerVisibility(videoSpinner, tvVideo, trackInfos);

                for (int i = 0; i < trackInfos.size(); i++) {
                    VideoTrack videoTrackInfo = (VideoTrack) trackInfos.get(i);
                    if(videoTrackInfo.isAdaptive()){
                        trackItems[i] = new TrackItem("Auto", videoTrackInfo.getUniqueId());
                    } else {
                        trackItems[i] = new TrackItem(String.valueOf(videoTrackInfo.getBitrate()), videoTrackInfo.getUniqueId());
                    }
                }

                break;
            case Consts.TRACK_TYPE_AUDIO:
                TextView tvAudio = (TextView) this.findViewById(R.id.tvAudio);
                changeSpinnerVisibility(audioSpinner, tvAudio, trackInfos);

                for (int i = 0; i < trackInfos.size(); i++) {
                    AudioTrack audioTrackInfo = (AudioTrack) trackInfos.get(i);
                    if(audioTrackInfo.isAdaptive()){
                        trackItems[i] = new TrackItem("Auto", audioTrackInfo.getUniqueId());
                    }else{
                        String label = audioTrackInfo.getLanguage() != null ? audioTrackInfo.getLanguage() : audioTrackInfo.getLabel();
                        String bitrate = (audioTrackInfo.getBitrate() >  0)? "" + audioTrackInfo.getBitrate() : "";
                        trackItems[i] = new TrackItem(label + " " + bitrate, audioTrackInfo.getUniqueId());
                    }
                }
                break;
            case Consts.TRACK_TYPE_TEXT:
                TextView tvSubtitle = (TextView) this.findViewById(R.id.tvText);
                changeSpinnerVisibility(textSpinner, tvSubtitle, trackInfos);

                for (int i = 0; i < trackInfos.size(); i++) {

                    TextTrack textTrackInfo = (TextTrack) trackInfos.get(i);
                    String lang = (textTrackInfo.getLanguage() != null) ? textTrackInfo.getLanguage() : "unknown";
                    trackItems[i] = new TrackItem(lang, textTrackInfo.getUniqueId());
                }
                break;
        }
        return trackItems;
    }

    private void changeSpinnerVisibility(Spinner spinner, TextView textView, List<? extends BaseTrack> trackInfos) {
        //hide spinner if no data available.
        if (trackInfos.isEmpty()) {
            textView.setVisibility(View.GONE);
            spinner.setVisibility(View.GONE);
        } else {
            textView.setVisibility(View.VISIBLE);
            spinner.setVisibility(View.VISIBLE);
        }
    }

    private void applyAdapterOnSpinner(Spinner spinner, TrackItem[] trackInfo) {
        TrackItemAdapter trackItemAdapter = new TrackItemAdapter(this, R.layout.track_items_list_row, trackInfo);
        spinner.setAdapter(trackItemAdapter);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        TrackItem trackItem = (TrackItem) parent.getItemAtPosition(position);
        //tell to the player, to switch track based on the user selection.
        player.changeTrack(trackItem.getUniqueId());
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onOrientationChange(OrientationManager.ScreenOrientation screenOrientation) {
        switch(screenOrientation){
            case PORTRAIT:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case REVERSED_PORTRAIT:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                break;
            case LANDSCAPE:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            case REVERSED_LANDSCAPE:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                break;
            default:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
        }
    }
}
