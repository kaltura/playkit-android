package com.kaltura.magikapp.magikapp.homepage;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.kaltura.magikapp.R;
import com.kaltura.magikapp.magikapp.homepage.binders.DataBinder;
import com.kaltura.magikapp.magikapp.homepage.binders.OneImageDataBinder;
import com.kaltura.magikapp.magikapp.homepage.recycler.Template1RecyclerAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by vladir on 01/01/2017.
 */

public class Template1Fragment extends Fragment {

    private RecyclerView mRecyclerView;
    private FragmentActivityMediator mActivity;
    private Toolbar mToolbar;
    private Context mContext;
    private View mContainer;
    private ViewPager mViewPager;
    private String[] mViewPagerUrls = {"http://cdn.pinchofyum.com/wp-content/uploads/Simple-Mushroom-Penne-eaten-with-fork-600x900.jpg",
            "http://cdn.pinchofyum.com/wp-content/uploads/Dynamite-Plant-Power-Sushi-Bowl-2-2-600x900.jpg",
               "http://cdn.pinchofyum.com/wp-content/uploads/Sweet-Potato-Noodle-Salad-1-6-600x900.jpg" };


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (FragmentActivityMediator) context;
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
        mContainer = inflater.inflate(R.layout.activity_scrolling_template1, container, false);
        return mContainer;

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mToolbar = (Toolbar) mContainer.findViewById(R.id.toolbar);
        mActivity.setToolbar(mToolbar);

        mRecyclerView = (RecyclerView) mContainer.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));

        OneImageDataBinder oneImageBinder = new OneImageDataBinder(mContext);
        oneImageBinder.setData(mViewPagerUrls[0], "Dynamite plant test", "April 11, 2016 test");
        List<DataBinder> binders = new ArrayList<>();
        binders.add(oneImageBinder);

        mRecyclerView.setAdapter(new Template1RecyclerAdapter(mContext, binders));

//        mViewPager = (ViewPager) mContainer.findViewById(R.id.header_view_pager);
//        ToolbarHeaderImageAdapter headerPagerAdapter = new ToolbarHeaderImageAdapter(mContext, mViewPagerUrls);
//        mViewPager.setAdapter(headerPagerAdapter);

    }



    private class ToolbarHeaderImageAdapter extends PagerAdapter {

        private Context mContext;
        private LayoutInflater mInflater;
        private List<String> mUrls;


        public ToolbarHeaderImageAdapter(Context context, String[] urls) {
            this.mContext = context;
            mInflater = LayoutInflater.from(context);
            mUrls = new ArrayList<>(Arrays.asList(urls));
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = mInflater.inflate(R.layout.template1_view_pager_item, container);
            ImageView imageView = (ImageView) view.findViewById(R.id.view_pager_image);

            Glide.with(mContext).load(mUrls.get(position)).into(imageView);

            return container;
        }

        @Override
        public int getCount() {
            return mUrls.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return false;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "vladi";
        }
    }

}














