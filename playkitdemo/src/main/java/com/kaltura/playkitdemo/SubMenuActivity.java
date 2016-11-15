package com.kaltura.playkitdemo;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;


@Deprecated
public class SubMenuActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private LinearLayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_sub_menu);

        setToolbar();
        setRecyclerView();
    }


    private void setRecyclerView() {

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mLayoutManager = new LinearLayoutManager(this);
        mAdapter = new MenuRecyclerAdapter(getDataSet(), mMenuClickListener, false);


        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        // TODO - return back after upgrading to 25.0.0
        /*
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(), mLayoutManager.getOrientation());
        dividerItemDecoration.setDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.divider_small));
        mRecyclerView.addItemDecoration(dividerItemDecoration);
        */

    }



    MenuRecyclerAdapter.MenuClickListener mMenuClickListener = new MenuRecyclerAdapter.MenuClickListener() {
        @Override
        public void onItemClick(int position, View v) {
        }
    };



    private void setToolbar() {

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setTitle("Playback options");
            actionBar.setDisplayShowTitleEnabled(true);
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }




    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.action_info:
                return true;

            case R.id.action_chromecast:
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }


    private ArrayList<CardData> getDataSet() {
        String[] titles = new String[] {"DRM", "Live", "Offline", "Audio Only", "Multi Audio Track",
        "Captions", "Bitrate Selection"};
        ArrayList<CardData> results = new ArrayList<>();
        for (int index = 0; index < titles.length; index++) {
            results.add(new CardData(titles[index]));
        }
        return results;
    }
}
