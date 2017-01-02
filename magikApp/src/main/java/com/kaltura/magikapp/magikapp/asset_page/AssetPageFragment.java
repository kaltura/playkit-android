package com.kaltura.magikapp.magikapp.asset_page;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.kaltura.magikapp.PlaybackControlsView;
import com.kaltura.magikapp.R;
import com.kaltura.magikapp.magikapp.core.FragmentAid;
import com.kaltura.magikapp.magikapp.toolbar.ToolbarMediator;
import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.Player;

/**
 * Created by zivilan on 01/01/2017.
 */

public class AssetPageFragment extends Fragment {
    private Context mContext;
    private View mContainer;
    private ViewPager mViewPager;
    protected FragmentAid mFragmentAid;
    private int assetId;
    private TextView mTitle;
    private TextView mSubTitle;
    private TextView mDescription;

    private Player player;
    private MediaEntryProvider mediaProvider;
    private PlaybackControlsView controlsView;
    private boolean nowPlaying;
    ProgressBar progressBar;

    private Spinner videoSpinner, audioSpinner, textSpinner;




    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
        mFragmentAid = (FragmentAid) context;
        mContext = context;
    }

    public Fragment newInstance(int assetId) {
        this.assetId = assetId;
        return new AssetPageFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mContainer = inflater.inflate(R.layout.activity_scrolling, container, false);

        return mContainer;

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mFragmentAid.setToolbarTitle("");
        mFragmentAid.changeToolbarLayoutColor(false);
        mFragmentAid.setToolbarHomeButton(ToolbarMediator.BUTTON_BACK);

        mTitle = (TextView) mContainer.findViewById(R.id.asset_title);
        mSubTitle = (TextView) mContainer.findViewById(R.id.asset_sub_title);
        mDescription = (TextView) mContainer.findViewById(R.id.asset_description);

        mTitle.setText(getString(R.string.asset_title_sample));
        mSubTitle.setText(getString(R.string.asset_sub_title_sample));
        mDescription.setText(getString(R.string.asset_description_sample));

    }





}
