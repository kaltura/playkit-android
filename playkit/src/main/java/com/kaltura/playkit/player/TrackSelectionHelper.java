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


import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kaltura.android.exoplayer2.Format;
import com.kaltura.android.exoplayer2.RendererCapabilities;
import com.kaltura.android.exoplayer2.source.TrackGroup;
import com.kaltura.android.exoplayer2.source.TrackGroupArray;
import com.kaltura.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.kaltura.android.exoplayer2.trackselection.DefaultTrackSelector.SelectionOverride;
import com.kaltura.android.exoplayer2.trackselection.MappingTrackSelector;
import com.kaltura.android.exoplayer2.trackselection.TrackSelection;
import com.kaltura.android.exoplayer2.trackselection.TrackSelectionArray;
import com.kaltura.playkit.PKError;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKTrackConfig;
import com.kaltura.playkit.utils.Consts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;

import static com.kaltura.playkit.utils.Consts.TRACK_TYPE_AUDIO;
import static com.kaltura.playkit.utils.Consts.TRACK_TYPE_TEXT;
import static com.kaltura.playkit.utils.Consts.TRACK_TYPE_UNKNOWN;
import static com.kaltura.playkit.utils.Consts.TRACK_TYPE_VIDEO;

/**
 * Responsible for generating/sorting/holding and changing track info.
 * Created by anton.afanasiev on 22/11/2016.
 */

class TrackSelectionHelper {

    private static final PKLog log = PKLog.get("TrackSelectionHelper");

    private static final int TRACK_ADAPTIVE = -1;
    private static final int TRACK_DISABLED = -2;

    private static final int RENDERER_INDEX = 0;
    private static final int GROUP_INDEX = 1;
    private static final int TRACK_INDEX = 2;
    private static final int TRACK_RENDERERS_AMOUNT = 3;

    static final String NONE = "none";
    private static final String ADAPTIVE = "adaptive";

    private static final String VIDEO_PREFIX = "Video:";
    private static final String AUDIO_PREFIX = "Audio:";
    private static final String TEXT_PREFIX = "Text:";

    private static final String CEA_608 = "application/cea-608";
    private static final String LANGUAGE_UNKNOWN = "Unknown";


    private final DefaultTrackSelector selector;
    private TrackSelectionArray trackSelectionArray;
    private MappingTrackSelector.MappedTrackInfo mappedTrackInfo;

    private List<VideoTrack> videoTracks = new ArrayList<>();
    private List<AudioTrack> audioTracks = new ArrayList<>();
    private List<TextTrack> textTracks = new ArrayList<>();

    private String[] lastSelectedTrackIds;
    private String[] requestedChangeTrackIds;

    private PKTrackConfig preferredAudioLanguageConfig;
    private PKTrackConfig preferredTextLanguageConfig;

    private boolean cea608CaptionsEnabled; //Flag that indicates if application interested in receiving cea-608 text track format.

    private boolean mpgaAudioFormatEnabled; // Flag that indicates if application interested MPGA Audio format

    private TracksInfoListener tracksInfoListener;

    private TracksErrorListener tracksErrorListener;

    interface TracksInfoListener {

        void onTracksInfoReady(PKTracks PKTracks);

        void onRelease(String[] selectedTracks);

        void onVideoTrackChanged();

        void onAudioTrackChanged();

        void onTextTrackChanged();
    }

    interface TracksErrorListener {

        void onTracksOverrideABRError(PKError pkError);
    }

    enum TrackType {
        UNKNOWN, VIDEO, AUDIO, TEXT
    }

    /**
     * @param selector             The track selector.
     * @param lastSelectedTrackIds - last selected track id`s.
     */
    TrackSelectionHelper(DefaultTrackSelector selector,
                         String[] lastSelectedTrackIds) {
        this.selector = selector;
        this.lastSelectedTrackIds = lastSelectedTrackIds;
        this.requestedChangeTrackIds = Arrays.copyOf(lastSelectedTrackIds, lastSelectedTrackIds.length);
    }

