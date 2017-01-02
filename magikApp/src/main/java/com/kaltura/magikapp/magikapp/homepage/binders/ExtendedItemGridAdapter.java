package com.kaltura.magikapp.magikapp.homepage.binders;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.kaltura.magikapp.R;
import com.kaltura.magikapp.magikapp.asset_page.AssetInfo;
import com.kaltura.magikapp.magikapp.homepage.recycler.Template1RecyclerAdapter;

import java.util.List;

/**
 * Created by vladir on 01/01/2017.
 */

public class ExtendedItemGridAdapter extends RecyclerView.Adapter<ExtendedItemGridAdapter.ViewHolder> {

    private Context mContext;
    private List<String> mUrls;
    int[] mDrawableRes;
    private Template1RecyclerAdapter.ItemClick mOnItemClicked;

    public ExtendedItemGridAdapter(Context context, int[] drawableRes) {
        mContext = context;
        mDrawableRes = drawableRes;
    }


//    public ExtendedItemGridAdapter(Context context, String[] urls) {
//        mContext = context;
//        mUrls = new ArrayList(Arrays.asList(urls));
//    }

    @Override
    public ExtendedItemGridAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.fourimage_item_extended_layout, parent, false);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOnItemClicked.onClick(new AssetInfo());
            }
        });

        return new ExtendedItemGridAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ExtendedItemGridAdapter.ViewHolder holder, int position) {
        Glide.with(mContext).load(mDrawableRes[position]).centerCrop().crossFade().into(holder.mImageView);

        String title = null;
        String subTitle = null;
        switch (position){
            case 0:
                title = "Mushroom Gnoochi";
                subTitle = "These little snack-dessert winners are called No-Bake salted cups";
                break;
            case 1:
                title = "Chipotle sweet potato noodle salad";
                subTitle = "No-Bake salted cups with corn";
                break;
            case 2:
                title = "No-Bake salted cups with corn";
                subTitle = "Prepared medium well";
                break;
            case 3:
                title = "No-Bake salted caramel cups";
                subTitle = "vegetarian soup";
                break;
        }
        holder.mTitleTextView.setText(title);
        holder.mSubTitleTextView.setText(subTitle);
    }

    @Override
    public int getItemCount() {
        return 4;
    }

    public void setOnClickListener(Template1RecyclerAdapter.ItemClick onItemClicked) {
        mOnItemClicked = onItemClicked;
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

