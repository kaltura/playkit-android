package com.kaltura.magikapp.magikapp.menu;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.kaltura.magikapp.R;

import java.util.List;


/**
 * Created by zivilan on 01/01/2017.
 */

public class MenuAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    private List<MenuItem> mMenuItems;
    private static int mSelectedMenuItem;
    private MenuItemClickListener mListener;


    public MenuAdapter(List<MenuItem> mMenuItemList, MenuItemClickListener listener) {
        this.mMenuItems = mMenuItemList;
        this.mListener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        switch (viewType) {
            case MenuItem.TYPE_TITLE: {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_menu_title, viewGroup, false);
                MenuTitleViewHolder holder = new MenuTitleViewHolder(view);
                return holder;
            }
            case MenuItem.TYPE_MENU_ITEM: {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_menu_item, viewGroup, false);
                MenuItemViewHolder holder = new MenuItemViewHolder(view);
                return holder;
            }
        }
        return null;
    }

    public void clearSelection() {
        updateSelectedMenuItemState(false);
        mSelectedMenuItem = -1;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final MenuItem menuItem = mMenuItems.get(position);
        switch (menuItem.getMenuType()) {
            case MenuItem.TYPE_TITLE:
                initTitle((MenuTitleViewHolder) holder, menuItem, position);
                break;
            case MenuItem.TYPE_MENU_ITEM:
                initMenuItem((MenuItemViewHolder) holder, menuItem, position);
                break;
        }
    }

    private void initMenuItem(final MenuItemViewHolder holder, final MenuItem menuItem, final int position) {
        holder.itemName.setText(menuItem.getMenuName());
        holder.indicator.setVisibility(menuItem.isItemSelected() ? View.VISIBLE : View.INVISIBLE);
//        holder.indicator.setBackgroundColor(UnifiedConfigurationManager.getInstance().getCurrentBrandColor());
        holder.itemIcon.setImageResource(menuItem.getResIcon());
        holder.mainLayout.setBackgroundResource(menuItem.isItemSelected() ? R.color.selected_menu_item_bg : R.color.menuBackgroud);
        holder.mainLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onMenuItemClick(menuItem, position);
            }
        });
    }

    public void onMenuItemClick(final MenuItem menuItem, final int position) {
        boolean doNothing = mSelectedMenuItem > -1 && mMenuItems.get(mSelectedMenuItem).getId() == menuItem.getId();// in case user clicks the same menu item as the current one.

        if (!doNothing) {
            setSelectedItem(position);
        }

        mListener.onItemClick(menuItem, position, doNothing);
    }


    public void setSelectedItem(int position) {
        updateSelectedMenuItemState(false);
        mSelectedMenuItem = position;
        updateSelectedMenuItemState(true);
    }

    private void updateSelectedMenuItemState(boolean selected){
        if(mSelectedMenuItem > -1) {
            MenuItem menuItem = mMenuItems.get(mSelectedMenuItem);
            if (menuItem != null) {
                menuItem.setItemSelected(selected);
                notifyItemChanged(mSelectedMenuItem);
            }
        }
    }

    private void initTitle(MenuTitleViewHolder holder, MenuItem menuItem, int position) {
        holder.groupTitle.setText(menuItem.getMenuName());
    }

    @Override
    public int getItemViewType(int position) {
        return mMenuItems.get(position).getMenuType();
    }

    @Override
    public int getItemCount() {
        if (mMenuItems != null) {
            return mMenuItems.size();
        }
        return 0;
    }


    public class MenuItemViewHolder extends RecyclerView.ViewHolder {
        protected View mainLayout;
        protected TextView itemName;
        protected View indicator;
        protected ImageView itemIcon;

        public MenuItemViewHolder(View view) {
            super(view);
            this.mainLayout = view;
            this.itemName = (TextView) view.findViewById(R.id.title);
            this.indicator = view.findViewById(R.id.indicator);
            this.itemIcon = (ImageView) view.findViewById(R.id.icon);
        }
    }

    public class MenuTitleViewHolder extends RecyclerView.ViewHolder {
        protected TextView groupTitle;

        public MenuTitleViewHolder(View view) {
            super(view);
            this.groupTitle = (TextView) view;
        }
    }

    public interface MenuItemClickListener {
        /**
         * Called when an item in the navigation menu is selected.
         *
         * @param itemId The selected itemId
         * @return true to display the item as the selected item
         */
        boolean onItemClick(MenuItem itemId, int position, boolean doNothing);
    }

}