    /**
     * Prepare {@link PKTracks} object for application.
     * When the object is created, notify {@link ExoPlayerWrapper} about that,
     * and pass the {@link PKTracks} as parameter.
     * @param trackSelections the selected tracks.
     *
     * @return - true if tracks data created successful, if mappingTrackInfo not ready return false.
     */
    protected boolean prepareTracks(TrackSelectionArray trackSelections) {
        trackSelectionArray = trackSelections;
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
            TrackType trackType = getTrackType(rendererIndex);
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
                            case TRACK_TYPE_VIDEO:
                                if (format.bitrate == -1 && format.codecs == null) {
                                    continue;
                                }
                                videoTracks.add(new VideoTrack(uniqueId, format.bitrate, format.width, format.height, format.selectionFlags, false));
                                break;
                            case TRACK_TYPE_AUDIO:
                                if (format.language == null && format.codecs == null) {
                                    if (mpgaAudioFormatEnabled && format.id != null && format.id.matches("\\d+/\\d+")) {
                                        audioTracks.add(new AudioTrack(uniqueId, format.id, format.label, format.bitrate, format.channelCount, format.selectionFlags, false));
                                    }
                                } else {
                                    audioTracks.add(new AudioTrack(uniqueId, getLanguageFromFormat(format), format.label, format.bitrate, format.channelCount, format.selectionFlags, false));
                                }
                                break;
                            case TRACK_TYPE_TEXT:
                                if (CEA_608.equals(format.sampleMimeType)) {
                                    if (cea608CaptionsEnabled) {
                                        textTracks.add(new TextTrack(uniqueId, format.language, format.id, format.selectionFlags));
                                    }
                                } else {
                                    textTracks.add(new TextTrack(uniqueId, getLanguageFromFormat(format), format.label, format.selectionFlags));
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
        //Leave only adaptive audio tracks for user selection.
        ArrayList<AudioTrack> filteredAudioTracks = filterAdaptiveAudioTracks();

        int defaultVideoTrackIndex = getDefaultTrackIndex(videoTracks, lastSelectedTrackIds[TRACK_TYPE_VIDEO]);
        int defaultAudioTrackIndex = getDefaultTrackIndex(filteredAudioTracks, lastSelectedTrackIds[TRACK_TYPE_AUDIO]);
        int defaultTextTrackIndex = getDefaultTrackIndex(textTracks, lastSelectedTrackIds[TRACK_TYPE_TEXT]);

        return new PKTracks(videoTracks, filteredAudioTracks, textTracks, defaultVideoTrackIndex, defaultAudioTrackIndex, defaultTextTrackIndex);
    }

    @NonNull
    private TrackType getTrackType(int rendererIndex) {
        TrackType trackType;
        switch (rendererIndex) {
            case TRACK_TYPE_VIDEO:
                trackType = TrackType.VIDEO;
                break;
            case TRACK_TYPE_AUDIO:
                trackType = TrackType.AUDIO;
                break;
            case TRACK_TYPE_TEXT:
                trackType = TrackType.TEXT;
                break;
            default:
                trackType = TrackType.UNKNOWN;
                break;
        }
        return trackType;
    }

    private String getLanguageFromFormat(Format format) {
        if (format.language == null) {
            return LANGUAGE_UNKNOWN;
        }
        return format.language;
    }

    /**
     * Filter audioTracks, so that if it contain any sort of adaptive streams
     * PKTracks will expose only adaptive option
     * and hide all the bitrate variants.
     */
    private ArrayList<AudioTrack> filterAdaptiveAudioTracks() {
        ArrayList<AudioTrack> filteredAudioTracks = new ArrayList<>();

        AudioTrack audioTrack;
        int[] parsedUniqueId;
        int currentGroup = -1;

        for (int i = 0; i < audioTracks.size(); i++) {
            audioTrack = audioTracks.get(i);
            parsedUniqueId = parseUniqueId(audioTrack.getUniqueId());

            if (parsedUniqueId[TRACK_INDEX] == TRACK_ADAPTIVE) {
                filteredAudioTracks.add(audioTrack);
                currentGroup = parsedUniqueId[GROUP_INDEX];
            } else if (parsedUniqueId[GROUP_INDEX] != currentGroup) {
                filteredAudioTracks.add(audioTrack);
                currentGroup = -1;
            }
        }

        return filteredAudioTracks;
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
        String uniqueId = getUniqueId(TRACK_TYPE_TEXT, 0, TRACK_DISABLED);
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
            if (trackList.get(i) != null) {
                int selectionFlag = trackList.get(i).getSelectionFlag();
                if (selectionFlag == Consts.DEFAULT_TRACK_SELECTION_FLAG_HLS || selectionFlag == Consts.DEFAULT_TRACK_SELECTION_FLAG_DASH) {
                    defaultTrackIndex = i;
                }
            }
        }

        return restoreLastSelectedTrack(trackList, lastSelectedTrackId, getUpdatedDefaultTrackIndex(trackList, defaultTrackIndex));
    }

