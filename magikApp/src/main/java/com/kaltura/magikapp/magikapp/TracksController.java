package com.kaltura.magikapp.magikapp;

import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.view.ViewStub;
import android.widget.ImageView;

import com.kaltura.magikapp.PlayerControlsView;
import com.kaltura.magikapp.R;
import com.kaltura.playkit.AudioTrack;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKTracks;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.TextTrack;
import com.kaltura.playkit.VideoTrack;
import com.kaltura.playkit.utils.Consts;

import java.util.ArrayList;
import java.util.List;

import static com.kaltura.playkit.utils.Consts.TRACK_TYPE_AUDIO;
import static com.kaltura.playkit.utils.Consts.TRACK_TYPE_TEXT;
import static com.kaltura.playkit.utils.Consts.TRACK_TYPE_UNKNOWN;
import static com.kaltura.playkit.utils.Consts.TRACK_TYPE_VIDEO;


/**
 * Created by anton.afanasiev on 06/12/2016.
 */

 class TracksController {

    private static final String AUTO_TRACK_DESCRIPTION = "Auto";
    private Player player;
    private TracksView tracksView;
    private PlayerControlsView controlsView;
    private PKTracks tracks;

    private int currentTrackType = -1;
    private int lastVideoTrackSelection = 0;
    private int lastAudioTrackSelection = 0;
    private int lastTextTrackSelection = 0;


    interface OnTracksDialogEventListener {

        void showTracksSelectionDialog(int trackType);

        void onTrackSelected(String uniqueId, int lastTrackSelected);
    }

    private OnTracksDialogEventListener onTracksDialogEventListener = getOnTracksDialogEventListener();


     TracksController(PlayerControlsView controlsView) {
        this.controlsView = controlsView;
        inflateTracksDialog();
    }


    public void setPlayer(Player player) {
        this.player = player;
        subscribeToTracksAvailableEvent();
    }

     void destroyController() {
        tracks = null;
        toggleTrackSelectionDialogVisibility(false);
    }

    private void inflateTracksDialog() {
        ViewStub viewStub = (ViewStub) controlsView.findViewById(R.id.viewStub);
        viewStub.setLayoutResource(R.layout.tracks_selection_layout);
        tracksView = new TracksView(controlsView.getContext(), (ConstraintLayout) viewStub.inflate());
        tracksView.setOnTracksDialogEventListener(onTracksDialogEventListener);
    }

    private void subscribeToTracksAvailableEvent() {
        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                PlayerEvent.TracksAvailable tracksAvailable = (PlayerEvent.TracksAvailable) event;
                tracks = tracksAvailable.getPKTracks();
                toggleSelectionButtonState();
            }
        }, PlayerEvent.Type.TRACKS_AVAILABLE);
    }

    private List<TrackItem> createTrackItems(int eventType) {
        List<TrackItem> trackItems = new ArrayList<>();
        TrackItem trackItem;
        switch (eventType) {
            case TRACK_TYPE_VIDEO:
                List<VideoTrack> videoTracksInfo = tracks.getVideoTracks();
                for (int i = 0; i < videoTracksInfo.size(); i++) {
                    VideoTrack trackInfo = videoTracksInfo.get(i);
                    if (trackInfo.isAdaptive()) {
                        trackItem = new TrackItem(trackInfo.getUniqueId(), AUTO_TRACK_DESCRIPTION);
                    } else {
                        trackItem = new TrackItem(trackInfo.getUniqueId(), buildBitrateString(trackInfo.getBitrate()));
                    }

                    trackItems.add(trackItem);
                }
                break;
            case TRACK_TYPE_AUDIO:
                List<AudioTrack> audioTracksInfo = tracks.getAudioTracks();
                for (int i = 0; i < audioTracksInfo.size(); i++) {
                    AudioTrack trackInfo = audioTracksInfo.get(i);
                    if (trackInfo.isAdaptive()) {
                        trackItem = new TrackItem(trackInfo.getUniqueId(), buildLanguageString(trackInfo.getLanguage()) + " " + AUTO_TRACK_DESCRIPTION);
                    } else {
                        trackItem = new TrackItem(trackInfo.getUniqueId(), buildLanguageString(trackInfo.getLanguage()) + " " + buildBitrateString(trackInfo.getBitrate()));
                    }

                    trackItems.add(trackItem);
                }
                break;
            case TRACK_TYPE_TEXT:
                List<TextTrack> textTracksInfo = tracks.getTextTracks();
                for (int i = 0; i < textTracksInfo.size(); i++) {
                    TextTrack trackInfo = textTracksInfo.get(i);
                    if (trackInfo.isAdaptive()) {
                        trackItem = new TrackItem(trackInfo.getUniqueId(), AUTO_TRACK_DESCRIPTION);
                    } else {
                        trackItem = new TrackItem(trackInfo.getUniqueId(), buildLanguageString(trackInfo.getLanguage()));
                    }

                    trackItems.add(trackItem);
                }
                break;
        }

        return trackItems;
    }

    public void toggleTrackSelectionDialogVisibility(boolean doShow) {
        tracksView.toggleDialogVisibility(doShow);
    }

    private OnTracksDialogEventListener getOnTracksDialogEventListener() {
        OnTracksDialogEventListener onTracksDialogEventListener = new OnTracksDialogEventListener() {
            @Override
            public void showTracksSelectionDialog(int eventType) {
                int lastTrackSelection;
                List<TrackItem> trackItems;

                switch (eventType) {

                    case TRACK_TYPE_VIDEO:
                        currentTrackType = TRACK_TYPE_VIDEO;
                        lastTrackSelection = lastVideoTrackSelection;
                        trackItems = createTrackItems(eventType);
                        break;
                    case TRACK_TYPE_AUDIO:
                        currentTrackType = TRACK_TYPE_AUDIO;
                        lastTrackSelection = lastAudioTrackSelection;
                        trackItems = createTrackItems(eventType);
                        break;
                    case TRACK_TYPE_TEXT:
                        currentTrackType = TRACK_TYPE_TEXT;
                        lastTrackSelection = lastTextTrackSelection;
                        trackItems = createTrackItems(eventType);
                        break;
                    case TracksView.CLOSE_TRACKS_DIALOG:
                        tracksView.toggleDialogVisibility(false);
                        controlsView.toggleControlsVisibility(true);
                        return;
                    default:
                        return;
                }

                if (trackItems.size() > 1) {
                    tracksView.showTracksSelectionDialog(currentTrackType, trackItems, lastTrackSelection);
                } else {
                    showMessage(R.string.no_track_to_dispaly);
                }
            }

            @Override
            public void onTrackSelected(String uniqueId, int lastTrackSelected) {
                switch (currentTrackType) {
                    case TRACK_TYPE_VIDEO:
                        lastVideoTrackSelection = lastTrackSelected;
                        break;
                    case TRACK_TYPE_AUDIO:
                        lastAudioTrackSelection = lastTrackSelected;
                        break;
                    case TRACK_TYPE_TEXT:
                        lastTextTrackSelection = lastTrackSelected;
                        break;
                    case TRACK_TYPE_UNKNOWN:
                        //halt if the track type is unknown. Should never happen, but who knows...who knows...
                        return;
                }
                player.changeTrack(uniqueId);
            }
        };

        return onTracksDialogEventListener;
    }

    private void toggleSelectionButtonState() {

        tracksView.setSelectionButtonEnabled(TRACK_TYPE_VIDEO, tracks.getVideoTracks().size() > 1);

        tracksView.setSelectionButtonEnabled(TRACK_TYPE_AUDIO, tracks.getAudioTracks().size() > 1);

        tracksView.setSelectionButtonEnabled(TRACK_TYPE_TEXT, tracks.getTextTracks().size() > 1);

    }

    private void showMessage(int string) {

        if (controlsView != null) {
            ImageView itemView = (ImageView) controlsView.findViewById(R.id.icon_play_pause);
            Snackbar snackbar = Snackbar.make(itemView, string, Snackbar.LENGTH_LONG);
            snackbar.show();
        }
    }

    private static String buildBitrateString(long bitrate) {
        return bitrate == Consts.NO_VALUE ? ""
                : String.format("%.2fMbit", bitrate / 1000000f);
    }

    private static String buildLanguageString(String language) {
        return TextUtils.isEmpty(language) || "und".equals(language) ? ""
                : language;
    }


}
