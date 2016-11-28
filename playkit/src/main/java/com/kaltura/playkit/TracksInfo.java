package com.kaltura.playkit;

import java.util.List;
import com.kaltura.playkit.TrackSelectionHelper.OnTrackInfoChanged;

/**
 * Container for all available track info.
 * Created by anton.afanasiev on 17/11/2016.
 */

public class TracksInfo {

    private List<BaseTrackInfo> audioTrackInfo;
    private List<BaseTrackInfo> videoTrackInfo;
    private List<BaseTrackInfo> subtitleTrackInfo;
    private OnTrackInfoChanged onTrackInfoChanged;
    private int[] lastTrackSelections = new int[3];

    public TracksInfo(List<BaseTrackInfo> videoTrackInfo, List<BaseTrackInfo> audioTrackInfo, List<BaseTrackInfo> subtitleTrackInfo, OnTrackInfoChanged onTrackInfoChanged) {
        this.audioTrackInfo = audioTrackInfo;
        this.videoTrackInfo = videoTrackInfo;
        this.subtitleTrackInfo = subtitleTrackInfo;
        this.onTrackInfoChanged = onTrackInfoChanged;
    }

    public List<BaseTrackInfo> getAudioTrackInfo() {
        return audioTrackInfo;
    }

    public List<BaseTrackInfo> getVideoTrackInfo() {
        return videoTrackInfo;
    }

    public List<BaseTrackInfo> getSubtitleTrackInfo() {
        return subtitleTrackInfo;
    }

    public OnTrackInfoChanged getOnTrackInfoChanged() {
        return onTrackInfoChanged;
    }

    public void updateLastSelection(int[] lastSelections){
        this.lastTrackSelections = lastSelections;
    }

    public int getLastSelection(int trackType) {
        return lastTrackSelections[trackType];
    }
}

