package com.kaltura.playkitdemo;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.kaltura.playkitdemo.jsonConverters.ConverterSubMenu;

import java.util.ArrayList;


public class SubMenuFragment extends AbsMenuFragment {


    private static final String ROOT_MENU_POSITION = "ROOT_MENU_POSITION";

    private ArrayList<ConverterSubMenu> mConverterSubMenuList;
    private OnSubMenuInteractionListener mListener;
    private int mRootMenuPosition;



    public SubMenuFragment() {
        // Required empty public constructor
    }


    public static SubMenuFragment newInstance(int rootMenuPosition, ArrayList<ConverterSubMenu> converterSubMenuList) {

        SubMenuFragment fragment = new SubMenuFragment();

        Bundle args = new Bundle();
        args.putParcelableArrayList(MainActivity.CONVERTER_SUB_MENU_LIST, converterSubMenuList);
        args.putInt(ROOT_MENU_POSITION, rootMenuPosition);
        fragment.setArguments(args);

        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Bundle arguments = getArguments();

        if (arguments != null) {
            mConverterSubMenuList = arguments.getParcelableArrayList(MainActivity.CONVERTER_SUB_MENU_LIST);
            mRootMenuPosition = arguments.getInt(ROOT_MENU_POSITION);
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
            public void onItemClick(int subMenuPosition, View v) {
                mListener.onSubMenuInteraction(mRootMenuPosition, subMenuPosition);
            }
        }, false);
    }




    @Override
    protected ArrayList<String> getDataSet() {

        ArrayList<String> subMenuTitles = new ArrayList<>();

        for (ConverterSubMenu subMenu : mConverterSubMenuList) {
            subMenuTitles.add(subMenu.getSubMenuTitle());
        }

        return subMenuTitles;
    }


    @Override
    protected int getDivider() {
        return R.drawable.divider_small;
    }


    public interface OnSubMenuInteractionListener {
        void onSubMenuInteraction(int rootMenuPosition, int subMenuPosition);
    }
}
