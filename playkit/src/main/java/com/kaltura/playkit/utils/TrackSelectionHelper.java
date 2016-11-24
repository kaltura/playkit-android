package com.kaltura.playkit.utils;

import android.text.TextUtils;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.RendererCapabilities;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.FixedTrackSelection;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.RandomTrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import com.google.android.exoplayer2.util.MimeTypes;
import com.kaltura.playkit.AudioTrackData;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.SubtitleTrackData;
import com.kaltura.playkit.TrackData;
import com.kaltura.playkit.VideoTrackData;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Created by anton.afanasiev on 22/11/2016.
 */

public class TrackSelectionHelper {

    private static final PKLog log = PKLog.get("TrackSelectionHelper");
    private static final String TAG = TrackSelectionHelper.class.getSimpleName();


    private static final TrackSelection.Factory FIXED_FACTORY = new FixedTrackSelection.Factory();
    private static final TrackSelection.Factory RANDOM_FACTORY = new RandomTrackSelection.Factory();
    private static final int RENDERERS_AMOUNT = 3;
    private static final int TRACK_SUBTITLE = 2;
    private static final int TRACK_AUDIO = 1;
    private static final int TRACK_VIDEO = 0;

    private final MappingTrackSelector selector;
    private final TrackSelection.Factory adaptiveVideoTrackSelectionFactory;

    private MappingTrackSelector.MappedTrackInfo trackInfo;
    private int trackType;
    private TrackGroupArray trackGroups;
    private boolean[] trackGroupsAdaptive;
    private boolean isDisabled;
    private MappingTrackSelector.SelectionOverride override;

    private TrackData trackData;
    private boolean[] isAdaptiveList;

    /**
     * @param selector                           The track selector.
     * @param adaptiveVideoTrackSelectionFactory A factory for adaptive video {@link TrackSelection}s,
     *                                           or null if the selection helper should not support adaptive video.
     */
    public TrackSelectionHelper(MappingTrackSelector selector,
                                TrackSelection.Factory adaptiveVideoTrackSelectionFactory) {
        this.selector = selector;
        this.adaptiveVideoTrackSelectionFactory = adaptiveVideoTrackSelectionFactory;
    }


    private void setOverride(int group, int[] tracks, boolean enableRandomAdaptation) {
        TrackSelection.Factory factory = tracks.length == 1 ? FIXED_FACTORY
                : (enableRandomAdaptation ? RANDOM_FACTORY : adaptiveVideoTrackSelectionFactory);
        override = new MappingTrackSelector.SelectionOverride(factory, group, tracks);
    }

    // Track array manipulation.

    private static int[] getTracksAdding(MappingTrackSelector.SelectionOverride override, int addedTrack) {
        int[] tracks = override.tracks;
        tracks = Arrays.copyOf(tracks, tracks.length + 1);
        tracks[tracks.length - 1] = addedTrack;
        return tracks;
    }

    private static int[] getTracksRemoving(MappingTrackSelector.SelectionOverride override, int removedTrack) {
        int[] tracks = new int[override.length - 1];
        int trackCount = 0;
        for (int i = 0; i < tracks.length + 1; i++) {
            int track = override.tracks[i];
            if (track != removedTrack) {
                tracks[trackCount++] = track;
            }
        }
        return tracks;
    }

    // Track name construction.

    private static String buildTrackName(Format format) {
        String trackName;
        if (MimeTypes.isVideo(format.sampleMimeType)) {
            trackName = joinWithSeparator(joinWithSeparator(buildResolutionString(format),
                    buildBitrateString(format)), buildTrackIdString(format));
        } else if (MimeTypes.isAudio(format.sampleMimeType)) {
            trackName = joinWithSeparator(joinWithSeparator(joinWithSeparator(buildLanguageString(format),
                    buildAudioPropertyString(format)), buildBitrateString(format)),
                    buildTrackIdString(format));
        } else {
            trackName = joinWithSeparator(joinWithSeparator(buildLanguageString(format),
                    buildBitrateString(format)), buildTrackIdString(format));
        }
        return trackName.length() == 0 ? "unknown" : trackName;
    }

    private static String buildResolutionString(Format format) {
        return format.width == Format.NO_VALUE || format.height == Format.NO_VALUE
                ? "" : format.width + "x" + format.height;
    }

