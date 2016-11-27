package com.kaltura.playkitdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import com.kaltura.playkit.SubtitleTrackInfo;
import com.kaltura.playkit.TrackSelectionHelper;
import com.kaltura.playkit.TracksInfo;
import com.kaltura.playkit.VideoTrackInfo;
import com.kaltura.playkit.backend.base.OnMediaLoadCompletion;
import com.kaltura.playkit.backend.mock.MockMediaProvider;
import com.kaltura.playkit.connect.ResultElement;
import com.kaltura.playkit.plugins.SamplePlugin;

import java.util.List;


public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    
    
    public static final boolean AUTO_PLAY_ON_RESUME = true;

    private static final PKLog log = PKLog.get("MainActivity");

    private Player player;
    private MediaEntryProvider mediaProvider;
    private PlaybackControlsView controlsView;
    private boolean nowPlaying;


    private Spinner videoSpinner, audioSpinner, subtitleSpinner;

    private void registerPlugins() {
        PlayKitManager.registerPlugins(SamplePlugin.factory);
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

    private void onMediaLoaded(PKMediaEntry mediaEntry){

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
        subtitleSpinner = (Spinner) this.findViewById(R.id.subtitleSpinner);
        subtitleSpinner.setOnItemSelectedListener(this);
        audioSpinner.setOnItemSelectedListener(this);
        videoSpinner.setOnItemSelectedListener(this);
    }

    private void configurePlugins(PlayerConfig.Plugins config) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("delay", 4200);
        config.setPluginConfig("Sample", jsonObject);
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
                //When the track data available, this event occurs. It brings the info object with it.
                PlayerEvent.TrackAvailable trackAvailable = (PlayerEvent.TrackAvailable) event;
                populateSpinnersWithTrackInfo(trackAvailable.getTracksInfo());

            }
        }, PlayerEvent.Type.TRACKS_AVAILABLE);
    }



    @Override
    protected void onResume() {
        super.onResume();
        if(player != null){
            player.onApplicationResumed();
            if (nowPlaying && AUTO_PLAY_ON_RESUME) {
                player.play();
            }
        }
        if(controlsView != null){
            controlsView.resume();
        }
    }

    /**
     * populating spinners with track info.
     * @param tracksInfo - the track info.
     */
    private void populateSpinnersWithTrackInfo(TracksInfo tracksInfo) {

        //Retrieve info that describes available tracks.(video/audio/subtitle).
        String[] videoData = obtainRelevantTrackInfo(TrackSelectionHelper.TRACK_VIDEO, tracksInfo.getVideoTrackInfo());
        //populate spinner with this info.
        applyAdapterOnSpinner(videoSpinner, videoData);

        String[] audioData = obtainRelevantTrackInfo(TrackSelectionHelper.TRACK_AUDIO, tracksInfo.getAudioTrackInfo());
        applyAdapterOnSpinner(audioSpinner, audioData);

        String[] subtitlesData = obtainRelevantTrackInfo(TrackSelectionHelper.TRACK_SUBTITLE, tracksInfo.getSubtitleTrackInfo());
        applyAdapterOnSpinner(subtitleSpinner, subtitlesData);

    }

    /**
     * Obtain info that user is interested in.
     * For example if user want to display in UI bitrate of the available tracks,
     * he can do it, by obtaining the tackType of video, and getting the getBitrate() from videoTrackInfo.
     * @param trackType - tyoe of the track you are interested in.
     * @param trackInfos - all availables tracks.
     * @return
     */
    private String[] obtainRelevantTrackInfo(int trackType, List<BaseTrackInfo> trackInfos) {
        String[] trackInfo = new String[trackInfos.size()];
        switch (trackType){
            case TrackSelectionHelper.TRACK_VIDEO:
                //run throug all the tracks in this type.
                for (int i = 0; i < trackInfos.size(); i++) {
                    //cast each object to the videoInfo.
                    VideoTrackInfo videoTrackInfo = (VideoTrackInfo) trackInfos.get(i);
                    //obtain the desired info. (uniqueId/bitrate/width/language e.t.c).
                    trackInfo[i] = String.valueOf(videoTrackInfo.getUniqueId());
                }

                break;
            case TrackSelectionHelper.TRACK_AUDIO:
                for (int i = 0; i < trackInfos.size(); i++) {
                    AudioTrackInfo audioTrackInfo = (AudioTrackInfo) trackInfos.get(i);
                    trackInfo[i] = String.valueOf(audioTrackInfo.getUniqueId());
                }
                break;
            case TrackSelectionHelper.TRACK_SUBTITLE:
                for (int i = 0; i < trackInfos.size(); i++) {
                    SubtitleTrackInfo subtitleTrackInfo = (SubtitleTrackInfo) trackInfos.get(i);
                    trackInfo[i] = String.valueOf(subtitleTrackInfo.getUniqueId());
                }
                break;
        }
        return trackInfo;
    }

    private void applyAdapterOnSpinner(Spinner spinner, String[] trackInfo) {
        spinner.setAdapter(new ArrayAdapter<>(getApplicationContext(),
                android.R.layout.simple_spinner_item, trackInfo));
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        int trackType = TrackSelectionHelper.TRACK_TYPE_UNKNOWN;
        //decide which spinner was activated by user.
        switch (parent.getId()) {
            case R.id.videoSpinner:
                trackType = TrackSelectionHelper.TRACK_VIDEO;
                break;
            case R.id.audioSpinner:
                trackType = TrackSelectionHelper.TRACK_AUDIO;
                break;
            case R.id.subtitleSpinner:
                trackType = TrackSelectionHelper.TRACK_SUBTITLE;
                break;

        }

        log.d("track info item selected => " + parent.getItemAtPosition(position).toString());
        //tell to the player, to switch track based on the user selection.
        player.changeTrack(trackType, position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
