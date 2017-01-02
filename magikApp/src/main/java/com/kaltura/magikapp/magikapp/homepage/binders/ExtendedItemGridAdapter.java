package com.kaltura.magikapp.magikapp.homepage.binders;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.kaltura.magikapp.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by vladir on 01/01/2017.
 */

public class ExtendedItemGridAdapter extends RecyclerView.Adapter<ExtendedItemGridAdapter.ViewHolder> {

    private Context mContext;
    private List<String> mUrls;

    public ExtendedItemGridAdapter(Context context, String[] urls) {
        mContext = context;
        mUrls = new ArrayList(Arrays.asList(urls));
    }

    @Override
    public ExtendedItemGridAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ExtendedItemGridAdapter.ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.fourimage_item_extended_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(ExtendedItemGridAdapter.ViewHolder holder, int position) {
        Glide.with(mContext).load(mUrls.get(0)).centerCrop().crossFade().into(holder.mImageView);
//        holder.mTitleTextView.setText();
//        holder.mSubTitleTextView.setText();
    }

    @Override
    public int getItemCount() {
        return 4;
    }

    class ViewHolder extends GridAdapter.ViewHolder{

        TextView mTitleTextView;
        TextView mSubTitleTextView;

        public ViewHolder(View view) {
            super(view);
            mTitleTextView = (TextView) view.findViewById(R.id.oneimage_image_title);
            mSubTitleTextView = (TextView) view.findViewById(R.id.oneimage_image_sub_title);
        }
    }

}