    private static String buildAudioPropertyString(Format format) {
        return format.channelCount == Format.NO_VALUE || format.sampleRate == Format.NO_VALUE
                ? "" : format.channelCount + "ch, " + format.sampleRate + "Hz";
    }

    private static String buildLanguageString(Format format) {
        return TextUtils.isEmpty(format.language) || "und".equals(format.language) ? ""
                : format.language;
    }

    private static String buildBitrateString(Format format) {
        return format.bitrate == Format.NO_VALUE ? ""
                : String.format(Locale.US, "%.2fMbit", format.bitrate / 1000000f);
    }

    private static String joinWithSeparator(String first, String second) {
        return first.length() == 0 ? second : (second.length() == 0 ? first : first + ", " + second);
    }

    private static String buildTrackIdString(Format format) {
        return format.id == null ? "" : ("id:" + format.id);
    }

    public void changeTrack(int trackType, int position, MappedTrackInfo trackInfo) {
        log.e(TAG, "track type " + trackType + " position " + position);
        this.trackType = trackType;
        trackGroups = trackInfo.getTrackGroups(this.trackType);
        isDisabled = selector.getRendererDisabled(this.trackType);
        override = selector.getSelectionOverride(this.trackType, trackGroups);

        if (isAdaptiveList[this.trackType]) {
            override = new MappingTrackSelector.SelectionOverride(FIXED_FACTORY, 0, position);
        } else {
            override = new MappingTrackSelector.SelectionOverride(FIXED_FACTORY, position, 0);
        }


        //last part
        selector.setRendererDisabled(this.trackType, isDisabled);
        if (override != null) {
            selector.setSelectionOverride(this.trackType, trackGroups, override);
        } else {
            selector.clearSelectionOverrides(this.trackType);
        }
    }

    public TrackData getTrackData() {
        return this.trackData;
    }

