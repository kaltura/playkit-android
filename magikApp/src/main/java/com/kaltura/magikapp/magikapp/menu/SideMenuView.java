package com.kaltura.magikapp.magikapp.menu;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.kaltura.magikapp.R;


/**
 * Created by zivilan on 01/01/2017.
 */

public class SideMenuView extends LinearLayout implements View.OnClickListener, MenuAdapter.MenuItemClickListener {
    private RecyclerView mMenu;
    private SideMenuListener mListener;
    private MenuAdapter mMenuAdapter;
    public static final int SELECTION_NONE = -1;
    protected int mLastSelectedPosition = SELECTION_NONE;
    protected int mCurrentSelection = SELECTION_NONE;

    public SideMenuView(Context context) {
        super(context);
        init();
    }

    public SideMenuView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SideMenuView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setMenuListener(SideMenuListener listener) {
        mListener = listener;
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_side_menu, this);
        mMenu = (RecyclerView) findViewById(R.id.menu);
        mMenu.setLayoutManager(new LinearLayoutManager(getContext()));
        mMenuAdapter = new MenuAdapter(MenuHelper.getInstance().getMenuItems(), this);
        mMenu.setAdapter(mMenuAdapter);

    }

    @Override
    public boolean onItemClick(MenuItem menuItem, int position, boolean doNothing) {
        if (!doNothing && mListener != null) {
            return mListener.onMenuClicked(menuItem);
        }
        return false;
    }

    public MenuAdapter getMenuAdapter() {
        return mMenuAdapter;
    }

    public void setMenuSelectionById(String menuId) {
        int position = MenuHelper.getInstance().getMenuItemPosition(menuId);

        if (position > -1) {
            setMenuSelection(position);
        }
    }

    public void setMenuSelection(int selection) {
        if (selection >= SELECTION_NONE && mMenuAdapter != null && selection < mMenuAdapter.getItemCount()) {
            mMenuAdapter.setSelectedItem(selection);
            if (selection > SELECTION_NONE)
                mLastSelectedPosition = selection;
        }
    }

    @Override
    public void onClick(View view) {
    }
}
