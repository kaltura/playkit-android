package com.kaltura.playkitdemo;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.kaltura.playkit.Player;
import com.kaltura.playkitdemo.jsonConverters.ConverterSubMenu;


public class PlayerFragment extends Fragment {


    private Player mPlayer;
    private PlaybackControlsView mControlsView;
    private LinearLayout mPlayerContainer;


    private ConverterSubMenu mConverterSubMenu;

    private boolean mIsCardExpanded;
    private RelativeLayout mFeatureTitleContainer;
    private TextView mCardTitleView;
    private TextView mFeatureTitle;
    private TextView mFeatureDescription;



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
        mCardTitleView = (TextView) fragmentView.findViewById(R.id.test_test);
        mFeatureTitleContainer = (RelativeLayout) fragmentView.findViewById(R.id.feature_title_container);
        mPlayerContainer = (LinearLayout) fragmentView.findViewById(R.id.player_container);
        mFeatureTitle = (TextView) fragmentView.findViewById(R.id.feature_title);
        mFeatureDescription = (TextView) fragmentView.findViewById(R.id.feature_description);
        mControlsView = (PlaybackControlsView) fragmentView.findViewById(R.id.playerControls);

        mFeatureTitleContainer.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                mCardTitleView.setVisibility(mIsCardExpanded ? View.GONE :View.VISIBLE);
                mIsCardExpanded = !mIsCardExpanded;

            }
        });

        return fragmentView;
    }



    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mFeatureTitle.setText(mConverterSubMenu.getSubMenuTitle());
        mFeatureDescription.setText(mConverterSubMenu.getAboutFeature());

        configurePlayer(mConverterSubMenu.getFeatureVariants().get(0).getPlayerConfigURL());

    }


    // TODO duplication of code with StandalonePlayerActivity
    private void configurePlayer(String playerConfigLink) {

        PlayerProvider.getPlayer(playerConfigLink, getActivity(), new PlayerProvider.OnPlayerReadyListener() {
            @Override
            public void onPlayerRead(Player player) {
                mPlayer = player;
                startPlay();
            }
        });
    }


    private void startPlay() {

        if (mPlayer == null) {
            return;
        }

        mPlayerContainer.addView(mPlayer.getView());

        mControlsView.setPlayer(mPlayer);
        mControlsView.resume();

        //mPlayer.play();
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

        if (mPlayer != null) {
            mPlayer.release();
            mControlsView.release();
        }
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

