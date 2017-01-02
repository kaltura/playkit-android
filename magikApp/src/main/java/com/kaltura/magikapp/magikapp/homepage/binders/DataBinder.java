package com.kaltura.magikapp.magikapp.homepage.binders;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

/**
 * Created by vladir on 01/01/2017.
 */

abstract public class DataBinder <T extends RecyclerView.ViewHolder>{

    protected Context mContext;

    public DataBinder(Context context) {
        mContext = context;
    }

    abstract public T newViewHolder(ViewGroup parent);

    abstract public void bindViewHolder(T holder, int position);

    abstract public void showBackground(boolean isShow);

    abstract public int getItemCount();

    public void setTitles(String title1, String title2){}

}
