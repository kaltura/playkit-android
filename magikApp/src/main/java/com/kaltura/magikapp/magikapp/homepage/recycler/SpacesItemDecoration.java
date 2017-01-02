package com.kaltura.magikapp.magikapp.homepage.recycler;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by vladir on 01/01/2017.
 */

public class SpacesItemDecoration extends RecyclerView.ItemDecoration {

    private int space;

    public SpacesItemDecoration(int space) {
        this.space = space;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {

        switch (parent.getChildLayoutPosition(view)){
            case 0:
//                outRect.right = space;
                outRect.bottom = space;
                break;
            case 1:
                outRect.left = space;
                outRect.bottom = space;
                break;
            case 2:
//                outRect.right = space;
                outRect.top = space;
                break;
            case 3:
                outRect.left = space;
                outRect.top = space;
                break;

        }

//        outRect.left = space;
//        outRect.right = space;
//        outRect.bottom = space;
//        outRect.top = space;


    }
}
