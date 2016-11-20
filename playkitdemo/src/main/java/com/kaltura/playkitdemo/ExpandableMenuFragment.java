package com.kaltura.playkitdemo;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.kaltura.playkitdemo.data.CardData;
import com.kaltura.playkitdemo.jsonConverters.ConverterSubMenu;

import java.util.ArrayList;


public class ExpandableMenuFragment extends AbsMenuFragment {


    private static String TAG = "DEMO";


    /*
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    */

    private OnFragmentInteractionListener mListener;



    public ExpandableMenuFragment() {
        // Required empty public constructor
    }


    public static ExpandableMenuFragment newInstance(/*String param1, String param2*/) {
        ExpandableMenuFragment fragment = new ExpandableMenuFragment();
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

    @Override
    protected int getLayoutID() {
        return R.layout.fragment_expandable_menu;
    }

    /*
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View fragmentView = inflater.inflate(R.layout.fragment_expandable_menu, container, false);
        mRecyclerView = (RecyclerView) fragmentView.findViewById(R.id.recycler_view);

        return fragmentView;
    }
    */


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


    /*
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setRecyclerView();
    }
    */

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    /*
    private void setRecyclerView() {

        mLayoutManager = new LinearLayoutManager(getActivity());
        mAdapter = new ExpandableMenuRecyclerAdapter(getDataSet(), mMenuClickListener);


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
            Log.v(TAG, "TabletMenuActivity position " + position);
        }
    };
    */


    public void handleMoreInfoClick(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.more_info_url)));
        startActivity(browserIntent);
    }


    @Override
    protected int getDivider() {
        return R.drawable.divider_big;
    }


    @Override
    protected ArrayList<String> getDataSet() {

        /*
        String[] titles = new String[] {"Playback options", "Monetization", "Cast", "Analytics"};
        ArrayList<CardData> results = new ArrayList<>();
        for (int index = 0; index < titles.length; index++) {
            results.add(new CardData(titles[index]));
        }
        return results;
        */

        ArrayList<String> subMenuTitles = new ArrayList<>();

        /*
        for (ConverterSubMenu subMenu : mConverterRootMenu.getSubMenu()) {
            subMenuTitles.add(subMenu.getSubMenuTitle());
        }
        */

        return subMenuTitles;
    }


    @Override
    protected RecyclerView.Adapter getAdapter() {
        return new ExpandableMenuRecyclerAdapter(getDataSet(), mMenuClickListener);
    }


    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}
