package com.kaltura.magikapp.magikapp;

import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import com.kaltura.magikapp.R;

import java.util.List;

/**
 * Created by anton.afanasiev on 07/12/2016.
 */
public class TracksAdapter extends RecyclerView.Adapter<TracksAdapter.ViewHolder> {

    private List<TrackItem> trackItems;
    private String selectedItemUniqueId;
    private int lastTrackSelection;

    public TracksAdapter(List<TrackItem> trackItems, int lastTrackSelection) {
        this.trackItems = trackItems;
        this.lastTrackSelection = lastTrackSelection;
        selectedItemUniqueId = trackItems.get(lastTrackSelection).getUniqueId();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ConstraintLayout layout = (ConstraintLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.track_selection_row_item, parent, false);

        ViewHolder viewHolder = new ViewHolder(layout);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        holder.radioButton.setChecked(position == lastTrackSelection);
        holder.textView.setText(trackItems.get(position).getTrackDescription());
    }

    @Override
    public int getItemCount() {
        return trackItems.size();
    }

    public String getTrackItemId() {
        return selectedItemUniqueId;
    }

    public int getLastTrackSelection() {
        return lastTrackSelection;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private TextView textView;
        private RadioButton radioButton;

        public ViewHolder(ConstraintLayout layout) {
            super(layout);
            textView = (TextView) layout.findViewById(R.id.tvTrackDescription);
            radioButton = (RadioButton) layout.findViewById(R.id.rbTrackItem);

            radioButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    lastTrackSelection = getAdapterPosition();
                    selectedItemUniqueId = trackItems.get(lastTrackSelection).getUniqueId();
                    notifyItemRangeChanged(0, trackItems.size());
                }
            });
        }
    }
}



