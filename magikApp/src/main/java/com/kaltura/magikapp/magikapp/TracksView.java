package com.kaltura.magikapp.magikapp;

import android.content.Context;
import android.content.DialogInterface;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.kaltura.magikapp.R;
import com.kaltura.playkit.utils.Consts;

import java.util.List;

import static com.kaltura.playkit.utils.Consts.TRACK_TYPE_AUDIO;
import static com.kaltura.playkit.utils.Consts.TRACK_TYPE_TEXT;
import static com.kaltura.playkit.utils.Consts.TRACK_TYPE_VIDEO;

/**
 * Created by anton.afanasiev on 11/12/2016.
 */

public class TracksView implements View.OnClickListener{

    public static final int CLOSE_TRACKS_DIALOG = -1;

    private Context context;
    private ImageButton videoButton;
    private ImageButton audioButton;
    private ImageButton textButton;
    private ImageButton closeDialogButton;
    private ConstraintLayout constraintLayout;
    private TracksController.OnTracksDialogEventListener onTracksDialogEventListener;

    public TracksView(Context context, ConstraintLayout constraintLayout) {
        this.context = context;
        this.constraintLayout = constraintLayout;
        initialize();
        toggleDialogVisibility(false);
    }


    private void initialize() {
        videoButton = (ImageButton) constraintLayout.findViewById(R.id.video_track_select_btn);
        audioButton = (ImageButton) constraintLayout.findViewById(R.id.audio_track_select_btn);
        textButton = (ImageButton) constraintLayout.findViewById(R.id.text_track_select_btn);
        closeDialogButton = (ImageButton) constraintLayout.findViewById(R.id.close_btn);
        ImageView background = (ImageView) constraintLayout.findViewById(R.id.track_selection_background);

        videoButton.setOnClickListener(this);
        audioButton.setOnClickListener(this);
        textButton.setOnClickListener(this);
        closeDialogButton.setOnClickListener(this);
        background.setLayoutParams(new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT));
    }

    public void setOnTracksDialogEventListener(TracksController.OnTracksDialogEventListener onTracksDialogEventListener) {
        this.onTracksDialogEventListener = onTracksDialogEventListener;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.video_track_select_btn:
                onTracksDialogEventListener.showTracksSelectionDialog(TRACK_TYPE_VIDEO);
                break;
            case R.id.audio_track_select_btn:
                onTracksDialogEventListener.showTracksSelectionDialog(TRACK_TYPE_AUDIO);
                break;
            case R.id.text_track_select_btn:
                onTracksDialogEventListener.showTracksSelectionDialog(TRACK_TYPE_TEXT);
                break;
            case R.id.close_btn:
                onTracksDialogEventListener.showTracksSelectionDialog(CLOSE_TRACKS_DIALOG);
                break;
        }
    }


    public void showTracksSelectionDialog(int trackType, final List<TrackItem> trackItems, int lastTrackSelection) {
        RecyclerView recyclerView = buildTracksSelectionView();
        final TracksAdapter adapter = new TracksAdapter(trackItems, lastTrackSelection);
        recyclerView.setAdapter(adapter);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle(getDialogTitle(trackType));
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                closeDialogButton.performClick();
                onTracksDialogEventListener.onTrackSelected(adapter.getTrackItemId(), adapter.getLastTrackSelection());
            }
        });
        builder.setView(recyclerView);

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private int getDialogTitle(int trackType) {

        switch (trackType){
            case Consts.TRACK_TYPE_VIDEO:
                return R.string.video_track_selection_dialog_title;
            case Consts.TRACK_TYPE_AUDIO:
                return R.string.audio_track_selection_dialog_title;
            case Consts.TRACK_TYPE_TEXT:
                return R.string.text_track_selection_dialog_title;
            default:
                return R.string.empty_string;
        }
    }

    private RecyclerView buildTracksSelectionView() {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        RecyclerView recyclerView = (RecyclerView) inflater.inflate(R.layout.tracks_selection_recycle_view, null);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(context);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);

        return recyclerView;
    }

    public void toggleDialogVisibility(boolean doShow){
        if(doShow){
            constraintLayout.setVisibility(View.VISIBLE);
        }else{
            constraintLayout.setVisibility(View.INVISIBLE);
        }
    }

    public void setSelectionButtonEnabled(int trackTypeVideo, boolean shouldEnable) {
        ImageButton imageButton;
        float alpha = 1f;
        if(!shouldEnable) {
            alpha = 0.5f;
        }

        switch (trackTypeVideo){
            case TRACK_TYPE_VIDEO:
                imageButton = videoButton;
                break;
            case TRACK_TYPE_AUDIO:
                imageButton = audioButton;
                break;
            case TRACK_TYPE_TEXT:
                imageButton = textButton;
                break;
            default:
                return;
        }

        imageButton.setEnabled(shouldEnable);
        imageButton.setAlpha(alpha);
    }
}
