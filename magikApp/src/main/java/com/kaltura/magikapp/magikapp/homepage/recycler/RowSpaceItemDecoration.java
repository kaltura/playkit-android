package com.kaltura.magikapp.magikapp.homepage.recycler;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by vladir on 01/01/2017.
 */

public class RowSpaceItemDecoration extends RecyclerView.ItemDecoration {

    private int space;

    public RowSpaceItemDecoration(int space) {
        this.space = space;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.bottom = space;
        outRect.top = space;
    }
}
