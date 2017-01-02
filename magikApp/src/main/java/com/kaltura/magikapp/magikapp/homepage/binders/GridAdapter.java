package com.kaltura.magikapp.magikapp.homepage.binders;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.kaltura.magikapp.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by vladir on 01/01/2017.
 */

public class GridAdapter extends RecyclerView.Adapter<GridAdapter.ViewHolder> {

    private Context mContext;
    private List<String> mUrls;

    public GridAdapter(Context context, String[] urls) {
        mContext = context;
        mUrls = new ArrayList(Arrays.asList(urls));
    }

    @Override
    public GridAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new GridAdapter.ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.fourimage_item_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(GridAdapter.ViewHolder holder, int position) {
        Glide.with(mContext).load(mUrls.get(0)).centerCrop().crossFade().into(holder.mImageView);
    }

    @Override
    public int getItemCount() {
        return 4;
    }

    static class ViewHolder extends RecyclerView.ViewHolder{
        ImageView mImageView;

        public ViewHolder(View view) {
            super(view);
            mImageView = (ImageView) view.findViewById(R.id.four_image_item_image_view);
        }
    }

}
