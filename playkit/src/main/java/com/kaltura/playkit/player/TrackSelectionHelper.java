/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 * 
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 * 
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit.player;


import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.RendererCapabilities;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.FixedTrackSelection;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.SelectionOverride;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.utils.Consts;
import com.kaltura.playkit.PKError;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for generating/sorting/holding and changing track info.
 * Created by anton.afanasiev on 22/11/2016.
 */

class TrackSelectionHelper {

    private static final PKLog log = PKLog.get("TrackSelectionHelper");

    private static final TrackSelection.Factory FIXED_FACTORY = new FixedTrackSelection.Factory();


    private static final int TRACK_ADAPTIVE = -1;
    private static final int TRACK_DISABLED = -2;

    private static final int RENDERER_INDEX = 0;
    private static final int GROUP_INDEX = 1;
    private static final int TRACK_INDEX = 2;
    private static final int TRACK_RENDERERS_AMOUNT = 3;

    static final String NONE = "none";
    private static final String ADAPTIVE = "adaptive";

    private static final String VIDEO = "video";
    private static final String VIDEO_PREFIX = "Video:";

    private static final String AUDIO = "audio";
    private static final String AUDIO_PREFIX = "Audio:";

    private static final String TEXT_PREFIX = "Text:";

    private static final String CEA_608 = "application/cea-608";

    private final DefaultTrackSelector selector;
    private MappingTrackSelector.MappedTrackInfo mappedTrackInfo;
    private final TrackSelection.Factory adaptiveTrackSelectionFactory;
    private TracksInfoListener tracksInfoListener;

    private List<VideoTrack> videoTracks = new ArrayList<>();
    private List<AudioTrack> audioTracks = new ArrayList<>();
    private List<TextTrack> textTracks = new ArrayList<>();

    private String[] lastSelectedTrackIds;

    private long currentVideoBitrate = Consts.NO_VALUE;
    private long currentAudioBitrate = Consts.NO_VALUE;
    private long currentVideoWidth = Consts.NO_VALUE;
    private long currentVideoHeight = Consts.NO_VALUE;


    private boolean cea608CaptionsEnabled; //Flag that indicates if application interested in receiving cea-608 text track format.

    interface TracksInfoListener {

        void onTracksInfoReady(PKTracks PKTracks);

        void onTrackChanged();

        void onRelease(String[] selectedTracks);

        void onError(PKError error);
    }


    /**
     * @param selector                      The track selector.
     * @param adaptiveTrackSelectionFactory A factory for adaptive video {@link TrackSelection}s,
     *                                      or null if the selection helper should not support adaptive video.
     */
    TrackSelectionHelper(DefaultTrackSelector selector,
                         TrackSelection.Factory adaptiveTrackSelectionFactory,
                         String[] lastSelectedTrackIds) {
        this.selector = selector;
        this.adaptiveTrackSelectionFactory = adaptiveTrackSelectionFactory;
        this.lastSelectedTrackIds = lastSelectedTrackIds;
    }

    /**
     * Prepare {@link PKTracks} object for application.
     * When the object is created, notify {@link ExoPlayerWrapper} about that,
     * and pass the {@link PKTracks} as parameter.
     *
     * @return - true if tracks data created successful, if mappingTrackInfo not ready return false.
     */
    boolean prepareTracks() {
        mappedTrackInfo = selector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo == null) {
            log.w("Trying to get current MappedTrackInfo returns null");
            return false;
        }
        warnAboutUnsupportedRenderTypes();
        PKTracks tracksInfo = buildTracks();

        if (tracksInfoListener != null) {
            tracksInfoListener.onTracksInfoReady(tracksInfo);
        }

