package com.kaltura.playkitdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.kaltura.playkit.AudioTrackInfo;
import com.kaltura.playkit.BaseTrackInfo;
import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.TextTrackInfo;
import com.kaltura.playkit.TracksInfo;
import com.kaltura.playkit.VideoTrackInfo;
import com.kaltura.playkit.backend.base.OnMediaLoadCompletion;
import com.kaltura.playkit.backend.mock.MockMediaProvider;
import com.kaltura.playkit.connect.ResultElement;
import com.kaltura.playkit.plugins.KalturaStatsPlugin;
import com.kaltura.playkit.plugins.PhoenixAnalyticsPlugin;
import com.kaltura.playkit.utils.Consts;

import java.util.List;


public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {


    public static final boolean AUTO_PLAY_ON_RESUME = true;

    private static final PKLog log = PKLog.get("MainActivity");

    private Player player;
    private MediaEntryProvider mediaProvider;
    private PlaybackControlsView controlsView;
    private boolean nowPlaying;

    private Spinner videoSpinner, audioSpinner, textSpinner;

    private void registerPlugins() {
        PlayKitManager.registerPlugins(KalturaStatsPlugin.factory, PhoenixAnalyticsPlugin.factory);
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
                            log.e("failed to fetch media data: " + (response.getError() != null ? response.getError().getMessage() : ""));
                        }
                    }
                });
            }
        });

    }

    private void onMediaLoaded(PKMediaEntry mediaEntry) {

        PlayerConfig config = new PlayerConfig();

        config.media.setMediaEntry(mediaEntry);
        config.media.setStartPosition(30000);
        if (player == null) {

            configurePlugins(config.plugins);

            player = PlayKitManager.loadPlayer(config, this);

            log.d("Player: " + player.getClass());
            addPlayerListeners();

            LinearLayout layout = (LinearLayout) findViewById(R.id.player_root);
            layout.addView(player.getView());

            controlsView = (PlaybackControlsView) this.findViewById(R.id.playerControls);
            controlsView.setPlayer(player);
            initSpinners();
        }
        player.prepare(config.media);
    }

    private void initSpinners() {
        videoSpinner = (Spinner) this.findViewById(R.id.videoSpinner);
        audioSpinner = (Spinner) this.findViewById(R.id.audioSpinner);
        textSpinner = (Spinner) this.findViewById(R.id.subtitleSpinner);

        textSpinner.setOnItemSelectedListener(this);
        audioSpinner.setOnItemSelectedListener(this);
        videoSpinner.setOnItemSelectedListener(this);
    }

    private void configurePlugins(PlayerConfig.Plugins config) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("delay", 4200);
        config.setPluginConfig("KalturaStatistics", jsonObject);
        config.setPluginConfig("PhoenixAnalytics", jsonObject);
        config.setPluginConfig("Youbora", jsonObject);
    }

    @Override
    protected void onPause() {
        super.onPause();
        controlsView.release();
        player.onApplicationPaused();
    }

    private void addPlayerListeners() {
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
                populateSpinnersWithTrackInfo(tracksAvailable.getTracksInfo());

            }
        }, PlayerEvent.Type.TRACKS_AVAILABLE);
    }


    @Override
    protected void onResume() {
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

    /**
     * populating spinners with track info.
     *
     * @param tracksInfo - the track info.
     */
    private void populateSpinnersWithTrackInfo(TracksInfo tracksInfo) {

        //Retrieve info that describes available tracks.(video/audio/subtitle).
        TrackItem[] videoTrackItems = obtainRelevantTrackInfo(Consts.TRACK_TYPE_VIDEO, tracksInfo.getVideoTrackInfo());
        //populate spinner with this info.
        applyAdapterOnSpinner(videoSpinner, videoTrackItems);

        TrackItem[] audioTrackItems = obtainRelevantTrackInfo(Consts.TRACK_TYPE_AUDIO, tracksInfo.getAudioTrackInfo());
        applyAdapterOnSpinner(audioSpinner, audioTrackItems);


        TrackItem[] subtitlesTrackItems = obtainRelevantTrackInfo(Consts.TRACK_TYPE_TEXT, tracksInfo.getTextTrackInfo());
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
    private TrackItem[] obtainRelevantTrackInfo(int trackType, List<BaseTrackInfo> trackInfos) {
        TrackItem[] trackItems = new TrackItem[trackInfos.size()];
        switch (trackType) {
            case Consts.TRACK_TYPE_VIDEO:
                TextView tvVideo = (TextView) this.findViewById(R.id.tvVideo);
                changeSpinnerVisibility(videoSpinner, tvVideo, trackInfos);

                for (int i = 0; i < trackInfos.size(); i++) {
                        VideoTrackInfo videoTrackInfo = (VideoTrackInfo) trackInfos.get(i);
                        if(videoTrackInfo.isAdaptive()){
                            trackItems[i] = new TrackItem("Auto", videoTrackInfo.getUniqueId());
                        }else{
                            trackItems[i] = new TrackItem(String.valueOf(videoTrackInfo.getBitrate()), videoTrackInfo.getUniqueId());
                        }
                }

                break;
            case Consts.TRACK_TYPE_AUDIO:
                TextView tvAudio = (TextView) this.findViewById(R.id.tvAudio);
                changeSpinnerVisibility(audioSpinner, tvAudio, trackInfos);

                for (int i = 0; i < trackInfos.size(); i++) {
                    AudioTrackInfo audioTrackInfo = (AudioTrackInfo) trackInfos.get(i);
                    if(audioTrackInfo.isAdaptive()){
                        trackItems[i] = new TrackItem(audioTrackInfo.getLanguage() + " Auto", audioTrackInfo.getUniqueId());
                    }else{
                        trackItems[i] = new TrackItem(audioTrackInfo.getLanguage() + " " + String.valueOf(audioTrackInfo.getBitrate()), audioTrackInfo.getUniqueId());
                    }
                }
                break;
            case Consts.TRACK_TYPE_TEXT:
                TextView tvSubtitle = (TextView) this.findViewById(R.id.tvText);
                changeSpinnerVisibility(textSpinner, tvSubtitle, trackInfos);

                for (int i = 0; i < trackInfos.size(); i++) {

                    TextTrackInfo textTrackInfo = (TextTrackInfo) trackInfos.get(i);
                    trackItems[i] = new TrackItem(String.valueOf(textTrackInfo.getLanguage()), textTrackInfo.getUniqueId());
                }
                break;
        }
        return trackItems;
    }

    private void changeSpinnerVisibility(Spinner spinner, TextView textView, List<BaseTrackInfo> trackInfos) {
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
}
