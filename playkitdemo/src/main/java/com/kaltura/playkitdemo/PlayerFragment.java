package com.kaltura.playkitdemo;

import android.content.Context;
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

import com.kaltura.playkit.MockMediaEntryProvider;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.Utils;
import com.kaltura.playkit.connect.ResultElement;
import com.kaltura.playkit.mediaproviders.base.OnMediaLoadCompletion;
import com.kaltura.playkit.mediaproviders.mock.MockMediaProvider;
import com.kaltura.playkit.plugins.SamplePlugin;

import org.json.JSONException;
import org.json.JSONObject;

import static android.content.ContentValues.TAG;


public class PlayerFragment extends Fragment {



    private MockMediaProvider mockProvider;
    private Player mPlayer;

    private boolean mIsCardExpanded;
    private TextView mCardTitleView;
    private RelativeLayout mFeatureTitleContainer;
    private LinearLayout mPlayerContainer;


    private OnFragmentInteractionListener mListener;


    public PlayerFragment() {
        mIsCardExpanded = false;
    }



    public static PlayerFragment newInstance(/*String param1, String param2*/) {
        PlayerFragment fragment = new PlayerFragment();
        Bundle args = new Bundle();
        /*
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        */
        fragment.setArguments(args);
        return fragment;
    }


    private void registerPlugins() {
        PlayKitManager.registerPlugins(SamplePlugin.factory);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        */


        registerPlugins();
        mockProvider = new MockMediaProvider("mock/entries.playkit.json", getContext(), "1_1h1vsv3z");
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View fragmentView = inflater.inflate(R.layout.fragment_player, container, false);
        mCardTitleView = (TextView) fragmentView.findViewById(R.id.test_test);
        mFeatureTitleContainer = (RelativeLayout) fragmentView.findViewById(R.id.feature_title_container);
        mPlayerContainer = (LinearLayout) fragmentView.findViewById(R.id.player_container);


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
    public void onAttach(Context context) {
        super.onAttach(context);

        /*
        if (context instanceof OnRootMenuInteractionListener) {
            mListener = (OnRootMenuInteractionListener) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement OnRootMenuInteractionListener");
        }
        */
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }



    @Override
    public void onStart() {
        super.onStart();

        mockProvider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                if (response.isSuccess()) {
                    onMediaLoaded(response.getResponse());
                }
            }

        });
    }


    private void onMediaLoaded(PKMediaEntry mediaEntry){

        PlayerConfig config = new PlayerConfig();

        config.media.setMediaEntry(mediaEntry);
        configurePlugins(config.plugins);


        mPlayer = PlayKitManager.loadPlayer(config, getContext());

        Log.d(TAG, "Player: " + mPlayer.getClass());

        mPlayer.addEventListener(new PlayerEvent.Listener() {
            @Override
            public void onPlayerEvent(Player player, PlayerEvent event) {

            }
        }, PlayerEvent.DURATION_CHANGE, PlayerEvent.CAN_PLAY);

        mPlayer.addStateChangeListener(new PlayerState.Listener() {
            @Override
            public void onPlayerStateChanged(Player player, PlayerState newState) {

            }
        });


        mPlayerContainer.addView(mPlayer.getView());
        mPlayer.play();

    }


    private void configurePlugins(PlayerConfig.Plugins config) {
        /*
        try {
            config.setPluginConfig("Sample", new JSONObject().put("delay", 4200));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        */
    }



    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    @Override
    public void onStop() {
        super.onStop();

        mPlayer.release();
    }


    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

}

