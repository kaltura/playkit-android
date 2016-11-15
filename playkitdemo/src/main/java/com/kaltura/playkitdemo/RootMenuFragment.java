package com.kaltura.playkitdemo;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.ArrayList;


public class RootMenuFragment extends AbsMenuFragment {


    /*
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    */

    private OnRootMenuInteractionListener mListener;



    public RootMenuFragment() {
        // Required empty public constructor
    }


    public static RootMenuFragment newInstance(/*String param1, String param2*/) {
        RootMenuFragment fragment = new RootMenuFragment();
        Bundle args = new Bundle();
        /*
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        */
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            /*
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
            */
        }
    }

    /*
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View fragmentView = inflater.inflate(R.layout.fragment_root_menu, container, false);
        mRecyclerView = (RecyclerView) fragmentView.findViewById(R.id.recycler_view);

        return fragmentView;
    }
    */


    @Override
    protected int getLayoutID() {
        return R.layout.fragment_root_menu;
    }


    /*
    @Override
    public void onActivityCreated (Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        setRecyclerView();
    }
    */


    /*
    private void setRecyclerView() {

        mLayoutManager = new LinearLayoutManager(getContext());
        mAdapter = new MenuRecyclerAdapter(getDataSet(), mMenuClickListener, true);


        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        // TODO - return back after upgrading to 25.0.0
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(), mLayoutManager.getOrientation());
        dividerItemDecoration.setDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.divider_big));
        mRecyclerView.addItemDecoration(dividerItemDecoration);

    }
    */



    /*
    MenuRecyclerAdapter.MenuClickListener mMenuClickListener = new MenuRecyclerAdapter.MenuClickListener() {
        @Override
        public void onItemClick(int position, View v) {
            Log.v(AppMainActivity.TAG, "RootMenuFragment position " + position);
        }
    };
    */


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
    protected ArrayList<CardData> getDataSet() {
        String[] titles = new String[] {"Playback options", "Monetization", "Cast", "Analytics"};
        ArrayList<CardData> results = new ArrayList<>();
        for (int index = 0; index < titles.length; index++) {
            results.add(new CardData(titles[index]));
        }
        return results;
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
    public interface OnRootMenuInteractionListener {
        void onRootMenuInteraction(int  position);
    }
}