        return true;
    }

    /**
     * Actually build {@link PKTracks} object, based on the loaded manifest into Exoplayer.
     * This method knows how to filter unsupported/unknown formats, and create adaptive option when this is possible.
     */
    private PKTracks buildTracks() {

        clearTracksLists();

        TrackGroupArray trackGroupArray;
        TrackGroup trackGroup;
        Format format;
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
                    maybeAddAdaptiveTrack(rendererIndex, groupIndex, format);

                    //filter all the unsupported and unknown formats.
                    if (isFormatSupported(rendererIndex, groupIndex, trackIndex)) {
                        String uniqueId = getUniqueId(rendererIndex, groupIndex, trackIndex);
                        switch (rendererIndex) {
                            case Consts.TRACK_TYPE_VIDEO:
                                videoTracks.add(new VideoTrack(uniqueId, format.bitrate, format.width, format.height, format.selectionFlags, false));
                                break;
                            case Consts.TRACK_TYPE_AUDIO:

                                audioTracks.add(new AudioTrack(uniqueId, format.language, format.id, format.bitrate, format.selectionFlags, false));
                                break;
                            case Consts.TRACK_TYPE_TEXT:
                                if (CEA_608.equals(format.sampleMimeType)) {
                                    if (cea608CaptionsEnabled) {
                                        textTracks.add(new TextTrack(uniqueId, format.language, format.id, format.selectionFlags));
                                    }
                                } else {
                                    textTracks.add(new TextTrack(uniqueId, format.language, format.id, format.selectionFlags));
                                }
                                break;
                        }
                    } else {
                        log.w("format is not supported for this device. Format bitrate " + format.bitrate + " id " + format.id);
                    }
                }
            }
        }

        //add disable option to the text tracks.
        maybeAddDisabledTextTrack();

        int defaultVideoTrackIndex = getDefaultTrackIndex(videoTracks, lastSelectedTrackIds[Consts.TRACK_TYPE_VIDEO]);
        int defaultAudioTrackIndex = getDefaultTrackIndex(audioTracks, lastSelectedTrackIds[Consts.TRACK_TYPE_AUDIO]);
        int defaultTextTrackIndex = getDefaultTrackIndex(textTracks, lastSelectedTrackIds[Consts.TRACK_TYPE_TEXT]);

        return new PKTracks(videoTracks, audioTracks, textTracks, defaultVideoTrackIndex, defaultAudioTrackIndex, defaultTextTrackIndex);
    }

    /**
     * Add "disable" text track option.
     * Will add this track only if there is at least one text track available.
     * Selecting this track, will disable the text renderer.
     */
    private void maybeAddDisabledTextTrack() {

        if (textTracks.isEmpty()) {
            return;
        }
        String uniqueId = getUniqueId(Consts.TRACK_TYPE_TEXT, 0, TRACK_DISABLED);
        textTracks.add(0, new TextTrack(uniqueId, NONE, NONE, -1));
    }

    /**
     * Find the default selected track, based on the media manifest.
     *
     * @param trackList - the list of tracks to find the default track.
     * @return - the index of the track that is selected by default or lastSelected track(Depending on the use-case),
     * or 0 if no default selection is available and no track was previously selected.
     */
    private int getDefaultTrackIndex(List<? extends BaseTrack> trackList, String lastSelectedTrackId) {

        int defaultTrackIndex = 0;

        //If no tracks available the default track index will be 0.
        if (trackList.isEmpty()) {
            return defaultTrackIndex;
        }

        for (int i = 0; i < trackList.size(); i++) {
            if (trackList.get(i).getSelectionFlag() == Consts.DEFAULT_TRACK_SELECTION_FLAG) {
                defaultTrackIndex = i;
            }
        }

        return restoreLastSelectedTrack(trackList, lastSelectedTrackId, defaultTrackIndex);
    }

    /**
     * Will restore last selected track, only if there was actual selection and it is
     * differed from the default selection.
     *
     * @param trackList           - the list of tracks to manipulate.
     * @param lastSelectedTrackId - last selected track unique id.
     * @param defaultTrackIndex   - the index of the default track.
     * @return - The index of the last selected track id.
     */
    private int restoreLastSelectedTrack(List<? extends BaseTrack> trackList, String lastSelectedTrackId, int defaultTrackIndex) {
        //If track was previously selected and selection is differed from the default selection apply it.
        String defaultUniqueId = trackList.get(defaultTrackIndex).getUniqueId();
        if (!NONE.equals(lastSelectedTrackId) && !lastSelectedTrackId.equals(defaultUniqueId)) {
            changeTrack(lastSelectedTrackId);
            for (int i = 0; i < trackList.size(); i++) {
                if (lastSelectedTrackId.equals(trackList.get(i).getUniqueId())) {
                    return i;
                }
            }
        }

        return defaultTrackIndex;
    }

    /**
     * If such an option exist, this method creates an adaptive object for the specified renderer.
     *
     * @param rendererIndex - the index of the renderer that this adaptive object refer.
     * @param groupIndex    - the index of the group this adaptive object refer.
     * @param format        - the actual format of the adaptive object.
     */
    private void maybeAddAdaptiveTrack(int rendererIndex, int groupIndex, Format format) {
        String uniqueId = getUniqueId(rendererIndex, groupIndex, TRACK_ADAPTIVE);
        if (isAdaptive(rendererIndex, groupIndex) && !adaptiveTrackAlreadyExist(uniqueId, rendererIndex)) {
            switch (rendererIndex) {
                case Consts.TRACK_TYPE_VIDEO:
                    videoTracks.add(new VideoTrack(uniqueId, 0, 0, 0, format.selectionFlags, true));
                    break;
                case Consts.TRACK_TYPE_AUDIO:
                    audioTracks.add(new AudioTrack(uniqueId, format.language, format.id, 0, format.selectionFlags, true));
                    break;
                case Consts.TRACK_TYPE_TEXT:
                    textTracks.add(new TextTrack(uniqueId, format.language, format.id, format.selectionFlags));
                    break;
            }
        }
    }

    /**
     * Build uniqueId based on the track indexes.
     *
     * @param rendererIndex - renderer index of the current track.
     * @param groupIndex    - group index of the current track.
     * @param trackIndex    - actual track index.
     * @return - uniqueId that represent current track.
     */
    private String getUniqueId(int rendererIndex, int groupIndex, int trackIndex) {
        String rendererPrefix = "";
        switch (rendererIndex) {
            case Consts.TRACK_TYPE_VIDEO:
                rendererPrefix = VIDEO_PREFIX;
                break;
            case Consts.TRACK_TYPE_AUDIO:
                rendererPrefix = AUDIO_PREFIX;
                break;
            case Consts.TRACK_TYPE_TEXT:
                rendererPrefix = TEXT_PREFIX;
                break;
        }
        StringBuilder uniqueStringBuilder = new StringBuilder(rendererPrefix);
        uniqueStringBuilder.append(rendererIndex);
        uniqueStringBuilder.append(",");
        uniqueStringBuilder.append(groupIndex);
        uniqueStringBuilder.append(",");
        if (trackIndex == TRACK_ADAPTIVE) {
            uniqueStringBuilder.append(ADAPTIVE);
        } else if (trackIndex == TRACK_DISABLED) {
            uniqueStringBuilder.append(NONE);
        } else {
            uniqueStringBuilder.append(trackIndex);
        }
        return uniqueStringBuilder.toString();
    }

    /**
     * Change currently playing track with the new one.
     * Throws {@link IllegalArgumentException} if uniqueId is null or uniqueId is not valid format.
     *
     * @param uniqueId - unique identifier of the track to apply.
     */

    void changeTrack(String uniqueId) {

        validateUniqueId(uniqueId);

        log.i("change track to uniqueID -> " + uniqueId);
        mappedTrackInfo = selector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo == null) {
            log.w("Trying to get current MappedTrackInfo returns null. Do not change track with id - " + uniqueId);
            return;
        }
        int[] uniqueTrackId = parseUniqueId(uniqueId);
        int rendererIndex = uniqueTrackId[RENDERER_INDEX];

        lastSelectedTrackIds[rendererIndex] = uniqueId;

        if (shouldDisableTextTrack(uniqueTrackId)) {
            //disable text track
            selector.setRendererDisabled(Consts.TRACK_TYPE_TEXT, true);
            return;
        } else if (rendererIndex == Consts.TRACK_TYPE_TEXT) {
            selector.setRendererDisabled(Consts.TRACK_TYPE_TEXT, false);
        }


        SelectionOverride override = retrieveOverrideSelection(uniqueTrackId);
        overrideTrack(rendererIndex, override);

    }

    private boolean shouldDisableTextTrack(int[] uniqueId) {
        return uniqueId[TRACK_INDEX] == TRACK_DISABLED;
    }

    /**
     * @param uniqueId - the uniqueId to convert.
     * @return - int[] that consist from indexes that are readable to Exoplayer.
     */
    private int[] parseUniqueId(String uniqueId) {
        int[] parsedUniqueId = new int[3];
        String splitUniqueId = removePrefix(uniqueId);
        String[] strArray = splitUniqueId.split(",");

        for (int i = 0; i < strArray.length; i++) {
            switch (strArray[i]) {
                case ADAPTIVE:
                    parsedUniqueId[i] = TRACK_ADAPTIVE;
                    break;
                case NONE:
                    parsedUniqueId[i] = TRACK_DISABLED;
                    break;
                default:
                    parsedUniqueId[i] = Integer.parseInt(strArray[i]);
                    break;
            }
        }
        return parsedUniqueId;
    }

    /**
     * Build the the {@link SelectionOverride} object, based on the uniqueId. This {@link SelectionOverride}
     * will be feeded later to the Exoplayer in order to switch to the new track.
     * This method decide if it should create adaptive override or fixed.
     *
     * @param uniqueId - the unique id of the track that will override the existing one.
     * @return - the {@link SelectionOverride} which will override the existing selection.
     */
    private SelectionOverride retrieveOverrideSelection(int[] uniqueId) {

        SelectionOverride override;

        int rendererIndex = uniqueId[RENDERER_INDEX];
        int groupIndex = uniqueId[GROUP_INDEX];
        int trackIndex = uniqueId[TRACK_INDEX];

        boolean isAdaptive = trackIndex == TRACK_ADAPTIVE;

        if (isAdaptive) {

            List<Integer> adaptiveTrackIndexesList = new ArrayList<>();
            int[] adaptiveTrackIndexes;

            switch (rendererIndex) {
                case Consts.TRACK_TYPE_VIDEO:

                    VideoTrack videoTrack;
                    int videoGroupIndex;
                    int videoTrackIndex;

                    for (int i = 0; i < videoTracks.size(); i++) {

                        videoTrack = videoTracks.get(i);
                        videoGroupIndex = getIndexFromUniqueId(videoTrack.getUniqueId(), GROUP_INDEX);
                        videoTrackIndex = getIndexFromUniqueId(videoTrack.getUniqueId(), TRACK_INDEX);

                        if (videoGroupIndex == groupIndex && videoTrackIndex != TRACK_ADAPTIVE) {
                            adaptiveTrackIndexesList.add(getIndexFromUniqueId(videoTrack.getUniqueId(), TRACK_INDEX));
                        }
                    }
                    break;
                case Consts.TRACK_TYPE_AUDIO:

                    AudioTrack audioTrack;
                    int audioGroupIndex;
                    int audioTrackIndex;

                    for (int i = 0; i < audioTracks.size(); i++) {

                        audioTrack = audioTracks.get(i);
                        audioGroupIndex = getIndexFromUniqueId(audioTrack.getUniqueId(), GROUP_INDEX);
                        audioTrackIndex = getIndexFromUniqueId(audioTrack.getUniqueId(), TRACK_INDEX);

                        if (audioGroupIndex == groupIndex && audioTrackIndex != TRACK_ADAPTIVE) {
                            adaptiveTrackIndexesList.add(getIndexFromUniqueId(audioTrack.getUniqueId(), TRACK_INDEX));
                        }
                    }
                    break;
            }

            adaptiveTrackIndexes = convertAdaptiveListToArray(adaptiveTrackIndexesList);
            override = new SelectionOverride(adaptiveTrackSelectionFactory, groupIndex, adaptiveTrackIndexes);
        } else {
            override = new SelectionOverride(FIXED_FACTORY, groupIndex, trackIndex);
        }

        return override;
    }

    /**
     * Actually doing the override acrion on the track.
     *
     * @param rendererIndex - renderer index on which we want to apply the change.
     * @param override      - the new selection with which we want to override the currently active track.
     */
    private void overrideTrack(int rendererIndex, SelectionOverride override) {
        //if renderer is disabled we will hide it.
        boolean isRendererDisabled = selector.getRendererDisabled(rendererIndex);
        selector.setRendererDisabled(rendererIndex, isRendererDisabled);
        if (override != null) {
            //actually change track.
            TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(rendererIndex);
            selector.setSelectionOverride(rendererIndex, trackGroups, override);
        } else {
            //clear all the selections if the override is null.
            selector.clearSelectionOverrides(rendererIndex);
        }
    }

    /**
     * Checks if adaptive track for the specified group was created.
     *
     * @param uniqueId      - unique id.
     * @param rendererIndex - renderer index.
     * @return - true, if adaptive {@link BaseTrack} object already exist for this group.
     */
    private boolean adaptiveTrackAlreadyExist(String uniqueId, int rendererIndex) {

        List<? extends BaseTrack> trackList = new ArrayList<>();
        switch (rendererIndex) {
            case Consts.TRACK_TYPE_VIDEO:
                trackList = videoTracks;
                break;
            case Consts.TRACK_TYPE_AUDIO:
                trackList = audioTracks;
                break;
            case Consts.TRACK_TYPE_TEXT:
                trackList = textTracks;
                break;
        }

        for (BaseTrack track : trackList) {
            if (track.getUniqueId().equals(uniqueId)) {
                return true;
            }
        }
        return false;
    }

    private int getIndexFromUniqueId(String uniqueId, int groupIndex) {
        String uniqueIdWithoutPrefix = removePrefix(uniqueId);
        String[] strArray = uniqueIdWithoutPrefix.split(",");
        if (strArray[groupIndex].equals(ADAPTIVE)) {
            return -1;
        }

        return Integer.valueOf(strArray[groupIndex]);
    }


    private String removePrefix(String uniqueId) {
        String[] strArray = uniqueId.split(":");
        //always return the second element of the splitString.
        return strArray[1];
    }

    private int[] convertAdaptiveListToArray(List<Integer> adaptiveTrackIndexesList) {
        int[] adaptiveTrackIndexes = new int[adaptiveTrackIndexesList.size()];
        for (int i = 0; i < adaptiveTrackIndexes.length; i++) {
            adaptiveTrackIndexes[i] = adaptiveTrackIndexesList.get(i);
        }

        return adaptiveTrackIndexes;
    }

    private boolean isFormatSupported(int rendererCount, int groupIndex, int trackIndex) {
        return mappedTrackInfo.getTrackFormatSupport(rendererCount, groupIndex, trackIndex)
                == RendererCapabilities.FORMAT_HANDLED;
    }

    private boolean isAdaptive(int rendererIndex, int groupIndex) {
        TrackGroupArray trackGroupArray = mappedTrackInfo.getTrackGroups(rendererIndex);
        return adaptiveTrackSelectionFactory != null
                && mappedTrackInfo.getAdaptiveSupport(rendererIndex, groupIndex, false)
                != RendererCapabilities.ADAPTIVE_NOT_SUPPORTED
                && trackGroupArray.get(groupIndex).length > 1;
    }

    private void validateUniqueId(String uniqueId) throws IllegalArgumentException{

        if (uniqueId == null) {
            throw new IllegalArgumentException("uniqueId is null");
        }

        if (uniqueId.contains(VIDEO_PREFIX)
                || uniqueId.contains(AUDIO_PREFIX)
                || uniqueId.contains(TEXT_PREFIX)
                && uniqueId.contains(",")) {
            return;
        }

        throw new IllegalArgumentException("invalid structure of uniqueId " + uniqueId);
    }

    /**
     * Notify to log, that video/audio renderer have only unsupported tracks.
     */
    private void warnAboutUnsupportedRenderTypes() {
        if (mappedTrackInfo.getTrackTypeRendererSupport(Consts.TRACK_TYPE_VIDEO)
                == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
            log.w("Warning! All the video tracks are unsupported by this device.");
        }
        if (mappedTrackInfo.getTrackTypeRendererSupport(Consts.TRACK_TYPE_AUDIO)
                == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
            log.w("Warning! All the audio tracks are unsupported by this device.");
        }
    }

    void setTracksInfoListener(TracksInfoListener tracksInfoListener) {
        this.tracksInfoListener = tracksInfoListener;
    }

    private void clearTracksLists() {
        videoTracks.clear();
        audioTracks.clear();
        textTracks.clear();
    }

    void release() {
        tracksInfoListener.onRelease(lastSelectedTrackIds);
        tracksInfoListener = null;
        clearTracksLists();
    }

    long getCurrentVideoBitrate() {
        return currentVideoBitrate;
    }

    long getCurrentAudioBitrate() {
        return currentAudioBitrate;
    }

    long getCurrentVideoWidth() {
        return currentVideoWidth;
    }

    long getCurrentVideoHeight() {
        return currentVideoHeight;
    }

    void updateSelectedTracksBitrate(TrackSelectionArray trackSelections) {
        if (tracksInfoListener == null || trackSelections == null) {
            return;
        }

        for (TrackSelection trackSelection : trackSelections.getAll()) {
            if (trackSelection == null || trackSelection.getSelectedFormat() == null) {
                continue;
            }

            String sampleMimeType = "";
            String containerMimeType = "";
            if (trackSelection.getSelectedFormat().sampleMimeType != null) {
                sampleMimeType = trackSelection.getSelectedFormat().sampleMimeType;
            }
            if (trackSelection.getSelectedFormat().containerMimeType != null) {
                containerMimeType = trackSelection.getSelectedFormat().containerMimeType;
            }

            if ("".equals(sampleMimeType) && "".equals(containerMimeType)) {
                continue;
            }
            log.d("sampleMimeType = " + sampleMimeType);
            log.d("containerMimeType = " + containerMimeType);

            String auto = "";
            if ((sampleMimeType.contains(VIDEO) || containerMimeType.contains(VIDEO))) {

                if (trackSelection instanceof AdaptiveTrackSelection) {
                    auto = " Auto";
                }
                log.d("Selected" + auto + " video bitrate = " + trackSelection.getSelectedFormat().bitrate);
                currentVideoBitrate = trackSelection.getSelectedFormat().bitrate;
                currentVideoWidth = trackSelection.getSelectedFormat().width;
                currentVideoHeight = trackSelection.getSelectedFormat().height;
            } else if ((sampleMimeType.contains(AUDIO) || containerMimeType.contains(AUDIO))) {
                if (trackSelection instanceof AdaptiveTrackSelection) {
                    auto = " Auto";
                }
                log.d("Selected" + auto + " audio bitrate = " + trackSelection.getSelectedFormat().bitrate);
                currentAudioBitrate = trackSelection.getSelectedFormat().bitrate;
            }
        }
        tracksInfoListener.onTrackChanged();
    }

    void setCea608CaptionsEnabled(boolean cea608CaptionsEnabled) {
        this.cea608CaptionsEnabled = cea608CaptionsEnabled;
    }
}

