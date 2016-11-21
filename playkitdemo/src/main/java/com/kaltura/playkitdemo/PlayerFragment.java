package com.kaltura.playkitdemo;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.kaltura.playkit.Player;
import com.kaltura.playkitdemo.jsonConverters.ConverterFeatureVariants;
import com.kaltura.playkitdemo.jsonConverters.ConverterRootMenu;
import com.kaltura.playkitdemo.jsonConverters.ConverterSubMenu;

import java.util.ArrayList;

import static android.view.View.GONE;


public class PlayerFragment extends Fragment {


    private Player mPlayer;
    private PlaybackControlsView mControlsView;
    private LinearLayout mPlayerContainer;


    private ConverterSubMenu mConverterSubMenu;

    private boolean mIsCardExpanded;
    private RelativeLayout mFeatureTitleContainer;
    private TextView mFeatureTitle;
    private TextView mFeatureDescription;
    private ImageView mVerticalArrow;


    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private LinearLayoutManager mLayoutManager;



    public PlayerFragment() {
        mIsCardExpanded = false;
    }



    public static PlayerFragment newInstance(ConverterSubMenu converterSubMenu) {

        PlayerFragment fragment = new PlayerFragment();
        Bundle args = new Bundle();
        args.putParcelable(MainActivity.CONVERTER_SUB_MENU, converterSubMenu);
        fragment.setArguments(args);

        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = getArguments();
        if (arguments != null) {
            mConverterSubMenu = arguments.getParcelable(MainActivity.CONVERTER_SUB_MENU);
        }

        //getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View fragmentView = inflater.inflate(R.layout.fragment_player, container, false);

        mFeatureTitleContainer = (RelativeLayout) fragmentView.findViewById(R.id.feature_title_container);
        mPlayerContainer = (LinearLayout) fragmentView.findViewById(R.id.player_container);
        mFeatureTitle = (TextView) fragmentView.findViewById(R.id.feature_title);
        mFeatureDescription = (TextView) fragmentView.findViewById(R.id.feature_description);
        mControlsView = (PlaybackControlsView) fragmentView.findViewById(R.id.playerControls);
        mVerticalArrow = (ImageView) fragmentView.findViewById(R.id.vertical_arrow);
        mRecyclerView = (RecyclerView) fragmentView.findViewById(R.id.recycler_view);

        mFeatureTitleContainer.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                mRecyclerView.setVisibility(mIsCardExpanded ? GONE :View.VISIBLE);
                toggleVerticalArrow(mIsCardExpanded);

                mIsCardExpanded = !mIsCardExpanded;

            }
        });

        return fragmentView;
    }


    private void toggleVerticalArrow(boolean isCardExpanded) {
        mVerticalArrow.setImageResource(isCardExpanded ? R.drawable.ic_keyboard_arrow_down_grey_600_24dp : R.drawable.ic_keyboard_arrow_up_grey_600_24dp);
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mFeatureTitle.setText(mConverterSubMenu.getSubMenuTitle());
        mFeatureDescription.setText(mConverterSubMenu.getAboutFeature());

        // the default behaviour is to load the first player configuration
        configurePlayer(0);

        setFeatureContainer();
    }


    @Override
    public void onResume() {
        super.onResume();

        if (mPlayer != null) {
            mPlayer.restore();
            mControlsView.resume();
        }
    }


    @Override
    public void onPause() {
        super.onPause();

        releasePlayer();
    }



    private void releasePlayer() {
        if (mPlayer != null) {
            mPlayer.release();
            mControlsView.release();
        }
    }



    // TODO duplication of code with StandalonePlayerActivity
    private void configurePlayer(int playerIndex) {

        String standalonePlayerUrl = mConverterSubMenu.getFeatureVariants().get(playerIndex).getPlayerConfigURL();

        PlayerProvider.getPlayer(standalonePlayerUrl, getActivity(), new PlayerProvider.OnPlayerReadyListener() {
            @Override
            public void onPlayerReady(Player player) {
                releasePlayer();
                mPlayer = player;
                startPlay();
            }
        });
    }


    private void startPlay() {

        if (mPlayer == null) {
            return;
        }

        mPlayerContainer.removeAllViews();
        mPlayerContainer.addView(mPlayer.getView());

        mControlsView.setPlayer(mPlayer);
        mControlsView.resume();

    }


    private void setFeatureContainer() {

        ArrayList<String> featureVariants = getDataSet();
        Context context = getContext();

        /*
        if there's only one feature variant,
        we don't enable the option to expand the card
         */
        if (featureVariants.size() == 1) {

            mVerticalArrow.setVisibility(View.INVISIBLE);
            mFeatureTitleContainer.setClickable(false);

        } else {

            setRecyclerView(featureVariants, context);
        }
    }



    private void setRecyclerView(ArrayList<String> featureVariants, Context context) {

        mLayoutManager = new LinearLayoutManager(context);


        mAdapter = new SubMenuRecyclerAdapter(featureVariants, new MenuRecyclerAdapter.MenuClickListener() {
            @Override
            public void onItemClick(int position, View v) {
                pausePlayer();
                configurePlayer(position);
            }
        }, R.layout.sub_menu_row_small);


        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(context, mLayoutManager.getOrientation());
            dividerItemDecoration.setDrawable(ContextCompat.getDrawable(context, R.drawable.divider_extra_small));
            mRecyclerView.addItemDecoration(dividerItemDecoration);
        }
    }



    private void pausePlayer() {
        if (mPlayer != null) {
            mPlayer.pause();
        }
    }


    protected ArrayList<String> getDataSet() {

        ArrayList<String> featureVariants = new ArrayList<>();

        for (ConverterFeatureVariants featureVariant : mConverterSubMenu.getFeatureVariants()) {
            featureVariants.add(featureVariant.getFeatureTitle());
        }

        return featureVariants;
    }


    public interface OnFeatureVariantInteractionListener {
        void onFeatureVariantInteraction(int  position);
    }



    /*
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Activity activity = getActivity();

        switch(newConfig.orientation) {

            case Configuration.ORIENTATION_LANDSCAPE:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;

            case Configuration.ORIENTATION_PORTRAIT:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;

            default:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }
    */



}