    private int getUpdatedDefaultTrackIndex(List<? extends BaseTrack> trackList, int defaultTrackIndex) {

        if (!trackList.isEmpty()) {
            int trackType = TRACK_TYPE_UNKNOWN;
            if (trackList.get(0) instanceof AudioTrack) {
                trackType = TRACK_TYPE_AUDIO;
            } else if (trackList.get(0) instanceof TextTrack) {
                trackType = TRACK_TYPE_TEXT;
            }

            if (trackType != TRACK_TYPE_UNKNOWN && trackSelectionArray != null && trackType < trackSelectionArray.length) {
                TrackSelection trackSelection = trackSelectionArray.get(trackType);
                if (trackSelection != null && trackSelection.getSelectedFormat() != null) {
                    defaultTrackIndex = findDefaultTrackIndex(trackSelection.getSelectedFormat().language, trackList, defaultTrackIndex);
                }
            }
        }

        return defaultTrackIndex;
    }

    private int findDefaultTrackIndex(String selectedFormatLanguage, List<? extends BaseTrack> trackList, int defaultTrackIndex) {
        if (trackList != null && selectedFormatLanguage != null) {

            for (int i = 0; i < trackList.size(); i++) {
                    if (trackList.get(i) != null && selectedFormatLanguage.equals(trackList.get(i).getLanguage())) {
                        defaultTrackIndex = i;
                        break;
                    }
            }
        }

        return defaultTrackIndex;
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
                case TRACK_TYPE_VIDEO:
                    videoTracks.add(new VideoTrack(uniqueId, 0, 0, 0, format.selectionFlags, true));
                    break;
                case TRACK_TYPE_AUDIO:
                    audioTracks.add(new AudioTrack(uniqueId, format.language, format.label, 0, format.channelCount, format.selectionFlags, true));
                    break;
                case TRACK_TYPE_TEXT:
                    textTracks.add(new TextTrack(uniqueId, format.language, format.label, format.selectionFlags));
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
        return getUniqueIdPrefix(rendererIndex)
                + rendererIndex
                + ","
                + groupIndex
                + ","
                + getUniqueIdPostfix(rendererIndex, trackIndex);
    }

    private String getUniqueIdPrefix(int rendererIndex) {
        switch (rendererIndex) {
            case TRACK_TYPE_VIDEO:
                return VIDEO_PREFIX;
            case TRACK_TYPE_AUDIO:
                return AUDIO_PREFIX;
            case TRACK_TYPE_TEXT:
                return TEXT_PREFIX;
            default:
                return "";
        }
    }

    private String getUniqueIdPostfix(int rendererIndex, int trackIndex) {
        switch (rendererIndex) {
            case TRACK_ADAPTIVE:
                return ADAPTIVE;
            case TRACK_DISABLED:
                return NONE;
            default:
                return String.valueOf(trackIndex);
        }
    }

    /**
     * Change currently playing track with the new one.
     * Throws {@link IllegalArgumentException} if uniqueId is null or uniqueId is not valid format.
     *
     * @param uniqueId - unique identifier of the track to apply.
     */

    protected void changeTrack(String uniqueId) {
        log.i("Request change track to uniqueID -> " + uniqueId);
        mappedTrackInfo = selector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo == null) {
            log.w("Trying to get current MappedTrackInfo returns null. Do not change track with id - " + uniqueId);
            return;
        }

        int[] uniqueTrackId = validateUniqueId(uniqueId);
        int rendererIndex = uniqueTrackId[RENDERER_INDEX];

        requestedChangeTrackIds[rendererIndex] = uniqueId;

        DefaultTrackSelector.ParametersBuilder parametersBuilder = selector.getParameters().buildUpon();
        if (rendererIndex == TRACK_TYPE_TEXT) {
            //Disable text track renderer if needed.
            parametersBuilder.setRendererDisabled(TRACK_TYPE_TEXT, uniqueTrackId[TRACK_INDEX] == TRACK_DISABLED);
        }


