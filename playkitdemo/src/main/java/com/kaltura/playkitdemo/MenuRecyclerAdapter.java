package com.kaltura.playkitdemo;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by itanbarpeled on 12/11/2016.
 */


 class MenuRecyclerAdapter extends RecyclerView.Adapter<MenuRecyclerAdapter.DataObjectHolder> {

    private ArrayList<CardData> mDataSet;
    private MenuClickListener mClickListener;
    private int[] drawableArray;
    private boolean mShowBullet;



    MenuRecyclerAdapter(ArrayList<CardData> myDataSet, MenuClickListener clickListener, boolean showBullet) {
        mDataSet = myDataSet;
        drawableArray = new int[] {R.drawable.bullet_orange, R.drawable.bullet_yellow,
                R.drawable.bullet_green, R.drawable.bullet_red};
        mClickListener = clickListener;
        mShowBullet = showBullet;
    }

    @Override
    public DataObjectHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view_row, parent, false);

        return new DataObjectHolder(view);
    }


    @Override
    public void onBindViewHolder(DataObjectHolder holder, int position) {
        holder.titleTextView.setText(mDataSet.get(position).getTitle());
        setBulletDrawable(holder, position);
    }


    private void setBulletDrawable(DataObjectHolder holder, int position) {
        if (mShowBullet) {
            holder.titleTextView.setCompoundDrawablesWithIntrinsicBounds(drawableArray[position % 4], 0, 0, 0);
            holder.titleTextView.setCompoundDrawablePadding(convertToPX(holder.context, 24));
        }
    }


    /**
     * Some methods expect to receive values in pixels unit of measure.
     * This method converts dp to px.
     */
    private int convertToPX(Context context, int dp) {
        Resources resources = context.getResources();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.getDisplayMetrics());
    }


    /*
    public void addItem(DataObject dataObj, int index) {
        mDataSet.add(index, dataObj);
        notifyItemInserted(index);
    }

    public void deleteItem(int index) {
        mDataSet.remove(index);
        notifyItemRemoved(index);
    }
    */

    @Override
    public int getItemCount() {
        return mDataSet.size();
    }


    // TODO does this inner class should be static
    class DataObjectHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        Context context;
        TextView titleTextView;


        DataObjectHolder(View itemView) {

            super(itemView);

            titleTextView = (TextView) itemView.findViewById(R.id.card_title);
            context = itemView.getContext();
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            mClickListener.onItemClick(getAdapterPosition(), v);
        }
    }


    interface MenuClickListener {
        void onItemClick(int position, View v);
    }
}
