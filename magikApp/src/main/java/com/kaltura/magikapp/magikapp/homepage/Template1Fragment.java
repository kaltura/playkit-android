package com.kaltura.magikapp.magikapp.homepage;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kaltura.magikapp.R;
import com.kaltura.magikapp.magikapp.asset_page.AssetInfo;
import com.kaltura.magikapp.magikapp.asset_page.AssetPageFragment;
import com.kaltura.magikapp.magikapp.core.FragmentAid;
import com.kaltura.magikapp.magikapp.homepage.binders.DataBinder;
import com.kaltura.magikapp.magikapp.homepage.binders.ExtendedItemGridAdapter;
import com.kaltura.magikapp.magikapp.homepage.binders.FourImageDataBinder;
import com.kaltura.magikapp.magikapp.homepage.binders.SimpleGridAdapterTemplate1;
import com.kaltura.magikapp.magikapp.homepage.binders.OneImageDataBinder;
import com.kaltura.magikapp.magikapp.homepage.recycler.RowSpaceItemDecoration;
import com.kaltura.magikapp.magikapp.homepage.recycler.Template1RecyclerAdapter;
import com.kaltura.magikapp.magikapp.toolbar.ToolbarMediator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vladir on 01/01/2017.
 */

public class Template1Fragment extends Fragment {

    private RecyclerView mRecyclerView;
    private Context mContext;
    private View mContainer;
    private ViewPager mViewPager;
    protected FragmentAid mFragmentAid;

    private String[] mViewPagerUrls = {"http://cdn.pinchofyum.com/wp-content/uploads/Simple-Mushroom-Penne-eaten-with-fork-600x900.jpg",
            "http://cdn.pinchofyum.com/wp-content/uploads/Dynamite-Plant-Power-Sushi-Bowl-2-2-600x900.jpg",
               "http://cdn.pinchofyum.com/wp-content/uploads/Sweet-Potato-Noodle-Salad-1-6-600x900.jpg" };


    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
        mFragmentAid = (FragmentAid) context;
        mContext = context;
    }

    public static Fragment newInstance() {
        return new Template1Fragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mContainer = inflater.inflate(R.layout.content_scrolling_template1, container, false);
        return mContainer;

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mFragmentAid.setToolbarTitle("");
        mFragmentAid.changeToolbarLayoutColor(false);
        mFragmentAid.setToolbarHomeButton(ToolbarMediator.BUTTON_MENU);

        mRecyclerView = (RecyclerView) mContainer.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));

        OneImageDataBinder oneImageBinder = new OneImageDataBinder(mContext);
        oneImageBinder.setData(mViewPagerUrls[0], "Dynamite plant test", "April 11, 2016 test");
        List<DataBinder> binders = new ArrayList<>();
        binders.add(oneImageBinder);

        int[] drawableRes = {R.drawable.pasta, R.drawable.bliss, R.drawable.crock, R.drawable.korean};
        SimpleGridAdapterTemplate1 gridAdapter = new SimpleGridAdapterTemplate1(mContext, drawableRes);
        gridAdapter.setOnClickListener(mOnItemClicked);
        FourImageDataBinder fourImageDataBinder = new FourImageDataBinder(mContext, gridAdapter);
        fourImageDataBinder.setTitles("make this for", " DINNER");
        binders.add(fourImageDataBinder);

        int[] drawableRes2 = {R.drawable.pullaprt, R.drawable.squash, R.drawable.bruschetta, R.drawable.mediterranean};
        ExtendedItemGridAdapter extendedItemGridAdapter = new ExtendedItemGridAdapter(mContext, drawableRes2);
        extendedItemGridAdapter.setOnClickListener(mOnItemClicked);
        FourImageDataBinder fourImageDataBinder2 = new FourImageDataBinder(mContext, extendedItemGridAdapter);
        fourImageDataBinder.showBackground(false);
        fourImageDataBinder2.setTitles("the latest", " AND GREATEST");
        binders.add(fourImageDataBinder2);

        Template1RecyclerAdapter template1RecyclerAdapter = new Template1RecyclerAdapter(mContext, binders);
        mRecyclerView.setAdapter(template1RecyclerAdapter);
        mRecyclerView.addItemDecoration(new RowSpaceItemDecoration(30));

    }

    private Template1RecyclerAdapter.ItemClick mOnItemClicked = new Template1RecyclerAdapter.ItemClick() {
        @Override
        public void onClick(AssetInfo asset) {
            getFragmentManager().beginTransaction().replace(R.id.activity_scrolling_content, AssetPageFragment.newInstance()).addToBackStack("item").commit();
        }
    };

}














