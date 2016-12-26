package com.kaltura.playkit.player;



import android.view.Surface;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.RendererCapabilities;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.FixedTrackSelection;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.video.VideoRendererEventListener;
import com.kaltura.playkit.AudioTrack;
import com.kaltura.playkit.BaseTrack;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.TextTrack;
import com.kaltura.playkit.PKTracks;
import com.kaltura.playkit.VideoTrack;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.SelectionOverride;
import com.kaltura.playkit.utils.Consts;


import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for generating/sorting/holding and changing track info.
 * Created by anton.afanasiev on 22/11/2016.
 */

class TrackSelectionHelper implements VideoRendererEventListener, AudioRendererEventListener {

    private static final PKLog log = PKLog.get("TrackSelectionHelper");

    private static final TrackSelection.Factory FIXED_FACTORY = new FixedTrackSelection.Factory();


    private static final int TRACK_ADAPTIVE = -1;

    private static final int RENDERER_INDEX = 0;
    private static final int GROUP_INDEX = 1;
    private static final int TRACK_INDEX = 2;
    private static final int TRACK_RENDERERS_AMOUNT = 3;

    private static final String ADAPTIVE_SUFFIX = "adaptive";
    private static final String VIDEO_PREFIX = "Video:";
    private static final String AUDIO_PREFIX = "Audio:";
    private static final String TEXT_PREFIX = "Text:";

    private final DefaultTrackSelector selector;
    private MappingTrackSelector.MappedTrackInfo mappedTrackInfo;
    private final TrackSelection.Factory adaptiveTrackSelectionFactory;
    private ExoPlayerWrapper.TracksInfoListener tracksInfoListener;

    private List<BaseTrack> videoTracks = new ArrayList<>();
    private List<BaseTrack> audioTracks = new ArrayList<>();
    private List<BaseTrack> textTracks = new ArrayList<>();

    private long currentVideoBitrate = Consts.NO_VALUE;
    private long currentAudioBitrate = Consts.NO_VALUE;


    /**
     * @param selector                      The track selector.
     * @param adaptiveTrackSelectionFactory A factory for adaptive video {@link TrackSelection}s,
     *                                      or null if the selection helper should not support adaptive video.
     */
    TrackSelectionHelper(DefaultTrackSelector selector,
                         TrackSelection.Factory adaptiveTrackSelectionFactory) {
        this.selector = selector;
        this.adaptiveTrackSelectionFactory = adaptiveTrackSelectionFactory;
    }

