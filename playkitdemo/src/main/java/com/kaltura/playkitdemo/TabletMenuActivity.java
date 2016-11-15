package com.kaltura.playkitdemo;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;


@Deprecated
public class TabletMenuActivity extends AppCompatActivity {


    private static String TAG = "DEMO";


    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private LinearLayoutManager mLayoutManager;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_expandable_menu);

        setRecyclerView();
    }


    private void setRecyclerView() {

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mLayoutManager = new LinearLayoutManager(this);
        mAdapter = new ExpandableMenuRecyclerAdapter(getDataSet(), mMenuClickListener);


        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        // TODO - return back after upgrading to 25.0.0
        /*
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(), mLayoutManager.getOrientation());
        dividerItemDecoration.setDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.divider_big));
        mRecyclerView.addItemDecoration(dividerItemDecoration);
        */

    }


    MenuRecyclerAdapter.MenuClickListener mMenuClickListener = new MenuRecyclerAdapter.MenuClickListener() {
        @Override
        public void onItemClick(int position, View v) {
            Log.v(TAG, "TabletMenuActivity position " + position);
        }
    };


    public void handleMoreInfoClick(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.more_info_url)));
        startActivity(browserIntent);
    }


    private ArrayList<CardData> getDataSet() {
        String[] titles = new String[] {"Playback options", "Monetization", "Cast", "Analytics"};
        ArrayList<CardData> results = new ArrayList<>();
        for (int index = 0; index < titles.length; index++) {
            results.add(new CardData(titles[index]));
        }
        return results;
    }
}
