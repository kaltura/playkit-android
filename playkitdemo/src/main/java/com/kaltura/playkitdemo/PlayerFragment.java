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

import org.json.JSONException;
import org.json.JSONObject;

import static android.content.ContentValues.TAG;
import static com.kaltura.playkitdemo.MockParams.Format;
import static com.kaltura.playkitdemo.MockParams.MediaId;


public class PlayerFragment extends Fragment {



    private MockMediaProvider mockProvider;
    private PhoenixMediaProvider phoenixMediaProvider;
    private Player mPlayer;

    private boolean mIsCardExpanded;
    private TextView mCardTitleView;
    private RelativeLayout mFeatureTitleContainer;
    private LinearLayout mPlayerContainer;
    //private PlaybackControlsView controlsView;


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
        //mockProvider = new MockMediaProvider("mock/entries.playkit.json", getContext(), "1_1h1vsv3z");
        phoenixMediaProvider = new PhoenixMediaProvider(MockParams.sessionProvider, MediaId, MockParams.MediaType, Format);
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

        /*
        mockProvider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                if (response.isSuccess()) {
                    onMediaLoaded(response.getResponse());
                }
            }

        });
        */


        phoenixMediaProvider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(final ResultElement<PKMediaEntry> response) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (response.isSuccess()) {
                            onMediaLoaded(response.getResponse());
                        } else {

                            Toast.makeText(getContext(), "failed to fetch media data: " + (response.getError() != null ? response.getError().getMessage() : ""), Toast.LENGTH_LONG).show();
                            Log.e(TAG, "failed to fetch media data: " + (response.getError() != null ? response.getError().getMessage() : ""));
                        }
                    }
                });
            }
        });
    }


    private void onMediaLoaded(PKMediaEntry mediaEntry){

        PlayerConfig config = new PlayerConfig();

        config.media.setMediaEntry(mediaEntry);
        //config.media.setAutoPlay(true);


        if(mPlayer == null){

            configurePlugins(config.plugins);


            mPlayer = PlayKitManager.loadPlayer(config, getContext());
            mPlayer.play();

            Log.d(TAG, "Player: " + mPlayer.getClass());
            addPlayerListeners();

            mPlayerContainer.addView(mPlayer.getView());

            /*
            controlsView = (PlaybackControlsView) this.findViewById(R.id.playerControls);
            controlsView.setPlayer(mPlayer);
            */
        }else {
            mPlayer.prepare(config.media);
        }

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
    public void onResume() {
        super.onResume();
        if(mPlayer != null){
            mPlayer.restore();
            /*
            if (nowPlaying) {
                mPlayer.play();
            }
            */
        }

        /*
        if(controlsView != null){
            controlsView.resume();
        }
        */
    }


    @Override
    public void onStop() {
        super.onStop();

        //controlsView.release();
        mPlayer.release();
    }


    private void addPlayerListeners() {
        mPlayer.addEventListener(new PlayerEvent.Listener() {
            @Override
            public void onPlayerEvent(Player player, PlayerEvent event) {

            }
        }, PlayerEvent.DURATION_CHANGE, PlayerEvent.CAN_PLAY);

        mPlayer.addEventListener(new PlayerEvent.Listener() {
            @Override
            public void onPlayerEvent(Player player, PlayerEvent event) {
                switch (event) {
                    case PLAY:
                        //nowPlaying = true;
                        break;
                    case PAUSE:
                        //nowPlaying = false;
                        break;
                }
            }
        }, PlayerEvent.PLAYING);

        mPlayer.addStateChangeListener(new PlayerState.Listener() {
            @Override
            public void onPlayerStateChanged(Player player, PlayerState newState) {
                /*
                if(controlsView != null){
                    controlsView.setPlayerState(newState);
                }
                */
            }
        });
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