    /**
     * Prepare {@link PKTracks} object for application.
     * When the object is created, notify {@link ExoPlayerWrapper} about that,
     * and pass the {@link PKTracks} as parameter.
     * @return - true if tracks data created successful, if mappingTrackInfo not ready return false.
     */
    boolean prepareTracks() {
        mappedTrackInfo = selector.getCurrentMappedTrackInfo();
        if(mappedTrackInfo == null){
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
                    if (isFormatSupported(rendererIndex, groupIndex, trackIndex) && format.id != null) {
                        String uniqueId = getUniqueId(rendererIndex, groupIndex, trackIndex);
                        switch (rendererIndex) {
                            case Consts.TRACK_TYPE_VIDEO:
                                videoTracks.add(new VideoTrack(uniqueId, format.bitrate, format.width, format.height, false));
                                break;
                            case Consts.TRACK_TYPE_AUDIO:
                                audioTracks.add(new AudioTrack(uniqueId, format.language, format.id, format.bitrate, false));
                                break;
                            case Consts.TRACK_TYPE_TEXT:
                                textTracks.add(new TextTrack(uniqueId, format.language, format.id));
                                break;
                        }
                    }else{
                        log.w("format is not supported for this device. Format bitrate " + format.bitrate + " id " + format.id);
                    }
                }
            }
        }

        return new PKTracks(videoTracks, audioTracks, textTracks);
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
                    videoTracks.add(new VideoTrack(uniqueId, 0, 0, 0, true));
                    break;
                case Consts.TRACK_TYPE_AUDIO:
                    audioTracks.add(new AudioTrack(uniqueId, format.language, format.id, 0, true));
                    break;
                case Consts.TRACK_TYPE_TEXT:
                    textTracks.add(new TextTrack(uniqueId, format.language, format.id));
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
            uniqueStringBuilder.append(ADAPTIVE_SUFFIX);
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

    void changeTrack(String uniqueId) throws IllegalArgumentException {
        if (uniqueId == null) {
            throw new IllegalArgumentException("uniqueId is null");
        }
        if (!isUniqueIdValid(uniqueId)) {
            throw new IllegalArgumentException("The uniqueId is not valid");
        }


        log.i("change track to uniqueID -> " + uniqueId);
        mappedTrackInfo = selector.getCurrentMappedTrackInfo();
        int[] uniqueTrackId = parseUniqueId(uniqueId);
        int rendererIndex = uniqueTrackId[RENDERER_INDEX];

        SelectionOverride override = retrieveOverrideSelection(uniqueTrackId);
        overrideTrack(rendererIndex, override);

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
            if (strArray[i].equals(ADAPTIVE_SUFFIX)) {
                parsedUniqueId[i] = TRACK_ADAPTIVE;
            } else {
                parsedUniqueId[i] = Integer.parseInt(strArray[i]);
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

                    for (int i = 1; i < videoTracks.size(); i++) {
                        videoTrack = (VideoTrack) videoTracks.get(i);
                        if (getIndexFromUniqueId(videoTrack.getUniqueId(), GROUP_INDEX) == groupIndex) {
                            adaptiveTrackIndexesList.add(getIndexFromUniqueId(videoTrack.getUniqueId(), TRACK_INDEX));
                        }
                    }
                    break;
                case Consts.TRACK_TYPE_AUDIO:
                    AudioTrack audioTrack;
                    for (int i = 1; i < audioTracks.size(); i++) {
                        audioTrack = (AudioTrack) audioTracks.get(i);
                        if (getIndexFromUniqueId(audioTrack.getUniqueId(), GROUP_INDEX) == groupIndex) {
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

        List<BaseTrack> trackList = new ArrayList<>();
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
        if (strArray[groupIndex].equals(ADAPTIVE_SUFFIX)) {
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

    private boolean isUniqueIdValid(String uniqueId) {
        if (uniqueId.contains(VIDEO_PREFIX)
                || uniqueId.contains(AUDIO_PREFIX)
                || uniqueId.contains(TEXT_PREFIX)
                && uniqueId.contains(",")) {
            return true;
        }
        log.e("Unique id is not valid => " + uniqueId);
        return false;
    }

    private void warnAboutUnsupportedRenderTypes() {
        if (mappedTrackInfo.getTrackTypeRendererSupport(Consts.TRACK_TYPE_VIDEO)
                == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
            log.w("Error unsupported video");
        }
        if (mappedTrackInfo.getTrackTypeRendererSupport(Consts.TRACK_TYPE_AUDIO)
                == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
            log.w("Error unsupported audio");
        }
    }

    void setTracksInfoListener(ExoPlayerWrapper.TracksInfoListener tracksInfoListener) {
        this.tracksInfoListener = tracksInfoListener;
    }

    private void clearTracksLists() {
        videoTracks.clear();
        audioTracks.clear();
        textTracks.clear();
    }

    public void release() {
        tracksInfoListener = null;
        videoTracks.clear();
        audioTracks.clear();
        textTracks.clear();
    }

    long getCurrentVideoBitrate() {
        return currentVideoBitrate;
    }


    long getCurrentAudioBitrate() {
        return currentAudioBitrate;
    }

    @Override
    public void onAudioEnabled(DecoderCounters counters) {

    }

    @Override
    public void onAudioSessionId(int audioSessionId) {

    }

    @Override
    public void onAudioDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {

    }

    @Override
    public void onAudioInputFormatChanged(Format format) {
        currentAudioBitrate = format.bitrate;
        tracksInfoListener.onTrackChanged();
    }

    @Override
    public void onAudioTrackUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {

    }

    @Override
    public void onAudioDisabled(DecoderCounters counters) {

    }

    @Override
    public void onVideoEnabled(DecoderCounters counters) {

    }

    @Override
    public void onVideoDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {

    }

    @Override
    public void onVideoInputFormatChanged(Format format) {
        currentVideoBitrate = format.bitrate;
        tracksInfoListener.onTrackChanged();
    }

    @Override
    public void onDroppedFrames(int count, long elapsedMs) {

    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {

    }

    @Override
    public void onRenderedFirstFrame(Surface surface) {
    }

    @Override
    public void onVideoDisabled(DecoderCounters counters) {

    }
}
