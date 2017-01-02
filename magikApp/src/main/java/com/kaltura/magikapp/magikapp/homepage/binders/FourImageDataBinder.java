package com.kaltura.magikapp.magikapp.homepage.binders;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kaltura.magikapp.R;
import com.kaltura.magikapp.magikapp.homepage.recycler.SpacesItemDecoration;

/**
 * Created by vladir on 01/01/2017.
 */

public class FourImageDataBinder extends DataBinder<FourImageDataBinder.ViewHolder> {

    private RecyclerView.Adapter mAdapter;
    private Drawable mBackground = null;

    public static String[] URLS = {"http://cdn.pinchofyum.com/wp-content/uploads/Simple-Mushroom-Penne-eaten-with-fork-600x900.jpg",
            "http://cdn.pinchofyum.com/wp-content/uploads/Dynamite-Plant-Power-Sushi-Bowl-2-2-600x900.jpg",
            "http://cdn.pinchofyum.com/wp-content/uploads/Sweet-Potato-Noodle-Salad-1-6-600x900.jpg",
            "http://www.foodinsight.org/sites/default/files/styles/main_image_for_details/public/colorful%20foods_0.png?itok=yTTDVxZF"};


    public FourImageDataBinder(Context context, RecyclerView.Adapter adapter) {
        super(context);
        mAdapter = adapter;
    }

    public FourImageDataBinder(Context context, RecyclerView.Adapter adapter, int resBackground) {
        super(context);
        mAdapter = adapter;
        mBackground = ResourcesCompat.getDrawable(mContext.getResources(), resBackground, null);
    }

    @Override
    public ViewHolder newViewHolder(ViewGroup parent) {
        View v = LayoutInflater.from(mContext).inflate(R.layout.fourimage_layout, parent, false);

        if (mBackground != null){
            v.findViewById(R.id.container).setBackground(mBackground);
        }

        int height = parent.getMeasuredHeight() / 3;
        v.setMinimumHeight(height);
        return new ViewHolder(v);
    }

    @Override
    public void bindViewHolder(ViewHolder holder, int position) {
        holder.mGrid.setLayoutManager(new GridLayoutManager(mContext, 2));
        holder.mGrid.setLayoutFrozen(true);
        holder.mGrid.setAdapter(mAdapter);
        holder.mGrid.addItemDecoration(new SpacesItemDecoration(30));

    }

    @Override
    public int getItemCount() {
        return 1;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private RecyclerView mGrid;

        public ViewHolder(View view) {
            super(view);
            mGrid = (RecyclerView) view.findViewById(R.id.fourimage_grid);
        }
    }


}
