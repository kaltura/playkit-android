package com.kaltura.playkit.basicplay;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.kaltura.netkit.connect.response.ResultElement;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.api.ovp.SimpleOvpSessionProvider;
import com.kaltura.playkit.mediaproviders.base.OnMediaLoadCompletion;
import com.kaltura.playkit.mediaproviders.ovp.KalturaOvpMediaProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    private Player player;
//    private List<PKMediaEntry> items;
    private PKMediaEntry[] entries = {
        entry("1_q81a5nbp", 2222401),
        entry("Sintel Short (dash)", "http://cdnapi.kaltura.com/p/2215841/playManifest/entryId/1_9bwuo813/format/mpegdash/protocol/http/a.mpd"),
        entry("Sintel Full (dash)", "http://cdnapi.kaltura.com/p/2215841/playManifest/entryId/1_w9zx2eti/format/mpegdash/protocol/http/a.mpd"),
        entry("Kaltura (HLS)", "http://cdnapi.kaltura.com/p/243342/sp/24334200/playManifest/entryId/1_sf5ovm7u/format/applehttp/protocol/http/a.m3u8"),
    };

    private PKMediaEntry entry(String id, String url) {
        return new PKMediaEntry().setId(id).setSources(Collections.singletonList(new PKMediaSource().setUrl(url)));
    }

    private PKMediaEntry entry(String id, String url, String licenseUrl, PKDrmParams.Scheme scheme) {
        PKMediaSource source = new PKMediaSource().setUrl(url);
        source.setDrmData(Collections.singletonList(new PKDrmParams(licenseUrl, scheme)));
        return new PKMediaEntry().setId(id).setSources(Collections.singletonList(source));
    }
    
    private PKMediaEntry entry(String id, int partnerId) {
        final CountDownLatch latch = new CountDownLatch(1);
        final PKMediaEntry[] entryHolder = new PKMediaEntry[1];
        new KalturaOvpMediaProvider()
                .setSessionProvider(new SimpleOvpSessionProvider("http://cdnapi.kaltura.com", partnerId, null))
                .setEntryId(id)
                .load(new OnMediaLoadCompletion() {
                    @Override
                    public void onComplete(ResultElement<PKMediaEntry> response) {
                        if (response.isSuccess()) {
                            entryHolder[0] = response.getResponse();
                        } else {
                            Log.e("error", response.getError().toString());
                        }
                        latch.countDown();
                    }
                });
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        final PKMediaEntry entry = entryHolder[0];

        return entry;
    }

    private void selectEntry() {
        List<String> names = new ArrayList<>();
        for (PKMediaEntry entry : entries) {
            names.add(entry.getId());
        }
        
        new AlertDialog.Builder(this).setItems(names.toArray(new String[names.size()]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                player.prepare(new PKMediaConfig().setMediaEntry(entries[which]));
            }
        }).show();
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        player = PlayKitManager.loadPlayer(this, null);
        ViewGroup container = (ViewGroup) findViewById(R.id.player_container);
        container.addView(player.getView());
        
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectEntry();
            }
        });
        
        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                switch (((PlayerEvent) event).type) {
                    case CAN_PLAY:
                        Snackbar.make(toolbar, "Ready to play", BaseTransientBottomBar.LENGTH_SHORT).show();
                        break;
                    case PLAYHEAD_UPDATED:
                        long position = ((PlayerEvent.PlayheadUpdated) event).position;
                        ((Button) findViewById(R.id.txt_pos)).setText(String.format("%.1f", position/1000d));
                        break;
                    case SOURCE_SELECTED:
                        final PKMediaSource source = ((PlayerEvent.SourceSelected) event).source;
                        Log.d("source selected", "URL: " + source.getUrl());
                        break;
                }
            }
        }, PlayerEvent.Type.PLAYHEAD_UPDATED, PlayerEvent.Type.CAN_PLAY, PlayerEvent.Type.SOURCE_SELECTED);
        
        findViewById(R.id.btn_play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.play();
            }
        });
        findViewById(R.id.btn_pause).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.pause();
            }
        });
        findViewById(R.id.btn_bwd).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.seekTo(player.getCurrentPosition()-10000);
            }
        });
        findViewById(R.id.btn_fwd).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.seekTo(player.getCurrentPosition()+10000);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
