package com.kaltura.magikapp.magikapp.homepage.binders;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by vladir on 01/01/2017.
 */

public class FourImageDataBinder extends DataBinder<FourImageDataBinder.ViewHolder> {


    public FourImageDataBinder(Context context) {
        super(context);
    }

    @Override
    public ViewHolder newViewHolder(ViewGroup parent) {
        return null;
    }

    @Override
    public void bindViewHolder(ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 1;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(View itemView) {
            super(itemView);
        }
    }
}
