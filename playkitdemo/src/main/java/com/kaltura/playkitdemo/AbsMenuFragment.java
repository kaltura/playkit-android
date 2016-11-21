package com.kaltura.playkitdemo;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kaltura.playkitdemo.data.CardData;

import java.util.ArrayList;

import static android.content.ContentValues.TAG;

/**
 * Created by itanbarpeled on 14/11/2016.
 */

public abstract class AbsMenuFragment extends Fragment {



    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private LinearLayoutManager mLayoutManager;




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View fragmentView = inflater.inflate(getLayoutID(), container, false);
        mRecyclerView = (RecyclerView) fragmentView.findViewById(R.id.recycler_view);

        return fragmentView;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setRecyclerView();
    }


    abstract protected int getLayoutID();


    protected void setRecyclerView() {


        mLayoutManager = new LinearLayoutManager(getContext());
        mAdapter = getAdapter();


        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(), mLayoutManager.getOrientation());
            dividerItemDecoration.setDrawable(ContextCompat.getDrawable(getContext(), getDivider()));
            mRecyclerView.addItemDecoration(dividerItemDecoration);
        }
    }



    abstract protected int getDivider();


    abstract protected ArrayList getDataSet();


    abstract protected RecyclerView.Adapter getAdapter();


    MenuRecyclerAdapter.MenuClickListener mMenuClickListener = new MenuRecyclerAdapter.MenuClickListener() {
        @Override
        public void onItemClick(int position, View v) {
            Log.v(TAG, "TabletMenuActivity position " + position);
        }
    };

}