    public void prepareTrackData(MappedTrackInfo trackInfo) {

        this.trackInfo = trackInfo;
        TrackGroupArray trackGroupArray;
        TrackGroup trackGroup;
        Format format;

        List<VideoTrackData> videoTrackDataList = new ArrayList<>();
        List<AudioTrackData> audioTrackDataList = new ArrayList<>();
        List<SubtitleTrackData> subtitleTrackDataList = new ArrayList<>();
        isAdaptiveList = new boolean[3];

        VideoTrackData videoTrackData;
        AudioTrackData audioTrackData;
        SubtitleTrackData subtitleTrackData;

        boolean isAdaptive;

        for (int rendererCount = 0; rendererCount < RENDERERS_AMOUNT; rendererCount++) {
            trackGroupArray = this.trackInfo.getTrackGroups(rendererCount);

            for (int groupIndex = 0; groupIndex < trackGroupArray.length; groupIndex++) {
                trackGroup = trackGroupArray.get(groupIndex);

                for (int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {
                    if (trackInfo.getTrackFormatSupport(rendererCount, groupIndex, trackIndex)
                            == RendererCapabilities.FORMAT_HANDLED) {
                    isAdaptive = adaptiveVideoTrackSelectionFactory != null
                            && trackInfo.getAdaptiveSupport(rendererCount, groupIndex, false)
                            != RendererCapabilities.ADAPTIVE_NOT_SUPPORTED
                            && trackGroupArray.get(groupIndex).length > 1;

                    }else{
                        isAdaptive = true;
                    }
                    format = trackGroup.getFormat(trackIndex);
                    isAdaptiveList[rendererCount] = isAdaptive;
                    switch (rendererCount) {
                        case TRACK_VIDEO:
                            videoTrackData = new VideoTrackData(format.bitrate, format.width, format.height, format.id);
                            videoTrackDataList.add(videoTrackData);
                            log.e(TAG, " VIDEO ====> bitrate " + format.bitrate + " width " + format.width + " height " + format.height + " id " + format.id);
                            log.e(TAG, "format at index " + trackIndex + " bitrate " + format.bitrate);
                            log.e(TAG, "fotmat at index " + trackIndex + " sampleMimeType " + format.sampleMimeType);
                            log.e(TAG, "format at index " + trackIndex + " codecs " + format.codecs);
                            log.e(TAG, "format at index " + trackIndex + " language " + format.language);
                            log.e(TAG, "format at index " + trackIndex + " frameRate " + format.frameRate);
                            log.e(TAG, "format at index " + trackIndex + " channelCount " + format.channelCount);
                            log.e(TAG, "format at index " + trackIndex + " pixelWidthHeightRatio " + format.pixelWidthHeightRatio);
                            log.e(TAG, "format at index " + trackIndex + " width " + format.width);
                            log.e(TAG, "format at index " + trackIndex + " height " + format.height);
                            log.e(TAG, "format at index " + trackIndex + " channel count " + format.channelCount);
                            log.e(TAG, "format at index " + trackIndex + " containerMimeType " + format.containerMimeType);
                            log.e(TAG, "format at index " + trackIndex + " id " + format.id);
                            log.e(TAG, "format at index " + trackIndex + " sample rate " + format.sampleRate);
                            log.e(TAG, "is adaptive " + isAdaptive);
                            log.e(TAG, " ------------------------------------------------------------------------------------------------------------------------");
                            break;
                        case TRACK_AUDIO:
                            audioTrackData = new AudioTrackData(format.language, format.id);
                            audioTrackDataList.add(audioTrackData);
                            log.e(TAG, " AUDIO ====> language " + format.language + " id " + format.id);
                            log.e(TAG, "format at index " + trackIndex + " bitrate " + format.bitrate);
                            log.e(TAG, "fotmat at index " + trackIndex + " sampleMimeType " + format.sampleMimeType);
                            log.e(TAG, "format at index " + trackIndex + " codecs " + format.codecs);
                            log.e(TAG, "format at index " + trackIndex + " language " + format.language);
                            log.e(TAG, "format at index " + trackIndex + " frameRate " + format.frameRate);
                            log.e(TAG, "format at index " + trackIndex + " channelCount " + format.channelCount);
                            log.e(TAG, "format at index " + trackIndex + " pixelWidthHeightRatio " + format.pixelWidthHeightRatio);
                            log.e(TAG, "format at index " + trackIndex + " width " + format.width);
                            log.e(TAG, "format at index " + trackIndex + " height " + format.height);
                            log.e(TAG, "format at index " + trackIndex + " channel count " + format.channelCount);
                            log.e(TAG, "format at index " + trackIndex + " containerMimeType " + format.containerMimeType);
                            log.e(TAG, "format at index " + trackIndex + " id " + format.id);
                            log.e(TAG, "format at index " + trackIndex + " sample rate " + format.sampleRate);
                            log.e(TAG, "is adaptive " + isAdaptive);
                            log.e(TAG, " ------------------------------------------------------------------------------------------------------------------------");

                            break;
                        case TRACK_SUBTITLE:
                            subtitleTrackData = new SubtitleTrackData(format.language, format.id);
                            subtitleTrackDataList.add(subtitleTrackData);
                            log.e(TAG, " SUBTITLE ====> language " + format.language + " id " + format.id);
                            log.e(TAG, "format at index " + trackIndex + " bitrate " + format.bitrate);
                            log.e(TAG, "fotmat at index " + trackIndex + " sampleMimeType " + format.sampleMimeType);
                            log.e(TAG, "format at index " + trackIndex + " codecs " + format.codecs);
                            log.e(TAG, "format at index " + trackIndex + " language " + format.language);
                            log.e(TAG, "format at index " + trackIndex + " frameRate " + format.frameRate);
                            log.e(TAG, "format at index " + trackIndex + " channelCount " + format.channelCount);
                            log.e(TAG, "format at index " + trackIndex + " pixelWidthHeightRatio " + format.pixelWidthHeightRatio);
                            log.e(TAG, "format at index " + trackIndex + " width " + format.width);
                            log.e(TAG, "format at index " + trackIndex + " height " + format.height);
                            log.e(TAG, "format at index " + trackIndex + " channel count " + format.channelCount);
                            log.e(TAG, "format at index " + trackIndex + " containerMimeType " + format.containerMimeType);
                            log.e(TAG, "format at index " + trackIndex + " id " + format.id);
                            log.e(TAG, "format at index " + trackIndex + " sample rate " + format.sampleRate);
                            log.e(TAG, "is adaptive " + isAdaptive);
                            log.e(TAG, " ------------------------------------------------------------------------------------------------------------------------");
                            break;
                    }
                }
            }
        }

        trackData = new TrackData(videoTrackDataList, audioTrackDataList, subtitleTrackDataList);

    }


}
