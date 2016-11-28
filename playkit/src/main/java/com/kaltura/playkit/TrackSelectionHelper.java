package com.kaltura.playkit;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.RendererCapabilities;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.FixedTrackSelection;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import com.kaltura.playkit.player.ExoPlayerWrapper;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.SelectionOverride;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * Responsible for generating/sorting/holding and changing track info.
 * Created by anton.afanasiev on 22/11/2016.
 */

public class TrackSelectionHelper {

    private static final PKLog log = PKLog.get("TrackSelectionHelper");

    private static final TrackSelection.Factory FIXED_FACTORY = new FixedTrackSelection.Factory();

    public static final int TRACK_VIDEO = 0;
    public static final int TRACK_AUDIO = 1;
    public static final int TRACK_SUBTITLE = 2;
    public static final int TRACK_TYPE_UNKNOWN = -1;

    private static final int TRACK_RENDERERS_AMOUNT = 3;

    private ExoPlayerWrapper.TrackInfoReadyListener trackReadyListener;

    private TrackGroupArray trackGroups;
    private final MappingTrackSelector selector;
    private MappingTrackSelector.MappedTrackInfo mappedTrackInfo;
    private final TrackSelection.Factory adaptiveTrackSelectionFactory;

    private Map<String, VideoTrackInfo> videoMap = new LinkedHashMap<>();
    private Map<String, List<AudioTrackInfo>> audioMap = new LinkedHashMap<>();
    private Map<String, SubtitleTrackInfo> subtitleMap = new LinkedHashMap<>();

    private boolean isDisabled;


    public interface OnTrackInfoChanged {
        void customizeTrackInfo(int trackType, String[] updatedInfo);
    }

    //When application want to display only part of available tracks, or change their order, it should notify us
    // with this interface and pass the tracks it is interested in.
    private OnTrackInfoChanged onTrackInfoChanged = initOnTackInfoChanged();

    /**
     * @param selector                           The track selector.
     * @param adaptiveTrackSelectionFactory A factory for adaptive video {@link TrackSelection}s,
     *                                           or null if the selection helper should not support adaptive video.
     */
    public TrackSelectionHelper(MappingTrackSelector selector,
                                TrackSelection.Factory adaptiveTrackSelectionFactory) {
        this.selector = selector;
        this.adaptiveTrackSelectionFactory = adaptiveTrackSelectionFactory;
    }

    /**
     * Change the currently playing track.
     * @param trackType
     * @param position
     * @param mappedTrackInfo
     */
    public void changeTrack(int trackType, int position, MappedTrackInfo mappedTrackInfo) {
        if(trackType == TRACK_TYPE_UNKNOWN){
            return;
        }

        isDisabled = selector.getRendererDisabled(trackType);
        trackGroups = mappedTrackInfo.getTrackGroups(trackType);

        //retrieve selection with which we should override the existing one.
        SelectionOverride override = retrieveOverrideSelection(trackType, position);
        //override the track
        overrideTrack(trackType, override);
    }

    private SelectionOverride retrieveOverrideSelection(int trackType, int position) {

        SelectionOverride override = null;

        switch (trackType) {
            case TRACK_VIDEO:
                // build override object with which we will actually change the current one.
                VideoTrackInfo videoTrackInfo = videoMap.get(videoMap.keySet().toArray()[position]);
                override = new MappingTrackSelector.SelectionOverride(FIXED_FACTORY, videoTrackInfo.getGroupIndex(), videoTrackInfo.getTrackIndex());
                break;
            case TRACK_AUDIO:
                //audio case is more complexed.
                List<AudioTrackInfo> audioTrackInfos = audioMap.get(audioMap.keySet().toArray()[position]);
                int[] audioTrackIndexes = new int[audioTrackInfos.size()];
                //get all the audio tracks from audioMap in the selection.
                for (int i = 0; i < audioTrackInfos.size(); i++) {
                    audioTrackIndexes[i] = audioTrackInfos.get(i).getTrackIndex();
                }
                //if current selection contains a couple of audio tracks the selection will be adaptive. otherwise it will be changed to the regular selection.
                TrackSelection.Factory factory = audioTrackIndexes.length > 1 ? adaptiveTrackSelectionFactory : FIXED_FACTORY;
                override = new MappingTrackSelector.SelectionOverride(factory, audioTrackInfos.get(0).getGroupIndex(), audioTrackIndexes);
                break;
            case TRACK_SUBTITLE:
                SubtitleTrackInfo subtitleTrackInfo = subtitleMap.get(subtitleMap.keySet().toArray()[position]);
                override = new MappingTrackSelector.SelectionOverride(FIXED_FACTORY, subtitleTrackInfo.getGroupIndex(), subtitleTrackInfo.getTrackIndex());
                break;
        }

        return override;
    }

