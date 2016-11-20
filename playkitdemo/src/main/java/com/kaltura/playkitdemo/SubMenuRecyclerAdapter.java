package com.kaltura.playkitdemo;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.kaltura.playkitdemo.data.CardData;

import java.util.ArrayList;

/**
 * Created by itanbarpeled on 13/11/2016.
 */

class SubMenuRecyclerAdapter extends RecyclerView.Adapter<SubMenuRecyclerAdapter.DataObjectHolder> {

    private ArrayList<String> mDataSet;
    private MenuRecyclerAdapter.MenuClickListener mClickListener;



    SubMenuRecyclerAdapter(ArrayList<String> myDataSet, MenuRecyclerAdapter.MenuClickListener clickListener) {
        mDataSet = myDataSet;
        mClickListener = clickListener;
    }

    @Override
    public DataObjectHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sub_menu_row, parent, false);

        return new DataObjectHolder(view);
    }


    @Override
    public void onBindViewHolder(DataObjectHolder holder, int position) {
        holder.subMenuTitle.setText(mDataSet.get(position));
    }



    @Override
    public int getItemCount() {
        return mDataSet.size();
    }


    // TODO does this inner class should be static
    class DataObjectHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        Context context;
        TextView subMenuTitle;


        DataObjectHolder(View itemView) {

            super(itemView);

            subMenuTitle = (TextView) itemView.findViewById(R.id.sub_menu_title);
            context = itemView.getContext();
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            mClickListener.onItemClick(getAdapterPosition(), v);
        }
    }

}