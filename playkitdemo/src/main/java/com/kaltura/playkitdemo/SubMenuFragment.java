package com.kaltura.playkitdemo;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.ArrayList;


public class SubMenuFragment extends MenuFragment {



    private OnSubMenuInteractionListener mListener;


    public SubMenuFragment() {
        // Required empty public constructor
    }


    public static SubMenuFragment newInstance(/*String param1, String param2*/) {
        SubMenuFragment fragment = new SubMenuFragment();
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
        return R.layout.fragment_sub_menu;
    }



    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnSubMenuInteractionListener) {
            mListener = (OnSubMenuInteractionListener) context;
        } else {
            throw new ClassCastException(context.toString() + " must implement OnRootMenuInteractionListener");
        }

    }


    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    @Override
    protected RecyclerView.Adapter getAdapter() {
        return new MenuRecyclerAdapter(getDataSet(), new MenuRecyclerAdapter.MenuClickListener() {
            @Override
            public void onItemClick(int position, View v) {
                mListener.onSubMenuInteraction(position);
            }
        }, false);
    }




    @Override
    protected ArrayList<CardData> getDataSet() {
        String[] titles = new String[] {"DRM", "Live", "Offline", "Audio Only", "Multi Audio Track",
                "Captions", "Bitrate Selection"};
        ArrayList<CardData> results = new ArrayList<>();
        for (int index = 0; index < titles.length; index++) {
            results.add(new CardData(titles[index]));
        }
        return results;
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
    public interface OnSubMenuInteractionListener {
        void onSubMenuInteraction(int position);
    }
}