    private void overrideTrack(int trackType, SelectionOverride override) {
        //if renderer is disabled we will hide it.
        selector.setRendererDisabled(trackType, isDisabled);
        if (override != null) {
            //actually change track.
            selector.setSelectionOverride(trackType, trackGroups, override);
        } else {
            //clear all the selections if the override is null.
            selector.clearSelectionOverrides(trackType);
        }
    }

    /**
     * Sort track info. We need it in order to have the correct representation of the tracks.
     * @param mappedTrackInfo
     */
    public void sortTrackInfo(MappedTrackInfo mappedTrackInfo) {

        this.mappedTrackInfo = mappedTrackInfo;
        mapTracksInfo();

        //build TrackInfo that user will receive based on sorted map.
        TracksInfo tracksInfo = buildInfoFromMap();
        //notify the ExoplayerWrapper that track info is ready.
        if (trackReadyListener != null) {
            trackReadyListener.onTrackInfoReady(tracksInfo);
        }
    }

    /**
     * Mapping the track info.
     */
    private void mapTracksInfo() {
        TrackGroupArray trackGroupArray;
        TrackGroup trackGroup;
        Format format;

        //audio can be represent as both adaptive and regular tracks. so we need to handle both use cases.
        List<AudioTrackInfo> audioTrackInfoList = new ArrayList<>();
        List<AudioTrackInfo> adaptiveAudioTrackInfoList = new ArrayList<>();

        //run through the all renders.
        for (int rendererIndex = 0; rendererIndex < TRACK_RENDERERS_AMOUNT; rendererIndex++) {

            //the trackGroupArray of the current renderer.
            trackGroupArray = mappedTrackInfo.getTrackGroups(rendererIndex);

            //run through the all track groups in current renderer.
            for (int groupIndex = 0; groupIndex < trackGroupArray.length; groupIndex++) {

                // the track group of the current trackGroupArray.
                trackGroup = trackGroupArray.get(groupIndex);

                //run through the all tracks in current trackGroup.
                for (int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {

                    // the format of the current trackGroup.
                    format = trackGroup.getFormat(trackIndex);

                    //filter all the unsupported and unknown formats.
                    if (isFormatSupported(rendererIndex, groupIndex, trackIndex) && format.id != null) {

                        switch (rendererIndex) {
                            case TRACK_VIDEO:
                                //put the videoTrackInfo in video map, with key as unique id of the format.
                                videoMap.put(format.id, new VideoTrackInfo(format.bitrate, format.width, format.height, format.id, groupIndex, trackIndex));
                                break;
                            case TRACK_AUDIO:
                                //with audio it is a little bit complex. There can be situation when we have groups that contain
                                // adaptive and non adaptive tracks at the same time.
                                // so we need to map them according to their adaptivnes.
                                AudioTrackInfo audioTrackInfo = new AudioTrackInfo(format.language, format.id, groupIndex, trackIndex);
                                boolean isAdaptive = isAdaptive(trackGroupArray, rendererIndex, groupIndex);
                                mapAudioTracks(audioTrackInfoList, adaptiveAudioTrackInfoList, audioTrackInfo, isAdaptive);
                                break;

                            case TRACK_SUBTITLE:
                                //put the subtitleTrackInfo in subtitle map, with key as unique id of the format.
                                subtitleMap.put(format.id, new SubtitleTrackInfo(format.language, format.id, groupIndex, trackIndex));
                                break;
                        }
                    }
                }
            }
        }
    }

    private TracksInfo buildInfoFromMap() {
        //iterate through all the audioLists in audio map, and obtain only the first element in each list.
        // we need only first item in order to represent the data to the application.
        //later when the user would like to change the track we will pull all the adaptive tracks.
        List<BaseTrackInfo> audioTrackInfos = new ArrayList();
        for (List<AudioTrackInfo> audioInfos : audioMap.values()) {
            audioTrackInfos.add(audioInfos.get(0));
        }

        return new TracksInfo(
                new ArrayList(videoMap.values()),
                audioTrackInfos,
                new ArrayList(subtitleMap.values()),
                onTrackInfoChanged);
    }


    private void mapAudioTracks(List<AudioTrackInfo> audioTrackInfoList, List<AudioTrackInfo> adaptiveAudioTrackInfoList, AudioTrackInfo audioTrackInfo, boolean isAdaptive) {
        // adaptive audio track
        if (isAdaptive) {
            //add item to adaptiveList.
            adaptiveAudioTrackInfoList.add(audioTrackInfo);
            String uniqueKey = adaptiveAudioTrackInfoList.get(0).getUniqueId();
            //if audioMap already contains list with the same unique key we should remove this list from map.
            if (audioMap.containsKey(uniqueKey)) {
                audioMap.remove(uniqueKey);
            }
            //and here we need to put it back, but with updated list.
            audioMap.put(uniqueKey, adaptiveAudioTrackInfoList);

        } else {
            // not adaptive audio track.

            //clear the adaptiveAudioList.
            adaptiveAudioTrackInfoList.clear();
            audioTrackInfoList.add(audioTrackInfo);
            String uniqueKey = audioTrackInfoList.get(0).getUniqueId();
            //put the copy regularAudioList in audioMap.
            audioMap.put(uniqueKey, new ArrayList<>(audioTrackInfoList));
            //clear the regular audio list.
            audioTrackInfoList.clear();
        }
    }

    private boolean isFormatSupported(int rendererCount, int groupIndex, int trackIndex) {
        return mappedTrackInfo.getTrackFormatSupport(rendererCount, groupIndex, trackIndex)
                == RendererCapabilities.FORMAT_HANDLED;
    }

    public boolean isAdaptive(TrackGroupArray trackGroupArray, int rendererIndex, int groupIndex) {
        return adaptiveTrackSelectionFactory != null
                && mappedTrackInfo.getAdaptiveSupport(rendererIndex, groupIndex, false)
                != RendererCapabilities.ADAPTIVE_NOT_SUPPORTED
                && trackGroupArray.get(groupIndex).length > 1;
    }

    public void setTrackReadyListener(ExoPlayerWrapper.TrackInfoReadyListener trackReadyListener) {
        this.trackReadyListener = trackReadyListener;
    }


    public void release() {
        trackReadyListener = null;
        onTrackInfoChanged = null;
        videoMap.clear();
        audioMap.clear();
        subtitleMap.clear();
    }

    private OnTrackInfoChanged initOnTackInfoChanged() {
        return new OnTrackInfoChanged() {
            @Override
            public void customizeTrackInfo(int trackType, String[] updatedInfo) {

                switch (trackType){
                    case TRACK_VIDEO:
                        LinkedHashMap<String, VideoTrackInfo> newVideoMap = new LinkedHashMap<>();
                        // iterate through all the items in map, and leave items that application is interested in.
                        // the index order of the items will be the same as in application.
                        for (String anUpdatedInfo : updatedInfo) {
                            for (int i = 0; i < videoMap.keySet().size(); i++) {
                                VideoTrackInfo videoTrackInfo = videoMap.get(videoMap.keySet().toArray()[i]);
                                if (anUpdatedInfo.equals(videoTrackInfo.getUniqueId())
                                        ||
                                        anUpdatedInfo.equals(String.valueOf(videoTrackInfo.getBitrate()))
                                        ||
                                        anUpdatedInfo.equals(String.valueOf(videoTrackInfo.getWidth()))
                                        ||
                                        anUpdatedInfo.equals(String.valueOf(videoTrackInfo.getHeight()))) {
                                    newVideoMap.put(videoTrackInfo.getUniqueId(), videoTrackInfo);
                                }
                            }
                        }
                        videoMap = newVideoMap;
                        break;
                    case TRACK_AUDIO:
                        LinkedHashMap<String, List<AudioTrackInfo>> newAudioMap = new LinkedHashMap<>();

                        for (String anUpdatedInfo : updatedInfo) {
                            for (int i = 0; i < audioMap.keySet().size(); i++) {
                                List<AudioTrackInfo> audioTrackInfo = audioMap.get(audioMap.keySet().toArray()[i]);
                                if (anUpdatedInfo.equals(audioTrackInfo.get(0).getUniqueId())
                                        ||
                                        anUpdatedInfo.equals(String.valueOf(audioTrackInfo.get(0).getLanguage()))) {
                                    newAudioMap.put(audioTrackInfo.get(0).getUniqueId(), audioTrackInfo);
                                }
                            }
                        }
                        audioMap = newAudioMap;
                        break;
                    case TRACK_SUBTITLE:
                        LinkedHashMap<String, SubtitleTrackInfo> newSubtitleMap = new LinkedHashMap<>();

                        for (String anUpdatedInfo : updatedInfo) {
                            for (int i = 0; i < subtitleMap.keySet().size(); i++) {
                                SubtitleTrackInfo subtitleTrackInfo = subtitleMap.get(subtitleMap.keySet().toArray()[i]);
                                if (anUpdatedInfo.equals(subtitleTrackInfo.getUniqueId()) ||
                                        anUpdatedInfo.equals(subtitleTrackInfo.getLanguage())) {
                                    newSubtitleMap.put(subtitleTrackInfo.getUniqueId(), subtitleTrackInfo);
                                }
                            }
                        }
                        subtitleMap = newSubtitleMap;
                        break;
                }
            }
        };
    }

}
