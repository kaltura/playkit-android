package com.kaltura.magikapp.magikapp.homepage.binders;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.kaltura.magikapp.R;

/**
 * Created by vladir on 01/01/2017.
 */

public class OneImageDataBinder extends DataBinder<OneImageDataBinder.ViewHolder> {

    private String mUrl;
    private String mTitleText;
    private String mSubTitleText;

    public OneImageDataBinder(Context context) {
        super(context);
    }

    public void setData(String url, String title, String subtitle){
        mUrl = url;
        mTitleText = title;
        mSubTitleText = subtitle;
    }

    @Override
    public ViewHolder newViewHolder(ViewGroup parent) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.oneimage_layout, parent, false);
        int height = parent.getMeasuredHeight() / 2;
        v.setMinimumHeight(height);
        return new ViewHolder(v);
    }

    @Override
    public void bindViewHolder(ViewHolder holder, int position) {
        Glide.with(mContext).load(R.drawable.dynamite).centerCrop().crossFade().into(holder.mImageView);

        holder.mTitleTextView.setText(mTitleText);
        holder.mSubTitleTextView.setText(mSubTitleText);
    }

    @Override
    public void showBackground(boolean isShow) {

    }

    @Override
    public int getItemCount() {
        return 1;
    }

    class ViewHolder extends RecyclerView.ViewHolder{

        ImageView mImageView;
        TextView mTitleTextView;
        TextView mSubTitleTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            mImageView = (ImageView)itemView.findViewById(R.id.oneimage_image);
            mTitleTextView = (TextView)itemView.findViewById(R.id.oneimage_image_title);
            mSubTitleTextView = (TextView)itemView.findViewById(R.id.oneimage_image_sub_title);
        }
    }
}
