package com.kaltura.playkitdemo;

import android.content.Context;
import android.content.res.Resources;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by itanbarpeled on 13/11/2016.
 */

public class ExpandableMenuRecyclerAdapter extends RecyclerView.Adapter<ExpandableMenuRecyclerAdapter.DataObjectHolder> {


    private ArrayList<CardData> mDataSet;
    private MenuRecyclerAdapter.MenuClickListener mClickListener;
    private int[] drawableArray;



    ExpandableMenuRecyclerAdapter(ArrayList<CardData> myDataSet, MenuRecyclerAdapter.MenuClickListener clickListener) {
        mDataSet = myDataSet;
        drawableArray = new int[] {R.drawable.bullet_orange, R.drawable.bullet_yellow,
                R.drawable.bullet_green, R.drawable.bullet_red};
        mClickListener = clickListener;
    }


    @Override
    public ExpandableMenuRecyclerAdapter.DataObjectHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.expandable_menu_row, parent, false);

        return new ExpandableMenuRecyclerAdapter.DataObjectHolder(view);
    }


    @Override
    public void onBindViewHolder(ExpandableMenuRecyclerAdapter.DataObjectHolder holder, int position) {
        holder.rootMenuTitle.setText(mDataSet.get(position).getTitle());
        setBulletDrawable(holder, position);
    }


    private void setBulletDrawable(ExpandableMenuRecyclerAdapter.DataObjectHolder holder, int position) {
        holder.rootMenuTitle.setCompoundDrawablesWithIntrinsicBounds(drawableArray[position % 4], 0, 0, 0);
        holder.rootMenuTitle.setCompoundDrawablePadding(convertToPX(holder.context, 24));
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
    class DataObjectHolder extends RecyclerView.ViewHolder {

        RecyclerView.Adapter adapter;
        LinearLayoutManager layoutManager;

        boolean toggleSubMenu;

        Context context;
        TextView rootMenuTitle;
        RelativeLayout rootMenuContainer;
        RecyclerView subMenuList;



        DataObjectHolder(View itemView) {

            super(itemView);

            context = itemView.getContext();
            rootMenuTitle = (TextView) itemView.findViewById(R.id.menu_root_title);
            rootMenuContainer = (RelativeLayout) itemView.findViewById(R.id.menu_root_container);
            subMenuList = (RecyclerView) itemView.findViewById(R.id.sub_menu_list);

            toggleSubMenu = true;

            rootMenuContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    subMenuList.setVisibility(toggleSubMenu ? View.VISIBLE : View.GONE);
                    toggleSubMenu = !toggleSubMenu;
                }
            });


            setRecyclerView(context);

        }


        private void setRecyclerView(Context context) {

            layoutManager = new LinearLayoutManager(context);
            adapter = new SubMenuRecyclerAdapter(getDataSet(), mClickListener);


            subMenuList.setHasFixedSize(true);
            subMenuList.setLayoutManager(layoutManager);
            subMenuList.setAdapter(adapter);

            // TODO - restore when back to 25.0.0
            DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(context, layoutManager.getOrientation());
            dividerItemDecoration.setDrawable(ContextCompat.getDrawable(context, R.drawable.divider_small));
            subMenuList.addItemDecoration(dividerItemDecoration);

        }


        private ArrayList<CardData> getDataSet() {
            String[] titles = new String[] {"DRM", "Live", "Offline", "Audio Only", "Multi Audio Track",
                    "Captions", "Bitrate Selection"};
            ArrayList<CardData> results = new ArrayList<>();
            for (int index = 0; index < titles.length; index++) {
                results.add(new CardData(titles[index]));
            }
            return results;
        }

    }




}
