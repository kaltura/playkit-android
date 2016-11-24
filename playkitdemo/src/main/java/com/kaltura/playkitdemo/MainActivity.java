package com.kaltura.playkitdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.TrackData;
import com.kaltura.playkit.backend.base.OnMediaLoadCompletion;
import com.kaltura.playkit.backend.mock.MockMediaProvider;
import com.kaltura.playkit.connect.ResultElement;
import com.kaltura.playkit.plugins.SamplePlugin;


public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {


    public static final boolean AUTO_PLAY_ON_RESUME = false;

    private static final String TAG = "MainActivity";
    private static final int TRACK_TYPE_UNKNOWN = -1;
    private static final int TRACK_TYPE_VIDEO = 0;
    private static final int TRACK_TYPE_AUDIO = 1;
    private static final int TRACK_TYPE_CC = 2;

    private Player player;
    private MediaEntryProvider mediaProvider;
    private PlaybackControlsView controlsView;
    private boolean nowPlaying;

    private Spinner videoSpinner, audioSpinner, ccSpiner;

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
                            Log.e(TAG, "failed to fetch media data: " + (response.getError() != null ? response.getError().getMessage() : ""));
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

            Log.d(TAG, "Player: " + player.getClass());
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
        ccSpiner = (Spinner) this.findViewById(R.id.ccSpinner);
        ccSpiner.setOnItemSelectedListener(this);
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
        player.release();
    }

    private void addPlayerListeners() {
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

        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {

                TrackData trackData = player.getTrackData();
                String[] videoData = new String[trackData.getVideoTrackData().size()];

                for (int i = 0; i < videoData.length; i++) {
                    videoData[i] = String.valueOf(trackData.getVideoTrackData().get(i).getId());
                }
                videoSpinner.setAdapter(new ArrayAdapter<>(getApplicationContext(),
                        android.R.layout.simple_spinner_item, videoData));

                String[] audioData = new String[trackData.getAudioTrackData().size()];

                for (int i = 0; i < audioData.length; i++) {
                    audioData[i] = String.valueOf(trackData.getAudioTrackData().get(i).getId());
                }
                audioSpinner.setAdapter(new ArrayAdapter<>(getApplicationContext(),
                        android.R.layout.simple_spinner_item, audioData));

                String[] ccData = new String[trackData.getSubtitleTrackData().size()];

                for (int i = 0; i < ccData.length; i++) {
                    ccData[i] = String.valueOf(trackData.getSubtitleTrackData().get(i).getId());
                }
                ccSpiner.setAdapter(new ArrayAdapter<>(getApplicationContext(),
                        android.R.layout.simple_spinner_item, ccData));
            }
        }, PlayerEvent.TRACKS_AVAILABLE);


        player.addStateChangeListener(new PlayerState.Listener() {
            @Override
            public void onPlayerStateChanged(Player player, PlayerState newState) {
                if (controlsView != null) {
                    controlsView.setPlayerState(newState);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) {
            player.restore();
            if (nowPlaying && AUTO_PLAY_ON_RESUME) {
                player.play();
            }
        }
        if (controlsView != null) {
            controlsView.resume();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        int trackType = TRACK_TYPE_UNKNOWN;

        switch (parent.getId()) {
            case R.id.videoSpinner:
                trackType = TRACK_TYPE_VIDEO;
                break;
            case R.id.audioSpinner:
                trackType = TRACK_TYPE_AUDIO;
                break;
            case R.id.ccSpinner:
                trackType = TRACK_TYPE_CC;
                break;

        }

        Log.e(TAG, "on item selected " + parent.getItemAtPosition(position).toString());
        player.changeTrack(trackType, position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
