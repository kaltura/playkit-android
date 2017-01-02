package com.kaltura.magikapp.magikapp.homepage.recycler;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.kaltura.magikapp.magikapp.asset_page.AssetInfo;
import com.kaltura.magikapp.magikapp.homepage.binders.DataBinder;

import java.util.List;

/**
 * Created by vladir on 01/01/2017.
 */

public class Template1RecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    List<DataBinder> mBinders;
    private Context mContext;

    public interface ItemClick{
        void onClick(AssetInfo asset);
    }


    public Template1RecyclerAdapter(Context context, List<DataBinder> binders) {
        mContext = context;
        mBinders = binders;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return getDataBinder(viewType).newViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        int binderPosition = getBinderPosition(position);
        // note: for simplicity type is same as position
        getDataBinder(viewHolder.getItemViewType()).bindViewHolder(viewHolder, binderPosition);
    }

    public DataBinder getDataBinder(int viewType){
         return mBinders.get(viewType);
    }

    private int getBinderPosition(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return 3;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

}
