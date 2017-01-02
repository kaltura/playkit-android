package com.kaltura.magikapp.magikapp.homepage.binders;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.kaltura.magikapp.R;
import com.kaltura.magikapp.magikapp.homepage.recycler.SpacesItemDecoration;

/**
 * Created by vladir on 01/01/2017.
 */

public class FourImageDataBinder extends DataBinder<FourImageDataBinder.ViewHolder> {

    private RecyclerView.Adapter mAdapter;
    private boolean mIsShowBackground = true;
    private String mTitle1;
    private String mTitle2;


    public static String[] URLS = {"http://cdn.pinchofyum.com/wp-content/uploads/Simple-Mushroom-Penne-eaten-with-fork-600x900.jpg",
            "http://cdn.pinchofyum.com/wp-content/uploads/Dynamite-Plant-Power-Sushi-Bowl-2-2-600x900.jpg",
            "http://cdn.pinchofyum.com/wp-content/uploads/Sweet-Potato-Noodle-Salad-1-6-600x900.jpg",
            "http://www.foodinsight.org/sites/default/files/styles/main_image_for_details/public/colorful%20foods_0.png?itok=yTTDVxZF"};


    public FourImageDataBinder(Context context, RecyclerView.Adapter adapter) {
        super(context);
        mAdapter = adapter;
    }

    @Override
    public void setTitles(String title1, String title2) {
        mTitle1 = title1;
        mTitle2 = title2;
    }

    @Override
    public ViewHolder newViewHolder(ViewGroup parent) {
        View v = LayoutInflater.from(mContext).inflate(R.layout.fourimage_layout, parent, false);

        int height = parent.getMeasuredHeight() / 3;
        v.setMinimumHeight(height);
        return new ViewHolder(v);
    }

    @Override
    public void bindViewHolder(ViewHolder holder, int position) {
        holder.mGrid.setLayoutManager(new GridLayoutManager(mContext, 2));
        holder.mGrid.setLayoutFrozen(true);
        holder.mGrid.setAdapter(mAdapter);
        holder.mGrid.addItemDecoration(new SpacesItemDecoration(30));

        if (!mTitle1.isEmpty()) {
            holder.mTitle1.setText(mTitle1);
        }

        if (!mTitle2.isEmpty()) {
            holder.mTitle2.setText(mTitle2);
        }

        if (!mIsShowBackground) {
            holder.mContainer.setBackgroundResource(0);
        }


    }

    @Override
    public void showBackground(boolean isShow) {
        mIsShowBackground = isShow;
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private RecyclerView mGrid;
        private View mContainer;
        private TextView mTitle1;
        private TextView mTitle2;

        public ViewHolder(View view) {
            super(view);
            mGrid = (RecyclerView) view.findViewById(R.id.fourimage_grid);
            mContainer = view.findViewById(R.id.container);
            mTitle1 = (TextView) view.findViewById(R.id.four_image_title1);
            mTitle2 = (TextView) view.findViewById(R.id.four_image_title2);
        }
    }


}