        SelectionOverride override = retrieveOverrideSelection(uniqueTrackId);
        overrideTrack(rendererIndex, override, parametersBuilder);
    }

    public void overrideMediaDefaultABR(long minVideoBitrate, long maxVideoBitrate) {

        List<String> uniqueIds = getABRUniqueIds(minVideoBitrate, maxVideoBitrate);
        mappedTrackInfo = selector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo == null || uniqueIds.isEmpty()) {
            return;
        }

        int[] uniqueTrackId = validateUniqueId(uniqueIds.get(0));
        int rendererIndex = uniqueTrackId[RENDERER_INDEX];

        requestedChangeTrackIds[rendererIndex] = uniqueIds.get(0);

        DefaultTrackSelector.ParametersBuilder parametersBuilder = selector.getParameters().buildUpon();


        SelectionOverride override = retrieveOverrideSelectionList(validateAndBuildUniqueIds(uniqueIds));
        overrideTrack(rendererIndex, override, parametersBuilder);
    }

    private List<String> getABRUniqueIds(long minVideoBitrate, long maxVideoBitrate) {
        List<String> uniqueIds = new ArrayList<>();
        boolean isValidABRRange = true;
        if (videoTracks != null) {
            Collections.sort(videoTracks);
            if (videoTracks.size() >= 2) {
                if ((minVideoBitrate < videoTracks.get(1).getBitrate() && maxVideoBitrate <  videoTracks.get(1).getBitrate()) ||
                        (minVideoBitrate > videoTracks.get(videoTracks.size() - 1).getBitrate() && maxVideoBitrate > videoTracks.get(videoTracks.size() - 1).getBitrate())) {
                    isValidABRRange = false;
                    String errorMessage = "given minVideoBitrate or maxVideoBitrate is invalid";
                    PKError currentError = new PKError(PKPlayerErrorType.UNEXPECTED, PKError.Severity.Recoverable, errorMessage, new IllegalArgumentException(errorMessage));
                    tracksErrorListener.onTracksOverrideABRError(currentError);
                }
            }
            Iterator<VideoTrack> videoTrackIterator = videoTracks.iterator();
            while (videoTrackIterator.hasNext()) {
                VideoTrack currentVideoTrack = videoTrackIterator.next();
                if (currentVideoTrack.isAdaptive() || (currentVideoTrack.getBitrate() >= minVideoBitrate && currentVideoTrack.getBitrate() <= maxVideoBitrate)) {
                    uniqueIds.add(currentVideoTrack.getUniqueId());
                } else {
                    if (!isValidABRRange) {
                        uniqueIds.add(currentVideoTrack.getUniqueId());
                    } else {
                        videoTrackIterator.remove();
                    }
                }
            }
        }
        return uniqueIds;
    }

    private SelectionOverride retrieveOverrideSelectionList(int[][] uniqueIds) {
        if (uniqueIds == null || uniqueIds[0] == null) {
            throw new IllegalArgumentException("Track selection with uniqueId = null");
        }

        // Only for video tracks : RENDERER_INDEX is always 0 means video
        SelectionOverride override;
        int rendererIndex = uniqueIds[0][RENDERER_INDEX];
        int groupIndex = uniqueIds[0][GROUP_INDEX];
        int trackIndex = uniqueIds[0][TRACK_INDEX];

        boolean isAdaptive = trackIndex == TRACK_ADAPTIVE;

        if (uniqueIds.length == 1 && isAdaptive) {
            override = overrideAutoABRTracks(rendererIndex, groupIndex);
        } else if (uniqueIds.length > 1) {
            override = overrideMediaDefaultABR(uniqueIds, rendererIndex, groupIndex);
        } else {
            override = new SelectionOverride(groupIndex, trackIndex);
        }
        return override;
    }

    @NonNull
    private SelectionOverride overrideMediaDefaultABR(int[][] uniqueIds, int rendererIndex, int groupIndex) {
        SelectionOverride override;
        int[] adaptiveTrackIndexes;
        List<Integer> adaptiveTrackIndexesList = new ArrayList<>();

        switch (rendererIndex) {
            case TRACK_TYPE_VIDEO:
            case TRACK_TYPE_AUDIO:
                createAdaptiveTrackIndexList(uniqueIds, groupIndex, adaptiveTrackIndexesList);
                break;
        }
        adaptiveTrackIndexes = convertAdaptiveListToArray(adaptiveTrackIndexesList);
        override = new SelectionOverride(groupIndex, adaptiveTrackIndexes);
        return override;
    }

    private void createAdaptiveTrackIndexList(int[][] uniqueIds, int groupIndex, List<Integer> adaptiveTrackIndexesList) {
        int trackIndex;
        int trackGroupIndex;

        for (int[] uniqueId : uniqueIds) {
            if (uniqueId != null) {
                trackGroupIndex = uniqueId[GROUP_INDEX];
                trackIndex = uniqueId[TRACK_INDEX];

                if (trackGroupIndex == groupIndex && trackIndex != TRACK_ADAPTIVE) {
                    adaptiveTrackIndexesList.add(uniqueId[TRACK_INDEX]);
                }
            }
        }
    }


    private int[][] validateAndBuildUniqueIds(List<String> uniqueIds) {
        int [][] idsList = new int [uniqueIds.size()][TRACK_RENDERERS_AMOUNT];
        for (int index = 0 ; index < idsList.length ; index++) {
            int[] uniqueTrackId = validateUniqueId(uniqueIds.get(index));
            idsList[index] = uniqueTrackId;
        }
        return idsList;
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
     * will fed later to the Exoplayer in order to switch to the new track.
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
                case TRACK_TYPE_VIDEO:

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
                case TRACK_TYPE_AUDIO:

                    AudioTrack audioTrack;
                    int audioGroupIndex;
                    int audioTrackIndex;
                    for (int i = 0; i < audioTracks.size(); i++) {

                        audioTrack = audioTracks.get(i);
                        audioGroupIndex = getIndexFromUniqueId(audioTrack.getUniqueId(), GROUP_INDEX);
                        audioTrackIndex = getIndexFromUniqueId(audioTrack.getUniqueId(), TRACK_INDEX);

                        if (audioGroupIndex == groupIndex && audioTrackIndex == TRACK_ADAPTIVE) {
                            TrackGroup trackGroup = mappedTrackInfo.getTrackGroups(TRACK_TYPE_AUDIO).get(audioGroupIndex);
                            if (trackGroup != null) {
                                for (int ind = 0 ; ind < trackGroup.length ; ind++) {
                                    adaptiveTrackIndexesList.add(ind);
                                }
                            }
                        }
                    }
                    break;
            }

            adaptiveTrackIndexes = convertAdaptiveListToArray(adaptiveTrackIndexesList);
            override = new SelectionOverride(groupIndex, adaptiveTrackIndexes);
        } else {
            override = new SelectionOverride(groupIndex, trackIndex);
        }

        return override;
    }

    @NonNull
    private SelectionOverride overrideAutoABRTracks(int rendererIndex, int groupIndex) {
        SelectionOverride override;List<Integer> adaptiveTrackIndexesList = new ArrayList<>();
        int[] adaptiveTrackIndexes;

        switch (rendererIndex) {
            case TRACK_TYPE_VIDEO:

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
            case TRACK_TYPE_AUDIO:

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
        override = new SelectionOverride(groupIndex, adaptiveTrackIndexes);
        return override;
    }

    /**
     * Actually doing the override action on the track.
     *
     * @param rendererIndex - renderer index on which we want to apply the change.
     * @param override      - the new selection with which we want to override the currently active track.
     */
    private void overrideTrack(int rendererIndex, SelectionOverride override, DefaultTrackSelector.ParametersBuilder parametersBuilder) {
        if (override != null) {
            //actually change track.
            TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(rendererIndex);
            parametersBuilder.setSelectionOverride(rendererIndex, trackGroups, override);
        } else {
            //clear all the selections if the override is null.
            parametersBuilder.clearSelectionOverrides(rendererIndex);
        }
        selector.setParameters(parametersBuilder);
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
            case TRACK_TYPE_VIDEO:
                trackList = videoTracks;
                break;
            case TRACK_TYPE_AUDIO:
                trackList = audioTracks;
                break;
            case TRACK_TYPE_TEXT:
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
        return mappedTrackInfo.getTrackSupport(rendererCount, groupIndex, trackIndex)
                == RendererCapabilities.FORMAT_HANDLED;
    }

    private boolean isAdaptive(int rendererIndex, int groupIndex) {
        TrackGroupArray trackGroupArray = mappedTrackInfo.getTrackGroups(rendererIndex);
        return mappedTrackInfo.getAdaptiveSupport(rendererIndex, groupIndex, false)
                != RendererCapabilities.ADAPTIVE_NOT_SUPPORTED
                && trackGroupArray.get(groupIndex).length > 1;
    }

    /**
     * Validate and return parsed uniqueId.
     *
     * @param uniqueId - uniqueId to validate
     * @return - parsed uniqueId in case of success.
     * @throws IllegalArgumentException when uniqueId is illegal.
     */
    private int[] validateUniqueId(String uniqueId) throws IllegalArgumentException {

        if (uniqueId == null) {
            throw new IllegalArgumentException("uniqueId is null");
        }

        if (uniqueId.contains(VIDEO_PREFIX)
                || uniqueId.contains(AUDIO_PREFIX)
                || uniqueId.contains(TEXT_PREFIX)
                && uniqueId.contains(",")) {

            int[] parsedUniqueId = parseUniqueId(uniqueId);
            if (!isRendererTypeValid(parsedUniqueId[RENDERER_INDEX])) {
                throw new IllegalArgumentException("Track selection with uniqueId = " + uniqueId + " failed. Due to invalid renderer index. " + parsedUniqueId[RENDERER_INDEX]);
            }

            if (!isGroupIndexValid(parsedUniqueId)) {
                throw new IllegalArgumentException("Track selection with uniqueId = " + uniqueId + " failed. Due to invalid group index. " + parsedUniqueId[GROUP_INDEX]);
            }

            if (!isTrackIndexValid(parsedUniqueId)) {
                throw new IllegalArgumentException("Track selection with uniqueId = " + uniqueId + " failed. Due to invalid track index. " + parsedUniqueId[TRACK_INDEX]);
            }
            return parsedUniqueId;
        }
        throw new IllegalArgumentException("Invalid structure of uniqueId " + uniqueId);
    }

    private boolean isTrackIndexValid(int[] parsedUniqueId) {
        int rendererIndex = parsedUniqueId[RENDERER_INDEX];
        int groupIndex = parsedUniqueId[GROUP_INDEX];
        int trackIndex = parsedUniqueId[TRACK_INDEX];

        if (rendererIndex == TRACK_TYPE_TEXT) {
            return trackIndex != TRACK_ADAPTIVE
                    && trackIndex >= TRACK_DISABLED
                    && trackIndex < mappedTrackInfo.getTrackGroups(rendererIndex).get(groupIndex).length;
        }

        return trackIndex >= TRACK_ADAPTIVE
                && trackIndex < mappedTrackInfo.getTrackGroups(rendererIndex).get(groupIndex).length;
    }

    private boolean isGroupIndexValid(int[] parsedUniqueId) {
        return parsedUniqueId[GROUP_INDEX] >= 0
                && parsedUniqueId[GROUP_INDEX] < mappedTrackInfo.getTrackGroups(parsedUniqueId[RENDERER_INDEX]).length;
    }

    private boolean isRendererTypeValid(int rendererIndex) {
        return rendererIndex >= TRACK_TYPE_VIDEO && rendererIndex <= TRACK_TYPE_TEXT;
    }

    /**
     * Notify to log, that video/audio renderer has only unsupported tracks.
     */
    private void warnAboutUnsupportedRenderTypes() {
        if (mappedTrackInfo.getTypeSupport(TRACK_TYPE_VIDEO)
                == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
            log.w("Warning! All the video tracks are unsupported by this device.");
        }
        if (mappedTrackInfo.getTypeSupport(TRACK_TYPE_AUDIO)
                == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
            log.w("Warning! All the audio tracks are unsupported by this device.");
        }
    }

    protected void setTracksInfoListener(TracksInfoListener tracksInfoListener) {
        this.tracksInfoListener = tracksInfoListener;
    }

    protected void setTracksErrorListener(TracksErrorListener tracksErrorListener) {
        this.tracksErrorListener = tracksErrorListener;
    }

    private void clearTracksLists() {
        videoTracks.clear();
        audioTracks.clear();
        textTracks.clear();
    }

    protected void release() {
        tracksInfoListener.onRelease(lastSelectedTrackIds);
        tracksInfoListener = null;
        clearTracksLists();
    }

    protected long getCurrentVideoBitrate() {
        if (trackSelectionArray != null) {
            TrackSelection trackSelection = trackSelectionArray.get(TRACK_TYPE_VIDEO);
            if (trackSelection != null) {
                return trackSelection.getSelectedFormat().bitrate;
            }
        }
        return -1;
    }

    protected long getCurrentAudioBitrate() {
        if (trackSelectionArray != null) {
            TrackSelection trackSelection = trackSelectionArray.get(TRACK_TYPE_AUDIO);
            if (trackSelection != null) {
                return trackSelection.getSelectedFormat().bitrate;
            }
        }
        return -1;
    }

    protected long getCurrentVideoWidth() {
        if (trackSelectionArray != null) {
            TrackSelection trackSelection = trackSelectionArray.get(TRACK_TYPE_VIDEO);
            if (trackSelection != null) {
                return trackSelection.getSelectedFormat().width;
            }
        }
        return -1;
    }

    protected long getCurrentVideoHeight() {
        if (trackSelectionArray != null) {
            TrackSelection trackSelection = trackSelectionArray.get(TRACK_TYPE_VIDEO);
            if (trackSelection != null) {
                return trackSelection.getSelectedFormat().height;
            }
        }
        return -1;
    }

    protected void notifyAboutTrackChange(TrackSelectionArray trackSelections) {

        this.trackSelectionArray = trackSelections;
        if (tracksInfoListener == null) {
            return;
        }

        if (shouldNotifyAboutTrackChanged(TRACK_TYPE_VIDEO)) {
            log.i("Video track changed to: " + requestedChangeTrackIds[TRACK_TYPE_VIDEO]);
            lastSelectedTrackIds[TRACK_TYPE_VIDEO] = requestedChangeTrackIds[TRACK_TYPE_VIDEO];
            tracksInfoListener.onVideoTrackChanged();
        }

        if (shouldNotifyAboutTrackChanged(TRACK_TYPE_AUDIO)) {
            log.i("Audio track changed to: " + requestedChangeTrackIds[TRACK_TYPE_AUDIO]);
            lastSelectedTrackIds[TRACK_TYPE_AUDIO] = requestedChangeTrackIds[TRACK_TYPE_AUDIO];
            tracksInfoListener.onAudioTrackChanged();
        }

        if (shouldNotifyAboutTrackChanged(TRACK_TYPE_TEXT)) {
            log.i("Text track changed to: " + requestedChangeTrackIds[TRACK_TYPE_TEXT]);
            lastSelectedTrackIds[TRACK_TYPE_TEXT] = requestedChangeTrackIds[TRACK_TYPE_TEXT];
            tracksInfoListener.onTextTrackChanged();
        }
    }

    private boolean shouldNotifyAboutTrackChanged(int renderType) {
        return !requestedChangeTrackIds[renderType].equals(lastSelectedTrackIds[renderType]);
    }

    BaseTrack getLastSelectedTrack(int renderType) {

        switch (renderType) {
            case TRACK_TYPE_VIDEO:
                for (VideoTrack track : videoTracks) {
                    if (track.getUniqueId().equals(lastSelectedTrackIds[renderType])) {
                        return track;
                    }
                }
                break;
            case TRACK_TYPE_AUDIO:
                for (AudioTrack track : audioTracks) {
                    if (track.getUniqueId().equals(lastSelectedTrackIds[renderType])) {
                        return track;
                    }
                }
                break;
            case TRACK_TYPE_TEXT:
                for (TextTrack track : textTracks) {
                    if (track.getUniqueId().equals(lastSelectedTrackIds[renderType])) {
                        return track;
                    }
                }
                break;
        }

        log.w("For some reason we could not found lastSelectedTrack of the specified render type = " + renderType);
        return null;
    }

    // clean previous selection
    protected void stop() {
        lastSelectedTrackIds = new String[]{NONE, NONE, NONE};
        requestedChangeTrackIds = new String[]{NONE, NONE, NONE};
        trackSelectionArray = null;
        mappedTrackInfo = null;
        videoTracks.clear();
        audioTracks.clear();
        textTracks.clear();
    }

    /**
     * Helper method which return the uniqueId of the preferred audio/text track. Base on user selection and/or
     * predefined requirements.
     *
     * @param trackType - the type of the track we are looking for (audio/text).
     * @return - uniqueId of the preferred track.
     * If no preferred AudioTrack exist or user defined to use OFF mode, will return null.
     * In case of TextTrack if no preferred track exist will return null.
     * Otherwise will return uniqueId that is corresponded to the selected {@link PKTrackConfig.Mode}.
     */
    protected String getPreferredTrackId(int trackType) {

        String preferredTrackUniqueId = null;
        switch (trackType) {
            case TRACK_TYPE_AUDIO:
                preferredTrackUniqueId = getPreferredAudioTrackUniqueId(trackType);
                break;
            case TRACK_TYPE_TEXT:
                preferredTrackUniqueId = getPreferredTextTrackUniqueId(trackType);
                break;
            default:
                break;
        }
        return preferredTrackUniqueId;
    }

    @Nullable
    private String getPreferredTextTrackUniqueId(int trackType) {
        if (!isValidPreferredTextConfig()) {
            return null;
        }
        String preferredTrackUniqueId = null;
        String preferredTextISO3Lang = preferredTextLanguageConfig.getTrackLanguage();
        if (preferredTextISO3Lang != null) {
            for (TextTrack track : textTracks) {
                String trackLang = track.getLanguage();
                if (trackLang == null) {
                    continue;
                }

                if (NONE.equals(preferredTextLanguageConfig.getTrackLanguage()) && NONE.equals(trackLang)) {
                    preferredTrackUniqueId = track.getUniqueId();
                    break;
                } else if (NONE.equals(trackLang)) {
                    continue;
                }

                Locale streamLang = new Locale(trackLang);
                try {
                    if (streamLang.getISO3Language().equals(preferredTextISO3Lang)) {
                        log.d("changing track type " + trackType + " to " + preferredTextLanguageConfig.getTrackLanguage());
                        preferredTrackUniqueId = track.getUniqueId();
                        break;
                    }
                } catch (MissingResourceException ex) {
                    log.e(ex.getMessage());
                    preferredTrackUniqueId = null;
                }
            }
            if (preferredTrackUniqueId == null) {
                preferredTrackUniqueId = maybeSetFirstTextTrackAsAutoSelection();
            }

        }
        return preferredTrackUniqueId;
    }

    @Nullable
    private String maybeSetFirstTextTrackAsAutoSelection() {
        String preferredTrackUniqueId = null;
        //if user set mode to AUTO and the locale lang is not in the stream and no default text track in the stream so we will not select None but the first text track in the stream
        if (preferredTextLanguageConfig != null && preferredTextLanguageConfig.getPreferredMode() == PKTrackConfig.Mode.AUTO && textTracks != null) {
            for (TextTrack track : textTracks) {
                if (track.getSelectionFlag() == Consts.DEFAULT_TRACK_SELECTION_FLAG_HLS || track.getSelectionFlag() == Consts.DEFAULT_TRACK_SELECTION_FLAG_DASH) {
                    preferredTrackUniqueId = track.getUniqueId();
                    break;
                }
            }
            if (preferredTrackUniqueId == null && textTracks.size() > 1) {
                //take index = 1 since index = 0 is text track "none"
                preferredTrackUniqueId = textTracks.get(1).getUniqueId();
            }
        }
        return preferredTrackUniqueId;
    }

    private String getPreferredAudioTrackUniqueId(int trackType) {
        if (!isValidPreferredAudioConfig()) {
            return null;
        }
        String preferredTrackUniqueId = null;
        String preferredAudioISO3Lang = preferredAudioLanguageConfig.getTrackLanguage();
        for (AudioTrack track : audioTracks) {
            String trackLang = track.getLanguage();
            if (trackLang == null) {
                continue;
            }
            Locale streamLang = new Locale(trackLang);
            try {
                if (streamLang.getISO3Language().equals(preferredAudioISO3Lang)) {
                    log.d("changing track type " + trackType + " to " + preferredAudioLanguageConfig.getTrackLanguage());
                    preferredTrackUniqueId = track.getUniqueId();
                    break;
                }
            } catch (MissingResourceException ex) {
                log.e(ex.getMessage());
            }
        }
        return preferredTrackUniqueId;
    }

    private boolean isValidPreferredAudioConfig() {
        return !(preferredAudioLanguageConfig == null ||
                preferredAudioLanguageConfig.getPreferredMode() == null ||
                preferredAudioLanguageConfig.getPreferredMode() == PKTrackConfig.Mode.OFF ||
                (preferredAudioLanguageConfig.getPreferredMode() == PKTrackConfig.Mode.SELECTION && preferredAudioLanguageConfig.getTrackLanguage() == null));
    }

    private boolean isValidPreferredTextConfig() {
        return !(preferredTextLanguageConfig == null ||
                preferredTextLanguageConfig.getPreferredMode() == null ||
                (preferredTextLanguageConfig.getPreferredMode() == PKTrackConfig.Mode.SELECTION && preferredTextLanguageConfig.getTrackLanguage() == null));
    }

    protected void applyPlayerSettings(PlayerSettings settings) {
        this.mpgaAudioFormatEnabled = settings.mpgaAudioFormatEnabled();
        this.cea608CaptionsEnabled  = settings.cea608CaptionsEnabled();
        this.preferredAudioLanguageConfig = settings.getPreferredAudioTrackConfig();
        this.preferredTextLanguageConfig  = settings.getPreferredTextTrackConfig();
    }

    public static boolean isFormatSupported(@NonNull Format format, @Nullable TrackType type) {

        if (type == TrackType.TEXT) {
            return true;    // always supported
        }

        if (format.codecs == null) {
            log.w("isFormatSupported: codecs==null, assuming supported");
            return true;
        }

        if (type == null) {
            // type==null: HLS muxed track with a <video,audio> tuple
            final String[] split = TextUtils.split(format.codecs, ",");
            boolean result = true;
            switch (split.length) {
                case 0: return false;
                case 2: result = PKCodecSupport.hasDecoder(split[1], false, true);
                    // fallthrough
                case 1: result &= PKCodecSupport.hasDecoder(split[0], false, true);
            }
            return result;

        } else {
            return PKCodecSupport.hasDecoder(format.codecs, false, true);
        }
    }
}
