package com.kaltura.playkitdemo;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.kaltura.playkitdemo.jsonConverters.ConverterPlayKitApp;
import com.kaltura.playkitdemo.jsonConverters.ConverterRootMenu;

import java.util.ArrayList;


public class RootMenuFragment extends AbsMenuFragment {


    private OnRootMenuInteractionListener mListener;
    private ConverterPlayKitApp mConverterPlayKitApp;



    public RootMenuFragment() {
        // Required empty public constructor
    }


    public static RootMenuFragment newInstance(ConverterPlayKitApp converterPlayKitApp) {

        RootMenuFragment fragment = new RootMenuFragment();

        Bundle args = new Bundle();
        args.putParcelable(SplashActivity.CONVERTER_PLAY_KIT_APP, converterPlayKitApp);
        fragment.setArguments(args);

        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();

        if (bundle != null) {
            mConverterPlayKitApp = bundle.getParcelable(SplashActivity.CONVERTER_PLAY_KIT_APP);
        }
    }



    @Override
    protected int getLayoutID() {
        return R.layout.fragment_root_menu;
    }


    @Override
    protected RecyclerView.Adapter getAdapter() {
        return new MenuRecyclerAdapter(getDataSet(), new MenuRecyclerAdapter.MenuClickListener() {
            @Override
            public void onItemClick(int position, View v) {
                mListener.onRootMenuInteraction(position);
            }
        }, true);
    }


    public void handleMoreInfoClick(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.more_info_url)));
        startActivity(browserIntent);
    }


    @Override
    protected ArrayList<String> getDataSet() {

        ArrayList<String> rootMenuTitles = new ArrayList<>();

        for (ConverterRootMenu rootMenu : mConverterPlayKitApp.getConverterRootMenuList()) {
            rootMenuTitles.add(rootMenu.getRootMenuTitle());
        }

        return rootMenuTitles;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnRootMenuInteractionListener) {
            mListener = (OnRootMenuInteractionListener) context;
        } else {
            throw new ClassCastException(context.toString() + "must implement OnRootMenuInteractionListener");
        }
    }


    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    @Override
    protected int getDivider() {
        return R.drawable.divider_big;
    }


    public interface OnRootMenuInteractionListener {
        void onRootMenuInteraction(int  position);
    }
}
