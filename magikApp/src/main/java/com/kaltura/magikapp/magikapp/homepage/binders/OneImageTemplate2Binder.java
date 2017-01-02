package com.kaltura.magikapp.magikapp.homepage.binders;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.kaltura.magikapp.R;

/**
 * Created by vladir on 02/01/2017.
 */

public class OneImageTemplate2Binder extends DataBinder<OneImageTemplate2Binder.ViewHolder> {

    private String mUrl;

    public OneImageTemplate2Binder(Context context) {
        super(context);
    }

    @Override
    public OneImageTemplate2Binder.ViewHolder newViewHolder(ViewGroup parent) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.oneimage_template2_layout, parent, false);
//        int height = (int) (parent.getMeasuredHeight() / 1.5);
//        v.setMinimumHeight(height);
        return new OneImageTemplate2Binder.ViewHolder(v);
    }

    @Override
    public void bindViewHolder(OneImageTemplate2Binder.ViewHolder holder, int position) {
        Glide.with(mContext).load(R.drawable.parties).centerCrop().crossFade().into(holder.mImageView);

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

        public ViewHolder(View itemView) {
            super(itemView);
            mImageView = (ImageView)itemView.findViewById(R.id.oneimage_image);
        }
    }
}
