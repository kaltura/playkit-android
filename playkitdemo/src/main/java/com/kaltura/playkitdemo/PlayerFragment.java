package com.kaltura.playkitdemo;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.Utils;
import com.kaltura.playkit.backend.base.OnMediaLoadCompletion;
import com.kaltura.playkit.backend.mock.MockMediaProvider;
import com.kaltura.playkit.backend.phoenix.PhoenixMediaProvider;
import com.kaltura.playkit.connect.ResultElement;
import com.kaltura.playkit.plugins.SamplePlugin;
import com.kaltura.playkitdemo.jsonConverters.ConverterSubMenu;

import org.json.JSONException;
import org.json.JSONObject;

import static android.R.attr.configure;
import static android.content.ContentValues.TAG;
import static com.kaltura.playkitdemo.MockParams.Format;
import static com.kaltura.playkitdemo.MockParams.MediaId;
import static com.kaltura.playkitdemo.MockParams.MediaId3;
import static com.kaltura.playkitdemo.MockParams.sessionProvider;


public class PlayerFragment extends Fragment {


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

        configurePlayer(mConverterSubMenu.getFeatureVariants().get(0).getPlayerConfigLink());

    }


    // TODO duplication of code with StandalonePlayerActivity
    private void configurePlayer(String playerConfigLink) {

        PlayerProvider.getPlayer(playerConfigLink, getActivity(), new PlayerProvider.OnPlayerReadyListener() {
            @Override
            public void onPlayerRead(Player player) {
                startPlay(player);
            }
        });
    }


    private void startPlay(Player player) {

        mPlayerContainer.addView(player.getView());
        mControlsView.setPlayer(player);
        mControlsView.resume();

        player.play();
    }


}

