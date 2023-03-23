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

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kaltura.android.exoplayer2.C;
import com.kaltura.android.exoplayer2.Format;
import com.kaltura.android.exoplayer2.RendererCapabilities;
import com.kaltura.android.exoplayer2.Tracks;
import com.kaltura.android.exoplayer2.dashmanifestparser.CustomAdaptationSet;
import com.kaltura.android.exoplayer2.dashmanifestparser.CustomDashManifest;
import com.kaltura.android.exoplayer2.dashmanifestparser.CustomFormat;
import com.kaltura.android.exoplayer2.dashmanifestparser.CustomRepresentation;
import com.kaltura.android.exoplayer2.source.TrackGroup;
import com.kaltura.android.exoplayer2.source.TrackGroupArray;
import com.kaltura.android.exoplayer2.source.dash.manifest.EventStream;
import com.kaltura.android.exoplayer2.text.Subtitle;
import com.kaltura.android.exoplayer2.text.webvtt.WebvttCueInfo;
import com.kaltura.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.kaltura.android.exoplayer2.trackselection.DefaultTrackSelector.SelectionOverride;
import com.kaltura.android.exoplayer2.trackselection.MappingTrackSelector;
import com.kaltura.android.exoplayer2.trackselection.TrackSelectionOverride;
import com.kaltura.android.exoplayer2.trackselection.TrackSelectionParameters;
import com.kaltura.android.exoplayer2.util.Util;
import com.kaltura.playkit.PKAbrFilter;
import com.kaltura.playkit.PKAudioCodec;
import com.kaltura.playkit.PKError;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKSubtitlePreference;
import com.kaltura.playkit.PKTrackConfig;
import com.kaltura.playkit.PKVideoCodec;
import com.kaltura.playkit.player.thumbnail.PKWebvttSubtitle;
import com.kaltura.playkit.player.thumbnail.ThumbnailInfo;
import com.kaltura.playkit.player.thumbnail.VttThumbnailDownloader;
import com.kaltura.playkit.utils.Consts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.kaltura.android.exoplayer2.util.MimeTypes.AUDIO_AAC;
import static com.kaltura.android.exoplayer2.util.MimeTypes.VIDEO_AV1;
import static com.kaltura.android.exoplayer2.util.MimeTypes.VIDEO_H265;
import static com.kaltura.android.exoplayer2.util.MimeTypes.VIDEO_VP8;
import static com.kaltura.android.exoplayer2.util.MimeTypes.VIDEO_VP9;
import static com.kaltura.playkit.utils.Consts.TRACK_TYPE_AUDIO;
import static com.kaltura.playkit.utils.Consts.TRACK_TYPE_IMAGE;
import static com.kaltura.playkit.utils.Consts.TRACK_TYPE_TEXT;
import static com.kaltura.playkit.utils.Consts.TRACK_TYPE_UNKNOWN;
import static com.kaltura.playkit.utils.Consts.TRACK_TYPE_VIDEO;

/**
 * Responsible for generating/sorting/holding and changing track info.
 * Created by anton.afanasiev on 22/11/2016.
 */

public class TrackSelectionHelper {

    private static final PKLog log = PKLog.get("TrackSelectionHelper");

    private static final int TRACK_ADAPTIVE = -1;
    private static final int TRACK_DISABLED = -2;

    private static final int RENDERER_INDEX = 0;
    private static final int GROUP_INDEX = 1;
    private static final int TRACK_INDEX = 2;
    private static final int TRACK_RENDERERS_AMOUNT = 3;

    static final String NONE = "none";
    static final String DISABLED = "disabled";
    private static final String ADAPTIVE = "adaptive";

    private static final String VIDEO_PREFIX = "Video:";
    private static final String AUDIO_PREFIX = "Audio:";
    private static final String TEXT_PREFIX = "Text:";
    private static final String IMAGE_PREFIX = "Image:";

    private static final String CEA_608 = "application/cea-608";
    private static final String LANGUAGE_UNKNOWN = "Unknown";

    private final Context context;
    private final DefaultTrackSelector selector;
    private Tracks tracksInfo;
    private MappingTrackSelector.MappedTrackInfo mappedTrackInfo;
    private TrackSelectionParameters trackSelectionParameters;
    private TrackSelectionParameters.Builder trackSelectionParametersBuilder;

    private List<VideoTrack> videoTracks = new ArrayList<>();
    private List<VideoTrack> originalVideoTracks;
    private final List<AudioTrack> audioTracks = new ArrayList<>();
    private final List<TextTrack> textTracks = new ArrayList<>();
    private final List<ImageTrack> imageTracks = new ArrayList<>();

    private @Nullable Format currentVideoFormat;
    private @Nullable Format currentAudioFormat;

    private Map<Pair<Long,Long>, ThumbnailInfo> externalVttThumbnailRangesInfo;
    private final Map<String, Map<String, List<Format>>> subtitleListMap = new HashMap<>();
    private final Map<PKVideoCodec,List<VideoTrack>> videoTracksCodecsMap = new HashMap<>();
    private final Map<PKAudioCodec,List<AudioTrack>> audioTracksCodecsMap = new HashMap<>();

    private String[] lastSelectedTrackIds;

    private String[] lastDisabledTrackIds;
    private String[] requestedChangeTrackIds;

    // To know if application passed the external subtitles
    private boolean hasExternalSubtitles = false;

    // To know if tracks has external subtitles or not.
    // Helpful in case if subtitles are removed by ExoPlayer in case of any discrepancy
    private boolean hasExternalSubtitlesInTracks = false;

    private TracksInfoListener tracksInfoListener;
    private TracksErrorListener tracksErrorListener;

    private PlayerSettings playerSettings;
    private String mediaItemManifestURL;
    private String redirectPlaybackURL;

    interface TracksInfoListener {

        void onTracksInfoReady(PKTracks PKTracks);

        void onRelease(String[] selectedTracks, String[] disbledTrackIds);

        void onVideoTrackChanged();

        void onAudioTrackChanged();

        void onTextTrackChanged();

        void onImageTrackChanged();

        void onEventStreamsChanged(List<EventStream> eventStreamList);
    }

    interface TracksErrorListener {
        void onTracksOverrideABRError(PKError pkError);
        void onUnsupportedVideoTracksError(PKError pkError);
        void onUnsupportedAudioTracksError(PKError pkError);
        void onUnsupportedAudioVideoTracksError(PKError pkError);
        void onUnsupportedTracksAvailableError(PKError pkError);
    }

    enum TrackType {
        UNKNOWN, VIDEO, AUDIO, TEXT
    }

    /**
     * @param selector             The track selector.
     * @param lastSelectedTrackIds - last selected track id`s.
     */
    public TrackSelectionHelper(Context context, DefaultTrackSelector selector,
                                String[] lastSelectedTrackIds, String[] lastDisabledTrackIds) {
        this.context = context;
        this.selector = selector;
        this.lastSelectedTrackIds = lastSelectedTrackIds;
        this.lastDisabledTrackIds = lastDisabledTrackIds;
        this.requestedChangeTrackIds = Arrays.copyOf(lastSelectedTrackIds, lastSelectedTrackIds.length);
        trackSelectionParameters = TrackSelectionParameters.getDefaults(context);
        trackSelectionParametersBuilder = trackSelectionParameters.buildUpon();
    }

    public void setMappedTrackInfo(MappingTrackSelector.MappedTrackInfo mappedTrackInfo) {
        this.mappedTrackInfo = mappedTrackInfo;
        if (playerSettings == null) {
            this.playerSettings = new PlayerSettings();
        }
    }

    public void setRedirectedManifestURL(String mediaItemManifestURL, String redirectPlaybackURL) {
        this.mediaItemManifestURL = mediaItemManifestURL;
        this.redirectPlaybackURL = redirectPlaybackURL;
    }

    public void clearPreviousMediaOverrides() {
        if (trackSelectionParametersBuilder != null && selector != null) {
            // We are specially doing it for Text Track because we disable the tracks only for Text tracks.
            trackSelectionParametersBuilder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false);
            // Clearing all the overrides
            trackSelectionParametersBuilder.clearOverrides();
            // Updating the default track selector of exoplayer
            selector.setParameters(trackSelectionParametersBuilder.build());
        }
    }

    /**
     * Prepare {@link PKTracks} object for application.
     * When the object is created, notify {@link ExoPlayerWrapper} about that,
     * and pass the {@link PKTracks} as parameter.
     * @param trackSelections the selected tracks.
     *
     * @return - true if tracks data created successful, if mappingTrackInfo not ready return false.
     */
    boolean prepareTracks(Tracks trackSelections, String externalThumbnailWebVttUrl, CustomDashManifest customDashManifest) {
        if (assertTrackSelectorIsNull("prepareTracks")) {
            return false;
        }

        tracksInfo = trackSelections;
        mappedTrackInfo = selector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo == null) {
            log.w("Trying to get current MappedTrackInfo returns null");
            return false;
        }

        if (checkTracksUnavailability(mappedTrackInfo)) {
            String errorMessage = "No audio, video and text track found";
            PKError currentError = new PKError(PKPlayerErrorType.UNEXPECTED, PKError.Severity.Fatal, errorMessage, new IllegalStateException(errorMessage));
            tracksErrorListener.onUnsupportedTracksAvailableError(currentError);
            return false;
        }

        warnAboutUnsupportedRendererTypes();

        List<CustomFormat> rawImageTracks = new ArrayList<>();
        List<EventStream> eventStreamList = new ArrayList<>();

        if (customDashManifest != null && customDashManifest.getPeriodCount() > 0) {
            for (int periodIndex = 0; periodIndex < customDashManifest.getPeriodCount(); periodIndex++) {

                eventStreamList.addAll(customDashManifest.getPeriod(periodIndex).eventStreams);
                List<CustomAdaptationSet> adaptationSets = customDashManifest.getPeriod(periodIndex).adaptationSets;

                for (int adaptationSetIndex = 0 ; adaptationSetIndex < adaptationSets.size() ; adaptationSetIndex++) {
                    if (adaptationSets.get(adaptationSetIndex).type != C.TRACK_TYPE_IMAGE) {
                        continue;
                    }
                    List<CustomRepresentation> representations = adaptationSets.get(adaptationSetIndex).representations;
                    for (CustomRepresentation representation : representations) {
                        if (representation.format == null || representation.format.formatThumbnailInfo == null) {
                            continue;
                        }

                        rawImageTracks.add(representation.format);
                    }
                }
            }
        }

        PKTracks tracksInfo = buildTracks(externalThumbnailWebVttUrl, rawImageTracks);

        if (tracksInfoListener != null) {
            tracksInfoListener.onTracksInfoReady(tracksInfo);
            if (!tracksInfo.getImageTracks().isEmpty()) {
                tracksInfoListener.onImageTrackChanged();
            }

            if (!eventStreamList.isEmpty()) {
                tracksInfoListener.onEventStreamsChanged(eventStreamList);
            }
        }

        return true;
    }

    /**
     * Actually build {@link PKTracks} object, based on the loaded manifest into Exoplayer.
     * This method knows how to filter unsupported/unknown formats, and create adaptive option when this is possible.
     */
    public PKTracks buildTracks(String externalThumbnailWebVttUrl, List<CustomFormat> rawImageTracks) {

        clearTracksLists();

        TrackGroupArray trackGroupArray;
        TrackGroup trackGroup;
        Format format;
        boolean videoTracksAvailable = false;

        //run through the all renders.
        for (int rendererIndex = 0; rendererIndex < TRACK_RENDERERS_AMOUNT; rendererIndex++) {
            TrackType trackType = getTrackType(rendererIndex);
            //the trackGroupArray of the current renderer.
            trackGroupArray = mappedTrackInfo.getTrackGroups(rendererIndex);

            if (rendererIndex == TRACK_TYPE_TEXT && hasExternalSubtitles) {
                extractTextTracksToMap(trackGroupArray);
            }

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

                                if (format.roleFlags == C.ROLE_FLAG_TRICK_PLAY) {
                                    // ROLE_FLAG_TRICK_PLAY is not a video track
                                    // in future handle that in hls case for thumbnailInfo
                                    continue;
                                }

                                if (!videoTracksAvailable) {
                                    videoTracksAvailable = true;
                                }

                                PKVideoCodec currentVideoTrackCodec = getVideoCodec(format);
                                VideoTrack currentVideoTrack = new VideoTrack(uniqueId, format.bitrate, format.width, format.height, format.selectionFlags, false, currentVideoTrackCodec, format.codecs);

                                if (!videoTracksCodecsMap.containsKey(currentVideoTrackCodec)) {
                                    videoTracksCodecsMap.put(currentVideoTrackCodec, new ArrayList<>());
                                }
                                if (videoTracksCodecsMap.get(currentVideoTrackCodec) != null) {
                                    videoTracksCodecsMap.get(currentVideoTrackCodec).add(currentVideoTrack);
                                }
                                break;
                            case TRACK_TYPE_AUDIO:
                                PKAudioCodec currentAudioTrackCodec = null;
                                AudioTrack currentAudioTrack = null;

                                if (format.language == null && format.id != null && format.id.matches("\\d+/\\d+")) {
                                    if (playerSettings != null && playerSettings.mpgaAudioFormatEnabled()) {
                                        currentAudioTrackCodec = PKAudioCodec.AAC;
                                        currentAudioTrack = new AudioTrack(uniqueId, format.id, format.label, format.bitrate, format.channelCount, format.selectionFlags, false, currentAudioTrackCodec, AUDIO_AAC);
                                        audioTracks.add(currentAudioTrack);
                                    }
                                } else {
                                    currentAudioTrackCodec = getAudioCodec(format);
                                    currentAudioTrack = new AudioTrack(uniqueId, getLanguageFromFormat(format), format.label, format.bitrate, format.channelCount, format.selectionFlags, false, currentAudioTrackCodec, format.codecs);
                                    audioTracks.add(currentAudioTrack);
                                }

                                if (currentAudioTrack != null && currentAudioTrackCodec != null) {
                                    if (!audioTracksCodecsMap.containsKey(currentAudioTrackCodec)) {
                                        audioTracksCodecsMap.put(currentAudioTrackCodec, new ArrayList<>());
                                    }
                                    if (audioTracksCodecsMap.get(currentAudioTrackCodec) != null) {
                                        audioTracksCodecsMap.get(currentAudioTrackCodec).add(currentAudioTrack);
                                    }
                                }
                                break;
                            case TRACK_TYPE_TEXT:
                                if (format.language != null && hasExternalSubtitles) {
                                    int selectionFlag = format.selectionFlags;
                                    if (selector != null && (selectionFlag == Consts.DEFAULT_TRACK_SELECTION_FLAG_HLS || selectionFlag == Consts.DEFAULT_TRACK_SELECTION_FLAG_DASH)) {
                                        DefaultTrackSelector.Parameters.Builder parametersBuilder = selector.getParameters().buildUpon();

                                        if (discardTextTrackOnPreference(format)) {
                                            trackSelectionParametersBuilder.clearOverride(trackGroup);
                                            parametersBuilder.setRendererDisabled(TRACK_TYPE_TEXT, true);
                                            continue;
                                        } else {
                                            TrackSelectionOverride trackSelectionOverride = new TrackSelectionOverride(trackGroup, Collections.singletonList(trackIndex));
                                            parametersBuilder.addOverride(trackSelectionOverride);
                                        }

                                        selector.setParameters(parametersBuilder);
                                    }
                                }

                                if (CEA_608.equals(format.sampleMimeType)) {
                                    if (playerSettings != null && playerSettings.cea608CaptionsEnabled()) {
                                        textTracks.add(new TextTrack(uniqueId, format.language, format.id, format.sampleMimeType, format.selectionFlags));
                                    }
                                } else {
                                    textTracks.add(new TextTrack(uniqueId, getLanguageFromFormat(format), format.label, format.sampleMimeType, format.selectionFlags));
                                }
                                break;
                        }
                    } else {
                        log.w("format is not supported for this device. Format bitrate " + format.bitrate + " id " + format.id);
                    }
                }
            }
        }

        if (!TextUtils.isEmpty(externalThumbnailWebVttUrl)) {
            createVttThumbnailImageTrack(externalThumbnailWebVttUrl);
        } else if (rawImageTracks != null && !rawImageTracks.isEmpty()) {
            createDashThumbnailImageTracks(rawImageTracks);
        }

        //add disable option to the text tracks.
        maybeAddDisabledTextTrack();

        // We intend to sort the video and audio tracks after filtration so that
        // while taking the default track index. Indexing of track will not get any impact.
        videoTracks = filterVideoTracks();
        Collections.sort(videoTracks);

        //Leave only adaptive audio tracks for user selection if no extra config is set.
        ArrayList<AudioTrack> filteredAudioTracks = filterAdaptiveAudioTracks();
        Collections.sort(filteredAudioTracks);

        int defaultVideoTrackIndex = getDefaultTrackIndex(videoTracks, lastSelectedTrackIds[TRACK_TYPE_VIDEO]);
        int defaultAudioTrackIndex = getDefaultTrackIndex(filteredAudioTracks, lastSelectedTrackIds[TRACK_TYPE_AUDIO]);
        int defaultTextTrackIndex = getDefaultTrackIndex(textTracks, lastSelectedTrackIds[TRACK_TYPE_TEXT]);
        int defaultImageTrackIndex = getDefaultTrackIndex(imageTracks, lastSelectedTrackIds[TRACK_TYPE_IMAGE]);

        return new PKTracks(videoTracks, filteredAudioTracks, textTracks, imageTracks, defaultVideoTrackIndex, defaultAudioTrackIndex, defaultTextTrackIndex, defaultImageTrackIndex);
    }

    private void createDashThumbnailImageTracks(List<CustomFormat> rawImageTracks) {
        for (int trackIndex = 0; trackIndex < rawImageTracks.size(); trackIndex++) {
            CustomFormat imageFormat = rawImageTracks.get(trackIndex);
            CustomFormat.FormatThumbnailInfo formatThumbnailInfo = imageFormat.formatThumbnailInfo;
            if (formatThumbnailInfo == null) {
                continue;
            }

            String uniqueId = getUniqueId(TRACK_TYPE_IMAGE, TRACK_TYPE_IMAGE, trackIndex);
            String fixedImageTrackURL = formatThumbnailInfo.imageTemplateUrl;
            if (!TextUtils.isEmpty(mediaItemManifestURL) &&
                    !TextUtils.isEmpty(redirectPlaybackURL) &&
                    !TextUtils.equals(mediaItemManifestURL, redirectPlaybackURL)) {
                fixedImageTrackURL = rebuildImageTrackURL(formatThumbnailInfo.imageTemplateUrl);
            }

            imageTracks.add(trackIndex, new DashImageTrack(uniqueId,
                    imageFormat.id,
                    imageFormat.bitrate,
                    imageFormat.width,
                    imageFormat.height,
                    formatThumbnailInfo.tilesHorizontal,
                    formatThumbnailInfo.tilesVertical,
                    formatThumbnailInfo.segmentDuration * Consts.MILLISECONDS_MULTIPLIER,
                    fixedImageTrackURL,
                    formatThumbnailInfo.presentationTimeOffset,
                    formatThumbnailInfo.timeScale,
                    formatThumbnailInfo.startNumber,
                    formatThumbnailInfo.endNumber
            ));
        }

        if (!imageTracks.isEmpty()) {
            updateLastSelectedImageTrackIds();
        }
    }

    @NonNull
    private String rebuildImageTrackURL(String fullImageUrl) {
        if (mediaItemManifestURL.contains("?")) {
            mediaItemManifestURL = mediaItemManifestURL.split("\\?")[0];
        }
        int index = mediaItemManifestURL.lastIndexOf('/');
        if (index > 0) {
            mediaItemManifestURL = mediaItemManifestURL.substring(0, index);
        }

        if (redirectPlaybackURL.contains("?")) {
            redirectPlaybackURL = redirectPlaybackURL.split("\\?")[0];
        }
        index = redirectPlaybackURL.lastIndexOf('/');
        if (index > 0) {
            redirectPlaybackURL = redirectPlaybackURL.substring(0, index);
        }
        String fullImageUrlSuffix = fullImageUrl.replaceAll(mediaItemManifestURL, "");
        fullImageUrl = redirectPlaybackURL + fullImageUrlSuffix;
        return fullImageUrl;
    }

    private void createVttThumbnailImageTrack(String externalThumbnailWebVttUrl) {
        externalVttThumbnailRangesInfo = new LinkedHashMap<>();
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            Future<Subtitle> webVttThumbnails = executorService.submit(new VttThumbnailDownloader(externalThumbnailWebVttUrl));
            if (webVttThumbnails != null) {
                PKWebvttSubtitle vttSubtitle = (PKWebvttSubtitle) webVttThumbnails.get();
                if (vttSubtitle != null) {
                    List<WebvttCueInfo> webvttCueInfos = vttSubtitle.getCueInfos();
                    if (webvttCueInfos != null && !webvttCueInfos.isEmpty()) {

                        long firstThumbStartTime = webvttCueInfos.get(0).startTimeUs / Consts.MILLISECONDS_MULTIPLIER;
                        long firstThumbEndTime = webvttCueInfos.get(0).endTimeUs / Consts.MILLISECONDS_MULTIPLIER;

                        Uri uri = Uri.parse(externalThumbnailWebVttUrl);
                        String baseUrl = uri.toString().replace(uri.getLastPathSegment(), "");

                        for (int i = 0; i < webvttCueInfos.size(); i++) {
                            Pair<Pair<Long, Long>, ThumbnailInfo> thumbnailInfoPair = getExternalVttThumbnailInfo(baseUrl, webvttCueInfos.get(i));
                            if (thumbnailInfoPair == null) {
                                continue;
                            }

                            externalVttThumbnailRangesInfo.put(new Pair<>(thumbnailInfoPair.first.first, thumbnailInfoPair.first.second), thumbnailInfoPair.second);
                        }

                        long imageDuration = firstThumbEndTime - firstThumbStartTime;
                        List<Pair<Long, Long>> externalVttThumbnailRangesInfoKeySetList = new ArrayList<>(externalVttThumbnailRangesInfo.keySet());
                        if (!externalVttThumbnailRangesInfoKeySetList.isEmpty()) {
                            ThumbnailInfo firstThumbnailInfo = externalVttThumbnailRangesInfo.get(externalVttThumbnailRangesInfoKeySetList.get(0));
                            if (firstThumbnailInfo != null) {

                                int cols = getThumbnailColsCount(webvttCueInfos, baseUrl);
                                cols = (cols > 0) ? cols : 1;

                                int rows = getThumbnailRowsCount(webvttCueInfos, baseUrl);
                                rows = (rows > 0) ? rows : 1;

                                float width = (firstThumbnailInfo.getWidth() * (cols - 1)) <= 0 ? -1 : firstThumbnailInfo.getWidth() * (cols - 1);
                                if (cols == 1) {
                                    width = firstThumbnailInfo.getWidth();
                                }

                                float height = (firstThumbnailInfo.getHeight() * (rows - 1) <= 0) ? -1 : firstThumbnailInfo.getHeight() * (rows - 1);
                                if (rows == 1) {
                                    height = firstThumbnailInfo.getHeight();
                                }

                                imageDuration = (imageDuration * cols * rows);

                                String uniqueId = getUniqueId(TRACK_TYPE_IMAGE, TRACK_TYPE_IMAGE, 0);
                                imageTracks.add(0, new ImageTrack(uniqueId, "externalVttThumbnail", -1, width, height, cols, rows, imageDuration, baseUrl));

                                updateLastSelectedImageTrackIds();
                            }
                        }
                    }
                }
            }
        } catch(Exception exception){
            log.e("error " + exception.getMessage());
        } finally{
            executorService.shutdown();
        }
    }

    private int getThumbnailColsCount(List<WebvttCueInfo> webvttCueInfos, String baseUrl) {
        int cols = 0;
        for (WebvttCueInfo webvttCueInfo : webvttCueInfos) {
            Pair<Pair<Long, Long>, ThumbnailInfo> thumbnailInfoPair = getExternalVttThumbnailInfo(baseUrl, webvttCueInfo);

            if (thumbnailInfoPair == null || thumbnailInfoPair.second == null) {
                continue;
            }

            final ThumbnailInfo thumbnailInfoSecond = thumbnailInfoPair.second;

            if (thumbnailInfoSecond.getHeight() == -1 || thumbnailInfoSecond.getWidth() == -1) {
                break;
            }

            if (thumbnailInfoSecond.getX() == 0 && thumbnailInfoSecond.getY() == 0) {
                cols++;
            } else if (thumbnailInfoSecond.getX() > 0) {
                cols++;
            } else if (thumbnailInfoSecond.getX() == 0) {
                break;
            }
        }
        return cols;
    }

    private int getThumbnailRowsCount(List<WebvttCueInfo> webvttCueInfos, String baseUrl) {
        int rows = 0;
        boolean rowsFirst = true;
        for (WebvttCueInfo webvttCueInfo : webvttCueInfos) {
            Pair<Pair<Long, Long>, ThumbnailInfo> thumbnailInfoPair = getExternalVttThumbnailInfo(baseUrl, webvttCueInfo);

            if (thumbnailInfoPair == null || thumbnailInfoPair.second == null) {
                continue;
            }

            final ThumbnailInfo thumbnailInfoSecond = thumbnailInfoPair.second;
            if (thumbnailInfoSecond.getHeight() == -1 || thumbnailInfoSecond.getWidth() == -1) {
                break;
            }

            if (thumbnailInfoSecond.getX() == 0 && thumbnailInfoSecond.getY() == 0) {
                if (rowsFirst) {
                    rows++;
                    rowsFirst = false;
                } else {
                    break;
                }
            } else if (thumbnailInfoSecond.getX() == 0) {
                rows++;
            }
        }
        return rows;
    }

    private void updateLastSelectedImageTrackIds() {
        if (NONE.equals(requestedChangeTrackIds[TRACK_TYPE_IMAGE])) {
            log.d("Image track changed to: " + requestedChangeTrackIds[TRACK_TYPE_IMAGE]);
            lastSelectedTrackIds[TRACK_TYPE_IMAGE] = imageTracks.get(0).getUniqueId();
        }
    }

    private Pair<Pair<Long,Long>, ThumbnailInfo> getExternalVttThumbnailInfo(String baseUrl, WebvttCueInfo cueInfo) {
        if (cueInfo == null || TextUtils.isEmpty(cueInfo.cue.text)) {
            return null;
        }

        String cueText = String.valueOf(cueInfo.cue.text);
        String imageUrl = "";
        long x = -1;
        long y = -1;
        long w = -1;
        long h = -1;

        if (!TextUtils.isEmpty(cueText)) {
            String[] cueParts = cueText.split("#");
            if (cueParts.length > 0) {
                imageUrl = cueParts[0];
                if (cueParts.length == 2 && cueParts[1].startsWith("xywh") && cueParts[1].contains("=")) {
                    String[] xywh = cueParts[1].split("=")[1].split(",");
                    if (xywh.length == 4) {
                        x = Long.parseLong(xywh[0]);
                        y = Long.parseLong(xywh[1]);
                        w = Long.parseLong(xywh[2]);
                        h = Long.parseLong(xywh[3]);
                    }
                }
            }
        }
        long startTime = cueInfo.startTimeUs / Consts.MILLISECONDS_MULTIPLIER;
        long endTime = cueInfo.endTimeUs / Consts.MILLISECONDS_MULTIPLIER;

        if (imageUrl.startsWith("/")) {
            imageUrl = imageUrl.replaceFirst("/", "");
        }
        String[] imageUrlSlices = imageUrl.split("/");

        StringBuilder maybeRemoveString = new StringBuilder();
        if (imageUrlSlices.length > 1) {
            for (int i = 0 ; i < imageUrlSlices.length - 1 ; i++) {
                maybeRemoveString.append("/").append(imageUrlSlices[i]).append("/");
            }
        }
        if (!TextUtils.isEmpty(maybeRemoveString.toString()) && baseUrl.endsWith(maybeRemoveString.toString())) {
            baseUrl = baseUrl.replace(maybeRemoveString.toString(), "");
            baseUrl += "/";
        }
        ThumbnailInfo thumbnailInfo = new ThumbnailInfo(baseUrl + imageUrl, x, y, w, h);
        return new Pair<>(new Pair<>(startTime, endTime), thumbnailInfo);
    }

    private boolean checkTracksUnavailability(MappingTrackSelector.MappedTrackInfo mappedTrackInfo) {
        return mappedTrackInfo.getTrackGroups(TRACK_TYPE_VIDEO).length == 0 &&
                mappedTrackInfo.getTrackGroups(TRACK_TYPE_AUDIO).length == 0 &&
                mappedTrackInfo.getTrackGroups(TRACK_TYPE_TEXT).length == 0;
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

    /**
     * Get the language from format (Audio/Video)
     * Convert the language to ISO3 language if required
     *
     * @param format Track's format
     * @return String language
     */
    private String getLanguageFromFormat(Format format) {
        String language = format.language;
        if (TextUtils.isEmpty(format.language)) {
            return LANGUAGE_UNKNOWN;
        }

        if ((!C.LANGUAGE_UNDETERMINED.equals(language) &&
                !isExternalSubtitle(format.language, format.sampleMimeType) &&
                language.length() > 2) ||
                isIso3LanguageRequired(format)) {

            Locale locale = Util.SDK_INT >= 21 ? Locale.forLanguageTag(language) : new Locale(language);

            try {
                String iso3Language = locale.getISO3Language();
                if (isExternalSubtitle(format.language, format.sampleMimeType)) {
                    // Append the mimetype to iso3language again with '-'
                    String mimeType = getExternalSubtitleMimeType(format);
                    if (!TextUtils.isEmpty(mimeType)) {
                        iso3Language = iso3Language.concat(mimeType);
                    }
                }
                return iso3Language;
            } catch (MissingResourceException | NullPointerException ex) {
                log.e(ex.getMessage());
            }
        }

        return language;
    }

    /**
     * Check if the text track is external subtitle
     *
     * @param language track's langauge
     * @param sampleMimeType track's mimeType
     *
     * @return boolean
     */
    private boolean isExternalSubtitle(String language, String sampleMimeType) {
        return language != null && (language.contains("-" + sampleMimeType) || language.contains("-" + "Unknown"));
    }

    /**
     * Only used for External Subtitles
     * Get the external subtitle language
     *
     * @param format External Subtitle's format
     * @return String language
     */
    @Nullable
    private String getExternalSubtitleLanguage(Format format) {
        if (format.language != null) {
            return format.language.substring(0, format.language.indexOf("-"));
        }
        return null;
    }

    /**
     * Only used for External Subtitles
     * If language length is greater than 2 then ISO3 code is required
     *
     * @return boolean if required
     */
    private boolean isIso3LanguageRequired(Format format) {
        if (isExternalSubtitle(format.language, format.sampleMimeType)) {
            String language = getExternalSubtitleLanguage(format);
            return !C.LANGUAGE_UNDETERMINED.equals(language) && !TextUtils.isEmpty(language) && language.length() > 2;
        }
        return false;
    }

    /**
     * Only used for External Subtitles
     *
     * We can not rely on Format's sample mimeType because
     * for external subtitles we can concatenating 'unknown' as well
     * in case if the mimeType is empty
     *
     * It includes '-' with the mimeType
     *
     * @return String subtitle mimeType
     */
    @Nullable
    private String getExternalSubtitleMimeType(Format format) {
        if (format.language != null) {
            return format.language.substring(format.language.indexOf("-"));
        }
        return null;
    }

    private boolean discardTextTrackOnPreference(Format format) {
        PKSubtitlePreference subtitlePreference = playerSettings.getSubtitlePreference();
        if (subtitlePreference == PKSubtitlePreference.OFF) {
            return false;
        }

        String languageName = format.language;
        boolean isExternalSubtitle = isExternalSubtitle(format.language, format.sampleMimeType);

        if (isExternalSubtitle) {
            languageName = getExternalSubtitleLanguage(format);
            Map<String, List<Format>> languageNameMap = subtitleListMap.get(languageName);
            if (subtitlePreference == PKSubtitlePreference.INTERNAL && languageNameMap != null && !languageNameMap.containsKey(languageName)) {
                // If there is no internal subtitle and the preference is PKSubtitlePreference.Internal from App
                // then we are not discarding this text track.
                return false;
            }
        }

        if (subtitleListMap.containsKey(languageName) && subtitleListMap.get(languageName).size() > 1) {

            if ((subtitlePreference == PKSubtitlePreference.INTERNAL && isExternalSubtitle) ||
                    (subtitlePreference == PKSubtitlePreference.EXTERNAL && !isExternalSubtitle)) {
                return true;
            } else if ((subtitlePreference == PKSubtitlePreference.EXTERNAL && isExternalSubtitle) ||
                    (subtitlePreference == PKSubtitlePreference.INTERNAL && !isExternalSubtitle)) {
                return false;
            }
        }
        return false;
    }

    private List<VideoTrack> filterVideoTracks() {

        if (videoTracksCodecsMap == null || videoTracksCodecsMap.isEmpty()) {
            return videoTracks;
        }

        boolean atLeastOneCodecSupportedInHardware = videoCodecsSupportedInHardware(); // if no hardware decoders play with the software

        VideoCodecSettings preferredVideoCodecSettings = playerSettings.getPreferredVideoCodecSettings();
        if (preferredVideoCodecSettings.getAllowMixedCodecAdaptiveness()) {
            populateAllCodecTracks(atLeastOneCodecSupportedInHardware);
            return videoTracks;
        }

        for (PKVideoCodec videoCodecForPlayback : preferredVideoCodecSettings.getCodecPriorityList()) {
            if (!videoTracksCodecsMap.containsKey(videoCodecForPlayback)) {
                continue;
            }

            if (videoTracksCodecsMap.get(videoCodecForPlayback) != null && videoTracksCodecsMap.get(videoCodecForPlayback).isEmpty()) {
                continue;
            }

            VideoTrack candidateVideoTrack = videoTracksCodecsMap.get(videoCodecForPlayback).get(0);
            if (candidateVideoTrack.getCodecName() != null &&
                    isCodecSupported(candidateVideoTrack.getCodecName(), TrackType.VIDEO, false)) {
                return videoTracksCodecsMap.get(videoCodecForPlayback);
            } else if ((!atLeastOneCodecSupportedInHardware  || preferredVideoCodecSettings.isAllowSoftwareDecoder()) &&
                    (candidateVideoTrack.getCodecName() != null && isCodecSupported(candidateVideoTrack.getCodecName(), TrackType.VIDEO, true))) {
                return videoTracksCodecsMap.get(videoCodecForPlayback);
            }
        }

        List<PKVideoCodec> videoCodecList = new ArrayList<>(Arrays.asList(PKVideoCodec.HEVC, PKVideoCodec.AV1, PKVideoCodec.VP9, PKVideoCodec.VP8, PKVideoCodec.AVC));

        for (PKVideoCodec pkVideoCodec : videoCodecList) {
            if (videoTracksCodecsMap.containsKey(pkVideoCodec)) {
                return videoTracksCodecsMap.get(pkVideoCodec);
            }
        }
        return videoTracks;
    }

    private boolean videoCodecsSupportedInHardware() {
        for (PKVideoCodec videoCodecForPlayback : playerSettings.getPreferredVideoCodecSettings().getCodecPriorityList()) {
            if (!videoTracksCodecsMap.containsKey(videoCodecForPlayback)) {
                continue;
            }

            if (videoTracksCodecsMap.get(videoCodecForPlayback) != null && videoTracksCodecsMap.get(videoCodecForPlayback).isEmpty()) {
                continue;
            }

            VideoTrack candidateVideoTrack = videoTracksCodecsMap.get(videoCodecForPlayback).get(0);
            if (candidateVideoTrack.getCodecName() != null &&
                    isCodecSupported(candidateVideoTrack.getCodecName(), TrackType.VIDEO, false)) {
                return true;
            }
        }
        return false;
    }

    private void populateAllCodecTracks(boolean atleastOneCodecSupportedInHardware) {
        for (Map.Entry<PKVideoCodec, List<VideoTrack>> multipleCodecEntry  : videoTracksCodecsMap.entrySet()) {
            for (VideoTrack codecVideoTrack : multipleCodecEntry.getValue()) {
                if (codecVideoTrack.getCodecName() != null &&
                        isCodecSupported(codecVideoTrack.getCodecName(), TrackType.VIDEO, false)) {
                    videoTracks.add(codecVideoTrack);
                } else if ((!atleastOneCodecSupportedInHardware || playerSettings.getPreferredVideoCodecSettings().isAllowSoftwareDecoder()) &&
                        (codecVideoTrack.getCodecName() != null && isCodecSupported(codecVideoTrack.getCodecName(), TrackType.VIDEO, true))) {
                    videoTracks.add(codecVideoTrack);
                } else if (codecVideoTrack.getCodecName() == null && codecVideoTrack.getCodecType() == PKVideoCodec.AVC && codecVideoTrack.getBitrate() >= 0) {
                    videoTracks.add(codecVideoTrack);
                }
            }
        }
    }

    /**
     * Filter audioTracks, so that if it contain any sort of adaptive streams
     * PKTracks will expose only adaptive option
     * and hide all the bitrate variants.
     * if app prefers to allow audio track selection by codec type it will return all available tracks by codec by audio groups
     */
    private ArrayList<AudioTrack> filterAdaptiveAudioTracks() {
        ArrayList<AudioTrack> filteredAudioTracks = new ArrayList<>();

        int[] parsedUniqueId;
        int currentGroup = -1;


        boolean allowMixedCodecs = playerSettings.getPreferredAudioCodecSettings().getAllowMixedCodecs();
        HashMap<String, Boolean> currentCodecMap = new HashMap<>();
        for (AudioTrack audioTrack : audioTracks) {
            parsedUniqueId = parseUniqueId(audioTrack.getUniqueId());

            if (parsedUniqueId[TRACK_INDEX] == TRACK_ADAPTIVE || (allowMixedCodecs && !currentCodecMap.containsKey(audioTrack.getCodecName()))) {
                filteredAudioTracks.add(audioTrack);
                currentGroup = parsedUniqueId[GROUP_INDEX];
                if (allowMixedCodecs) {
                    currentCodecMap.put(audioTrack.getCodecName(), parsedUniqueId[TRACK_INDEX] == TRACK_ADAPTIVE);
                }
            } else if (parsedUniqueId[GROUP_INDEX] != currentGroup) {
                filteredAudioTracks.add(audioTrack);
                currentGroup = -1;
            } else {
                if (allowMixedCodecs) {
                    Boolean isAdaptive = currentCodecMap.get(audioTrack.getCodecName());
                    if (isAdaptive != null && !isAdaptive) {
                        // check if same codec track is not adaptive
                        filteredAudioTracks.add(audioTrack);
                        currentGroup = -1;
                    }
                }
            }
        }
        currentCodecMap.clear();

        AudioCodecSettings preferredAudioCodecSettings = playerSettings.getPreferredAudioCodecSettings();
        if (preferredAudioCodecSettings.getAllowMixedCodecs() && preferredAudioCodecSettings.getAllowMixedBitrates()) {
            return new ArrayList<>(audioTracks);
        }

        if (preferredAudioCodecSettings.getAllowMixedCodecs() && !preferredAudioCodecSettings.getAllowMixedBitrates()) {
            return filteredAudioTracks;
        }

        if (audioTracksCodecsMap.size() > 1) {
            for (PKAudioCodec pkAudioCodec : preferredAudioCodecSettings.getCodecPriorityList()) {
                if (audioTracksCodecsMap.containsKey(pkAudioCodec) && audioTracksCodecsMap.get(pkAudioCodec) != null) {
                    if (preferredAudioCodecSettings.getAllowMixedBitrates()) {
                        ArrayList<AudioTrack> preferredCodecMixedBitrates = new ArrayList<>();
                        for (AudioTrack audioTrack : audioTracks) {
                            if (audioTrack.getCodecType() == pkAudioCodec) {
                                preferredCodecMixedBitrates.add(audioTrack);
                            }
                        }
                        return new ArrayList<>(preferredCodecMixedBitrates);
                    } else {
                        for (AudioTrack filteredTrack : filteredAudioTracks) {
                            if (filteredTrack.getCodecType() == pkAudioCodec) {
                                return new ArrayList<>(Collections.singletonList(filteredTrack));
                            }
                        }
                    }
                }
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
        textTracks.add(0, new TextTrack(uniqueId, NONE, NONE, NONE, -1));
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

        if (trackList.get(0) instanceof ImageTrack) {
            return restoreLastSelectedTrack(trackList, lastSelectedTrackId, getUpdatedDefaultTrackIndex(trackList, defaultTrackIndex));
        }

        for (int i = 0; i < trackList.size(); i++) {
            if (trackList.get(i) != null) {
                int selectionFlag = trackList.get(i).getSelectionFlag();
                if (selectionFlag == Consts.DEFAULT_TRACK_SELECTION_FLAG_HLS || selectionFlag == Consts.DEFAULT_TRACK_SELECTION_FLAG_DASH) {
                    if (trackList.get(i) instanceof TextTrack && hasExternalSubtitlesInTracks && playerSettings.getSubtitlePreference() != PKSubtitlePreference.OFF) {
                        PKSubtitlePreference pkSubtitlePreference = playerSettings.getSubtitlePreference();
                        Format selectedTextFormat = getSelectedTextTrackFormat();

                        if (selectedTextFormat == null) {
                            continue;
                        }

                        // TrackSelection is giving the default tracks for video, audio and text.
                        // If trackSelection contains a text which is an external text track, it means that either internal text track
                        // does not contain any track or there is no default track in internal text track. If there is no internal text track then
                        // forcing the preference to be External.

                        String languageName = null;
                        if (selectedTextFormat.language != null) {
                            languageName = selectedTextFormat.language;

                            if (!TextUtils.isEmpty(languageName) &&
                                    selectedTextFormat.sampleMimeType != null &&
                                    isExternalSubtitle(languageName, selectedTextFormat.sampleMimeType)) {
                                pkSubtitlePreference = PKSubtitlePreference.EXTERNAL;
                            }
                        }

                        TextTrack textTrack = (TextTrack) trackList.get(i);
                        boolean isExternalSubtitle = isExternalSubtitle(textTrack.getLanguage(), textTrack.getMimeType());

                        if (subtitleListMap.containsKey(languageName) && subtitleListMap.get(languageName).size() == 1) {
                            defaultTrackIndex = i;
                            break;
                        } else if (isExternalSubtitle && pkSubtitlePreference == PKSubtitlePreference.EXTERNAL) {
                            defaultTrackIndex = i;
                            break;
                        } else if (!isExternalSubtitle && pkSubtitlePreference == PKSubtitlePreference.INTERNAL) {
                            defaultTrackIndex = i;
                            break;
                        }
                    } else {
                        defaultTrackIndex = i;
                        break;
                    }
                }
            }
        }

        return restoreLastSelectedTrack(trackList, lastSelectedTrackId, getUpdatedDefaultTrackIndex(trackList, defaultTrackIndex));
    }

    /**
     * Creates a map which contains text language map
     * which inside holds with list of available formats for that specific language
     *
     * @param trackGroupArray TrackGroupArray this includes Internal and External Text Formats
     */
    private void extractTextTracksToMap(TrackGroupArray trackGroupArray) {
        for (int groupIndex = 0; groupIndex < trackGroupArray.length; groupIndex++) {
            TrackGroup trackGroup = trackGroupArray.get(groupIndex);

            for (int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {
                Format format = trackGroup.getFormat(trackIndex);

                String languageName = format.language;
                if (TextUtils.isEmpty(languageName)) {
                    continue;
                }

                boolean isExternalSubtitle = isExternalSubtitle(languageName, format.sampleMimeType);
                if (isExternalSubtitle) {
                    languageName = getExternalSubtitleLanguage(format);
                    hasExternalSubtitlesInTracks = true;
                }

                if (subtitleListMap.containsKey(languageName)) {
                    Map<String, List<Format>> baseTrackHashMap = subtitleListMap.get(languageName);
                    if (baseTrackHashMap.containsKey(format.language)) {
                        List<Format> langaugeList = baseTrackHashMap.get(format.language);
                        langaugeList.add(format);
                    } else {
                        List<Format> langaugeList = new ArrayList<>();
                        langaugeList.add(format);
                        baseTrackHashMap.put(format.language, langaugeList);
                    }
                } else {
                    Map<String, List<Format>> baseTrackHashMap = new HashMap<>();
                    List<Format> langaugeList = new ArrayList<>();
                    langaugeList.add(format);
                    baseTrackHashMap.put(format.language, langaugeList);
                    subtitleListMap.put(languageName, baseTrackHashMap);
                }
            }
        }
    }

    private int getUpdatedDefaultTrackIndex(List<? extends BaseTrack> trackList, int defaultTrackIndex) {

        if (!trackList.isEmpty()) {
            int trackType = TRACK_TYPE_UNKNOWN;
            if (trackList.get(0) instanceof AudioTrack) {
                trackType = TRACK_TYPE_AUDIO;
            } else if (trackList.get(0) instanceof TextTrack) {
                trackType = TRACK_TYPE_TEXT;
            }

            if (trackType == TRACK_TYPE_AUDIO && currentAudioFormat != null && tracksInfo != null) {
                defaultTrackIndex = findDefaultTrackIndex(currentAudioFormat.language, trackList, defaultTrackIndex);
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
        boolean skipRestoreTrack = false;

        if (trackList.get(0) instanceof VideoTrack && DISABLED.equals(lastDisabledTrackIds[TRACK_TYPE_VIDEO])) {
            disableVideoTracks(true);
            skipRestoreTrack = true;
        }
        if (trackList.get(0) instanceof AudioTrack && DISABLED.equals(lastDisabledTrackIds[TRACK_TYPE_VIDEO])) {
            disableAudioTracks(true);
            skipRestoreTrack = true;
        }
        if (trackList.get(0) instanceof TextTrack && DISABLED.equals(lastDisabledTrackIds[TRACK_TYPE_VIDEO])) {
            disableTextTracks(true);
            skipRestoreTrack = true;
        }

        //If track was previously selected and selection is differed from the default selection apply it.
        String defaultUniqueId = trackList.get(defaultTrackIndex).getUniqueId();
        if (!NONE.equals(lastSelectedTrackId) && !lastSelectedTrackId.equals(defaultUniqueId)) {
            try {
                if (!skipRestoreTrack) {
                    changeTrack(lastSelectedTrackId);
                }
                for (int i = 0; i < trackList.size(); i++) {
                    if (lastSelectedTrackId.equals(trackList.get(i).getUniqueId())) {
                        return i;
                    }
                }
            } catch (IllegalArgumentException ex) {
                PKError currentError = new PKError(PKPlayerErrorType.UNEXPECTED, PKError.Severity.Fatal, ex.getMessage(), ex);
                tracksErrorListener.onUnsupportedTracksAvailableError(currentError);
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
                    PKVideoCodec currentVideoTrackCodec = getVideoCodec(format);
                    VideoTrack adaptiveVideoTrack = new VideoTrack(uniqueId, 0, 0, 0, format.selectionFlags, true, currentVideoTrackCodec, format.codecs);
                    if (!videoTracksCodecsMap.containsKey(currentVideoTrackCodec)) {
                        videoTracksCodecsMap.put(currentVideoTrackCodec, new ArrayList<>());
                    }
                    videoTracksCodecsMap.get(currentVideoTrackCodec).add(adaptiveVideoTrack);
                    break;
                case TRACK_TYPE_AUDIO:
                    audioTracks.add(new AudioTrack(uniqueId, format.language, format.label, 0, format.channelCount, format.selectionFlags, true, getAudioCodec(format), format.codecs));                    break;
                case TRACK_TYPE_TEXT:
                    textTracks.add(new TextTrack(uniqueId, format.language, format.label, format.sampleMimeType, format.selectionFlags));
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
            case TRACK_TYPE_IMAGE:
                return IMAGE_PREFIX;
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

    protected void changeTrack(String uniqueId) throws IllegalArgumentException {
        if (assertTrackSelectorIsNull("changeTrack")) {
            return;
        }

        log.i("Request change track to uniqueID -> " + uniqueId);
        mappedTrackInfo = selector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo == null) {
            log.w("Trying to get current MappedTrackInfo returns null. Do not change track with id - " + uniqueId);
            return;
        }

        int[] uniqueTrackId = validateUniqueId(uniqueId);
        int rendererIndex = uniqueTrackId[RENDERER_INDEX];
        int groupIndex = uniqueTrackId[GROUP_INDEX];

        requestedChangeTrackIds[rendererIndex] = uniqueId;

        if (uniqueId.contains(IMAGE_PREFIX)) {
            log.d("Image track changed to: " + requestedChangeTrackIds[TRACK_TYPE_IMAGE]);
            lastSelectedTrackIds[TRACK_TYPE_IMAGE] = requestedChangeTrackIds[TRACK_TYPE_IMAGE];
            tracksInfoListener.onImageTrackChanged();
            return;
        }

        List<Integer> selectedTrackIndices = retrieveOverrideSelection(uniqueTrackId);
        overrideTrack(rendererIndex, groupIndex, selectedTrackIndices);
    }

    protected void overrideMediaVideoCodec() {

        if (videoTracksCodecsMap.size() == 1) {
            // No need to execute further as override track ignores the parameters given to the trackselector
            // so elemenating this behaviour
            // We will execute this only in case when video tracks have more than one type of video codec available
            return;
        }

        if (assertTrackSelectorIsNull("overrideMediaVideoCodec")) {
            return;
        }

        List<String> uniqueIds = getVideoTracksUniqueIds();

        mappedTrackInfo = selector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo == null || uniqueIds.isEmpty()) {
            return;
        }

        int[] uniqueTrackId = validateUniqueId(uniqueIds.get(0));
        int rendererIndex = uniqueTrackId[RENDERER_INDEX];
        int groupIndex = uniqueTrackId[GROUP_INDEX];

        requestedChangeTrackIds[rendererIndex] = uniqueIds.get(0);

        List<Integer> selectedTrackIndices = retrieveOverrideSelectionList(validateAndBuildUniqueIds(uniqueIds));
        overrideTrack(rendererIndex, groupIndex, selectedTrackIndices);
    }

    protected void overrideMediaDefaultABR(long minAbr, long maxAbr, PKAbrFilter pkAbrFilter) {
        if (assertTrackSelectorIsNull("overrideMediaDefaultABR")) {
            return;
        }

        List<String> uniqueIds = getCodecUniqueIdsWithABR(minAbr, maxAbr, pkAbrFilter);

        mappedTrackInfo = selector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo == null || uniqueIds.isEmpty()) {
            return;
        }

        int[] uniqueTrackId = validateUniqueId(uniqueIds.get(0));
        int rendererIndex = uniqueTrackId[RENDERER_INDEX];
        int groupIndex = uniqueTrackId[GROUP_INDEX];

        requestedChangeTrackIds[rendererIndex] = (NONE.equals(lastSelectedTrackIds[rendererIndex])) ?
                uniqueIds.get(0) : lastSelectedTrackIds[rendererIndex];

        List<Integer> selectedTrackIndices = retrieveOverrideSelectionList(validateAndBuildUniqueIds(uniqueIds));
        overrideTrack(rendererIndex,groupIndex, selectedTrackIndices);
    }

    private List<String> getVideoTracksUniqueIds() {
        List<String> uniqueIds = new ArrayList<>();
        if (videoTracks != null) {
            Collections.sort(videoTracks);
            for (VideoTrack currentVideoTrack : videoTracks) {
                uniqueIds.add(currentVideoTrack.getUniqueId());
            }
        }
        return uniqueIds;
    }

    private List<String> getCodecUniqueIdsWithABR(long minAbr, long maxAbr, PKAbrFilter pkAbrFilter) {
        List<String> uniqueIds = new ArrayList<>();

        boolean isValidABRRange = true;

        if (videoTracks != null && !videoTracks.isEmpty()) {

            if (originalVideoTracks == null) {
                originalVideoTracks = new ArrayList<>(videoTracks);
            } else if (originalVideoTracks.isEmpty()) {
                originalVideoTracks.addAll(videoTracks);
            } else {
                videoTracks.clear();
                videoTracks.addAll(originalVideoTracks);
            }

            Collections.sort(videoTracks);
            if (videoTracks.size() >= 2) {
                long minValueInStream;
                long maxValueInStream;

                switch (pkAbrFilter) {
                    case HEIGHT:
                        Collections.sort(videoTracks, new VideoTrack.HeightComparator());
                        minValueInStream = videoTracks.get(1).getHeight();
                        maxValueInStream = videoTracks.get(videoTracks.size() - 1).getHeight();
                        break;
                    case WIDTH:
                        Collections.sort(videoTracks, new VideoTrack.WidthComparator());
                        minValueInStream = videoTracks.get(1).getWidth();
                        maxValueInStream = videoTracks.get(videoTracks.size() - 1).getWidth();
                        break;
                    case PIXEL:
                        Collections.sort(videoTracks, new VideoTrack.PixelComparator());
                        minValueInStream = videoTracks.get(1).getWidth() * videoTracks.get(1).getHeight();
                        maxValueInStream = videoTracks.get(videoTracks.size() - 1).getWidth() * videoTracks.get(videoTracks.size() - 1).getHeight();
                        break;
                    case NONE:
                    default:
                        minValueInStream = videoTracks.get(1).getBitrate();
                        maxValueInStream = videoTracks.get(videoTracks.size() - 1).getBitrate();
                        break;
                }

                if ((minAbr > maxValueInStream) || (maxAbr < minValueInStream)) {
                    isValidABRRange = false;
                    minAbr = Long.MIN_VALUE;
                    maxAbr = Long.MAX_VALUE;
                    pkAbrFilter = PKAbrFilter.NONE;
                    String errorMessage = "given minVideo ABR or maxVideo ABR is invalid";
                    PKError currentError = new PKError(PKPlayerErrorType.UNEXPECTED, PKError.Severity.Recoverable, errorMessage, new IllegalArgumentException(errorMessage));
                    tracksErrorListener.onTracksOverrideABRError(currentError);
                }
            }

            Iterator<VideoTrack> videoTrackIterator = videoTracks.iterator();
            int currentIndex = 0;
            int originalVideoTrackSize = originalVideoTracks.size();
            while (videoTrackIterator.hasNext()) {
                VideoTrack currentVideoTrack = videoTrackIterator.next();
                long currentAbrSelectedValue;
                long nextAbrSelectedValue;
                // Get the next track from the originalVideoTrack list
                int nextIndex = (currentIndex == originalVideoTrackSize - 1) ? -1 : currentIndex + 1;

                switch (pkAbrFilter) {
                    case HEIGHT:
                        currentAbrSelectedValue = currentVideoTrack.getHeight();
                        nextAbrSelectedValue = nextIndex != -1 ? originalVideoTracks.get(nextIndex).getHeight() : -1;
                        break;
                    case WIDTH:
                        currentAbrSelectedValue = currentVideoTrack.getWidth();
                        nextAbrSelectedValue = nextIndex != -1 ? originalVideoTracks.get(nextIndex).getWidth() : -1;
                        break;
                    case PIXEL:
                        currentAbrSelectedValue = currentVideoTrack.getWidth() * currentVideoTrack.getHeight();
                        nextAbrSelectedValue = nextIndex != -1 ?
                                originalVideoTracks.get(nextIndex).getWidth() * originalVideoTracks.get(nextIndex).getHeight() :
                                -1;
                        break;
                    case NONE:
                    default:
                        currentAbrSelectedValue = currentVideoTrack.getBitrate();
                        nextAbrSelectedValue = nextIndex != -1 ? originalVideoTracks.get(nextIndex).getBitrate() : -1;
                        break;
                }

                if ((currentAbrSelectedValue >= minAbr && currentAbrSelectedValue <= maxAbr) ||
                        (nextAbrSelectedValue != -1 && minAbr > currentAbrSelectedValue && maxAbr < nextAbrSelectedValue)) {
                    uniqueIds.add(currentVideoTrack.getUniqueId());
                } else {
                    if (currentVideoTrack.isAdaptive() || !isValidABRRange) {
                        uniqueIds.add(currentVideoTrack.getUniqueId());
                    } else {
                        videoTrackIterator.remove();
                    }
                }

                currentIndex++;
            }
        }
        return uniqueIds;
    }

    private List<Integer> retrieveOverrideSelectionList(int[][] uniqueIds) {
        if (uniqueIds == null || uniqueIds[0] == null) {
            throw new IllegalArgumentException("Track selection with uniqueId = null");
        }

        // Only for video tracks : RENDERER_INDEX is always 0 means video
        List<Integer> selectedIndices = new ArrayList<>();

        int rendererIndex = uniqueIds[0][RENDERER_INDEX];
        int groupIndex = uniqueIds[0][GROUP_INDEX];
        int trackIndex = uniqueIds[0][TRACK_INDEX];

        boolean isAdaptive = trackIndex == TRACK_ADAPTIVE;

        if (uniqueIds.length == 1 && isAdaptive) {
            selectedIndices.addAll(overrideAutoABRTracks(rendererIndex, groupIndex));
        } else if (uniqueIds.length > 1) {
            selectedIndices.addAll(overrideMediaDefaultABR(uniqueIds, rendererIndex, groupIndex));
        } else {
            selectedIndices.add(trackIndex);
        }
        return selectedIndices;
    }

    @NonNull
    private List<Integer> overrideMediaDefaultABR(int[][] uniqueIds, int rendererIndex, int groupIndex) {
        List<Integer> adaptiveTrackIndexesList = new ArrayList<>();

        switch (rendererIndex) {
            case TRACK_TYPE_VIDEO:
            case TRACK_TYPE_AUDIO:
                createAdaptiveTrackIndexList(uniqueIds, groupIndex, adaptiveTrackIndexesList);
                break;
        }
        return adaptiveTrackIndexesList;
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
    private List<Integer> retrieveOverrideSelection(int[] uniqueId) {
        List<Integer> selectedIndices = new ArrayList<>();

        int rendererIndex = uniqueId[RENDERER_INDEX];
        int groupIndex = uniqueId[GROUP_INDEX];
        int trackIndex = uniqueId[TRACK_INDEX];

        boolean isAdaptive = trackIndex == TRACK_ADAPTIVE;

        if (isAdaptive) {

            List<Integer> adaptiveTrackIndexesList = new ArrayList<>();

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
                                    if (audioTrack.getCodecType() != null &&
                                            audioTrack.getCodecType().equals(getAudioCodec(trackGroup.getFormat(ind)))) {
                                        adaptiveTrackIndexesList.add(ind);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    break;
            }

            selectedIndices.addAll(adaptiveTrackIndexesList);
        } else {
            selectedIndices.add(trackIndex);
        }

        return selectedIndices;
    }

    @NonNull
    private List<Integer> overrideAutoABRTracks(int rendererIndex, int groupIndex) {
        List<Integer> adaptiveTrackIndexesList = new ArrayList<>();

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

        return adaptiveTrackIndexesList;
    }

    /**
     * Actually doing the override action on the track.
     *
     * @param rendererIndex - renderer index on which we want to apply the change.
     * @param groupIndex - Group index of the track
     * @param selectedIndices - Indexes of the tracks which are actually needs to be overrides.
     *                        This index is of the selected `Format` inside the `mappedTrackInfo`
     *                        Generally all the Formats come in one `Trackgroup` but for TEXT,
     *                        it comes individual `TrackGroup` for each TEXT `Format`
     */
    private void overrideTrack(int rendererIndex, int groupIndex, List<Integer> selectedIndices) {
        if (assertTrackSelectorIsNull("overrideTrack")) {
            return;
        }

        //actually change track.
        TrackGroup trackGroup = mappedTrackInfo.getTrackGroups(rendererIndex).get(groupIndex);
        if (!selectedIndices.isEmpty()) {
            if (rendererIndex == TRACK_TYPE_TEXT && selectedIndices.get(0) == TRACK_DISABLED) {
                // Just clear the selected indices,
                // it will let exoplayer know that no tracks from trackGroup should be played
                selectedIndices.clear();
            }
            TrackSelectionOverride trackSelectionOverride = new TrackSelectionOverride(trackGroup, selectedIndices);
            trackSelectionParametersBuilder.setOverrideForType(trackSelectionOverride);
        } else {
            //clear all the selections if selectedIndices are empty.
            trackSelectionParametersBuilder.clearOverride(trackGroup);
        }

        selector.setParameters(trackSelectionParametersBuilder.build());
    }

    public void updateTrackSelectorParameter(PlayerSettings playerSettings, DefaultTrackSelector.Parameters.Builder parametersBuilder) {
        if (playerSettings == null) {
            return;
        }

        if (playerSettings.isTunneledAudioPlayback() && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            parametersBuilder.setTunnelingEnabled(playerSettings.isTunneledAudioPlayback());
        }
        if (playerSettings.getMaxAudioBitrate() != null) {
            parametersBuilder.setMaxAudioBitrate(playerSettings.getMaxAudioBitrate());
        }
        if (playerSettings.getMaxAudioChannelCount() > 0) {
            parametersBuilder.setMaxAudioChannelCount(playerSettings.getMaxAudioChannelCount());
        }
        if (playerSettings.getPreferredVideoCodecSettings().getAllowMixedCodecAdaptiveness()) {
            parametersBuilder.setAllowVideoMixedMimeTypeAdaptiveness(true);
        }
        parametersBuilder.setConstrainAudioChannelCountToDeviceCapabilities(playerSettings.isConstrainAudioChannelCountToDeviceCapabilities());
    }

    public ThumbnailInfo getThumbnailInfo(long positionMS) {

        if (imageTracks.isEmpty()) {
            return null;
        }

        ImageTrack imageTrack = null;
        for (int index = 0; index < imageTracks.size() ; index++) {
            if (imageTracks.get(index).getUniqueId().equals(lastSelectedTrackIds[TRACK_TYPE_IMAGE])) {
                imageTrack = imageTracks.get(index);
                break;
            }
        }

        if (imageTrack == null) {
            return null;
        }

        if (externalVttThumbnailRangesInfo == null) {

            long seq = (long) Math.floor(positionMS * 1.0 / imageTrack.getDuration());
            double offset = positionMS % imageTrack.getDuration();
            int thumbIndex = (int) Math.floor((offset * imageTrack.getCols() * imageTrack.getRows()) / imageTrack.getDuration());
            long seqIdx = seq + ((DashImageTrack) imageTrack).getStartNumber();
            float imageWidth = imageTrack.getWidth() / imageTrack.getCols();
            float imageHeight = imageTrack.getHeight() / imageTrack.getRows();
            float imageX = (float) Math.floor(thumbIndex % imageTrack.getCols()) * imageWidth;
            float imageY = (float) Math.floor(thumbIndex / imageTrack.getCols()) * imageHeight;

            long imageRealUrlTime = ((seqIdx - 1) * imageTrack.getDuration());
            String realImageUrl = imageTrack.getUrl().replace("$Number$", String.valueOf(seqIdx)).replace("$Time$", String.valueOf(imageRealUrlTime));
            return new ThumbnailInfo(realImageUrl, imageX, imageY, imageWidth, imageHeight);
        } else if (externalVttThumbnailRangesInfo.size() > 0) {

            List<Pair<Long, Long>> keySetList = new ArrayList<>(externalVttThumbnailRangesInfo.keySet());

            long thumbIndex = findImageIndex(keySetList, externalVttThumbnailRangesInfo.size(), positionMS);

            if (thumbIndex > externalVttThumbnailRangesInfo.size() - 1) {
                thumbIndex = externalVttThumbnailRangesInfo.size() - 1;
            } else if (thumbIndex < 0) {
                thumbIndex = 0;
            }
            return externalVttThumbnailRangesInfo.get(keySetList.get((int) thumbIndex));
        }
        return null;
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
                List<VideoTrack> allVideoTracks = new ArrayList<>();
                for (Map.Entry<PKVideoCodec,List<VideoTrack>> videoTrackEntry : videoTracksCodecsMap.entrySet()) {
                    allVideoTracks.addAll(videoTrackEntry.getValue());
                }
                trackList = allVideoTracks;
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
                == C.FORMAT_HANDLED;
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
                || uniqueId.contains(IMAGE_PREFIX)
                && uniqueId.contains(",")) {
            int trackTypeId = getTrackTypeId(uniqueId);
            int[] parsedUniqueId = parseUniqueId(uniqueId);
            if (!isRendererTypeValid(parsedUniqueId[RENDERER_INDEX])) {
                lastSelectedTrackIds[trackTypeId] = NONE;
                throw new IllegalArgumentException("Track selection with uniqueId = " + uniqueId + " failed. Due to invalid renderer index. " + parsedUniqueId[RENDERER_INDEX]);
            }

            if (!isGroupIndexValid(parsedUniqueId)) {
                lastSelectedTrackIds[trackTypeId] = NONE;
                throw new IllegalArgumentException("Track selection with uniqueId = " + uniqueId + " failed. Due to invalid group index. " + parsedUniqueId[GROUP_INDEX]);
            }

            if (!isTrackIndexValid(parsedUniqueId)) {
                lastSelectedTrackIds[trackTypeId] = NONE;
                throw new IllegalArgumentException("Track selection with uniqueId = " + uniqueId + " failed. Due to invalid track index. " + parsedUniqueId[TRACK_INDEX]);
            }
            return parsedUniqueId;
        }
        throw new IllegalArgumentException("Invalid structure of uniqueId " + uniqueId);
    }

    public int getTrackTypeId(String uniqueId) {
        if (uniqueId.contains(VIDEO_PREFIX)) {
            return TRACK_TYPE_VIDEO;
        }
        if (uniqueId.contains(AUDIO_PREFIX)) {
            return TRACK_TYPE_AUDIO;
        }
        if (uniqueId.contains(TEXT_PREFIX)) {
            return TRACK_TYPE_TEXT;
        }
        if (uniqueId.contains(IMAGE_PREFIX)) {
            return TRACK_TYPE_IMAGE;
        }
        return -1;
    }

    private boolean isTrackIndexValid(int[] parsedUniqueId) {
        int rendererIndex = parsedUniqueId[RENDERER_INDEX];
        int groupIndex = parsedUniqueId[GROUP_INDEX];
        int trackIndex = parsedUniqueId[TRACK_INDEX];

        if (rendererIndex == TRACK_TYPE_IMAGE) {
            return trackIndex >= TRACK_ADAPTIVE;
        }

        if (rendererIndex == TRACK_TYPE_TEXT) {
            return trackIndex != TRACK_ADAPTIVE
                    && trackIndex >= TRACK_DISABLED
                    && trackIndex < mappedTrackInfo.getTrackGroups(rendererIndex).get(groupIndex).length;
        }

        return trackIndex >= TRACK_ADAPTIVE
                && trackIndex < mappedTrackInfo.getTrackGroups(rendererIndex).get(groupIndex).length;
    }

    private boolean isGroupIndexValid(int[] parsedUniqueId) {
        if (parsedUniqueId[GROUP_INDEX] == TRACK_TYPE_IMAGE) {
            return true;
        }

        return parsedUniqueId[GROUP_INDEX] >= 0
                && parsedUniqueId[GROUP_INDEX] < mappedTrackInfo.getTrackGroups(parsedUniqueId[RENDERER_INDEX]).length;
    }

    private boolean isRendererTypeValid(int rendererIndex) {
        return rendererIndex >= TRACK_TYPE_VIDEO && rendererIndex <= TRACK_TYPE_IMAGE;
    }

    /**
     * Notify to log, that video/audio renderer has only unsupported tracks.
     */
    private void warnAboutUnsupportedRendererTypes() {
        boolean videoTrackUnsupported = false;
        boolean audioTrackUnsupported = false;
        String errorMessage;

        if (mappedTrackInfo.getTypeSupport(TRACK_TYPE_VIDEO)
                == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
            videoTrackUnsupported = true;
        }
        if (mappedTrackInfo.getTypeSupport(TRACK_TYPE_AUDIO)
                == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
            audioTrackUnsupported = true;
        }

        if (videoTrackUnsupported && audioTrackUnsupported) {
            errorMessage = "Warning! All the video and audio tracks are unsupported by this device.";
            tracksErrorListener.onUnsupportedAudioVideoTracksError(getUnsupportedTrackError(errorMessage));
        } else if (videoTrackUnsupported) {
            errorMessage = "Warning! All the video tracks are unsupported by this device.";
            tracksErrorListener.onUnsupportedVideoTracksError(getUnsupportedTrackError(errorMessage));
        } else if (audioTrackUnsupported) {
            errorMessage = "Warning! All the audio tracks are unsupported by this device.";
            tracksErrorListener.onUnsupportedAudioTracksError(getUnsupportedTrackError(errorMessage));
        }
    }

    private PKError getUnsupportedTrackError(String errorMessage) {
        return new PKError(PKPlayerErrorType.UNEXPECTED, PKError.Severity.Recoverable, errorMessage, new IllegalStateException(errorMessage));
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
        imageTracks.clear();
        currentVideoFormat = null;
        currentAudioFormat = null;
        for (Map.Entry<PKVideoCodec,List<VideoTrack>> videoTrackEntry : videoTracksCodecsMap.entrySet()) {
            videoTrackEntry.getValue().clear();
        }
        videoTracksCodecsMap.clear();
        audioTracksCodecsMap.clear();
        subtitleListMap.clear();
        if (originalVideoTracks != null) {
            originalVideoTracks.clear();
        }
        if (externalVttThumbnailRangesInfo != null) {
            externalVttThumbnailRangesInfo.clear();
            externalVttThumbnailRangesInfo = null;
        }
    }

    protected void release() {
        if (tracksInfoListener != null) {
            tracksInfoListener.onRelease(lastSelectedTrackIds, lastDisabledTrackIds);
        }

        tracksInfoListener = null;
        trackSelectionParameters = null;
        trackSelectionParametersBuilder = null;

        if (selector != null) {
            selector.release();
        }

        clearTracksLists();
    }

    protected boolean isAudioOnlyStream() {
        if (tracksInfo != null) {
            return !tracksInfo.containsType(C.TRACK_TYPE_VIDEO);
        }
        return false;
    }

    protected long getCurrentVideoBitrate() {
        if (currentVideoFormat != null) {
            return currentVideoFormat.bitrate;
        }
        return -1;
    }

    protected long getCurrentVideoWidth() {
        if (currentVideoFormat != null) {
            return currentVideoFormat.width;
        }
        return -1;
    }

    protected long getCurrentVideoHeight() {
        if (currentVideoFormat != null) {
            return currentVideoFormat.height;
        }
        return -1;
    }

    protected long getCurrentAudioBitrate() {
        if (currentAudioFormat != null) {
            return currentAudioFormat.bitrate;
        }
        return -1;
    }

    protected void setCurrentVideoFormat(@NonNull Format videoFormat) {
        currentVideoFormat = videoFormat;
    }

    protected void setCurrentAudioFormat(@NonNull Format audioFormat) {
        currentAudioFormat = audioFormat;
    }

    @Nullable
    private Format getSelectedTextTrackFormat() {
        if (tracksInfo != null && !tracksInfo.getGroups().isEmpty()) {
            for (Tracks.Group trackGroupInfo : tracksInfo.getGroups()) {

                if (trackGroupInfo.getType() == C.TRACK_TYPE_TEXT &&
                        trackGroupInfo.isSelected() &&
                        trackGroupInfo.getMediaTrackGroup().length > 0) {
                    for (int groupIndex = 0; groupIndex < trackGroupInfo.length; groupIndex++) {

                        if (trackGroupInfo.isTrackSelected(groupIndex)) {
                            return trackGroupInfo.getTrackFormat(groupIndex);
                        }
                    }
                }
            }
        }

        return null;
    }

    protected void notifyAboutTrackChange(Tracks trackSelections) {

        this.tracksInfo = trackSelections;
        if (tracksInfoListener == null) {
            return;
        }

        if (shouldNotifyAboutTrackChanged(TRACK_TYPE_VIDEO)) {
            log.d("Video track changed to: " + requestedChangeTrackIds[TRACK_TYPE_VIDEO]);
            lastSelectedTrackIds[TRACK_TYPE_VIDEO] = requestedChangeTrackIds[TRACK_TYPE_VIDEO];
            tracksInfoListener.onVideoTrackChanged();
        }

        if (shouldNotifyAboutTrackChanged(TRACK_TYPE_AUDIO)) {
            log.d("Audio track changed to: " + requestedChangeTrackIds[TRACK_TYPE_AUDIO]);
            lastSelectedTrackIds[TRACK_TYPE_AUDIO] = requestedChangeTrackIds[TRACK_TYPE_AUDIO];
            tracksInfoListener.onAudioTrackChanged();
        }

        if (shouldNotifyAboutTrackChanged(TRACK_TYPE_TEXT)) {
            log.d("Text track changed to: " + requestedChangeTrackIds[TRACK_TYPE_TEXT]);
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
            case TRACK_TYPE_IMAGE:
                for (ImageTrack track : imageTracks) {
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
        lastSelectedTrackIds = new String[]{NONE, NONE, NONE, NONE};
        lastDisabledTrackIds = new String[]{NONE, NONE, NONE, NONE};
        requestedChangeTrackIds = new String[]{NONE, NONE, NONE, NONE};
        tracksInfo = null;
        mappedTrackInfo = null;
        videoTracks.clear();
        audioTracks.clear();
        textTracks.clear();
        imageTracks.clear();
        currentVideoFormat = null;
        currentAudioFormat = null;
        mediaItemManifestURL = null;
        redirectPlaybackURL = null;

        if (originalVideoTracks != null) {
            originalVideoTracks.clear();
        }
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
        PKTrackConfig preferredTextTrackConfig = playerSettings.getPreferredTextTrackConfig();

        if (preferredTextTrackConfig == null) {
            return preferredTrackUniqueId;
        }

        String preferredTextISO3Lang = preferredTextTrackConfig.getTrackLanguage();
        if (preferredTextISO3Lang != null) {
            for (TextTrack track : textTracks) {
                String trackLang = track.getLanguage();
                if (trackLang == null) {
                    continue;
                }

                if (NONE.equals(preferredTextISO3Lang) && NONE.equals(trackLang)) {
                    preferredTrackUniqueId = track.getUniqueId();
                    break;
                } else if (NONE.equals(trackLang)) {
                    continue;
                }

                Locale streamLang = new Locale(trackLang);
                try {
                    if (streamLang.getISO3Language().equals(preferredTextISO3Lang)) {
                        log.d("changing track type " + trackType + " to " + preferredTextISO3Lang);
                        preferredTrackUniqueId = track.getUniqueId();
                        break;
                    }
                } catch (MissingResourceException | NullPointerException ex) {
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
        PKTrackConfig preferredTextTrackConfig = playerSettings.getPreferredTextTrackConfig();
        if (preferredTextTrackConfig != null && preferredTextTrackConfig.getPreferredMode() == PKTrackConfig.Mode.AUTO && textTracks != null) {
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
        PKTrackConfig preferredAudioTrackConfig = playerSettings.getPreferredAudioTrackConfig();

        if (preferredAudioTrackConfig == null) {
            return preferredTrackUniqueId;
        }

        String preferredAudioISO3Lang = preferredAudioTrackConfig.getTrackLanguage();
        for (AudioTrack track : audioTracks) {
            String trackLang = track.getLanguage();
            if (trackLang == null) {
                continue;
            }
            Locale streamLang = new Locale(trackLang);
            try {
                if (streamLang.getISO3Language().equals(preferredAudioISO3Lang)) {
                    log.d("changing track type " + trackType + " to " + preferredAudioISO3Lang);
                    preferredTrackUniqueId = track.getUniqueId();
                    break;
                }
            } catch (MissingResourceException | NullPointerException ex) {
                log.e(ex.getMessage());
            }
        }
        return preferredTrackUniqueId;
    }

    private PKVideoCodec getVideoCodec(Format format) {
        String codec = format.codecs;
        if (codec != null) {
            if (codec.startsWith("hev1") || codec.startsWith("hvc1")) {
                return PKVideoCodec.HEVC;
            }  else if (codec.startsWith("vp9") || codec.startsWith("vp09")) {
                return PKVideoCodec.VP9;
            } else if (codec.startsWith("vp8") || codec.startsWith("vp08")) {
                return PKVideoCodec.VP8;
            } else if (codec.startsWith("av01")) {
                return PKVideoCodec.AV1;
            }
        }

        if (VIDEO_H265.equals(format.sampleMimeType)) {
            return PKVideoCodec.HEVC;
        } else if (VIDEO_VP8.equals(format.sampleMimeType)) {
            return PKVideoCodec.VP8;
        } else if (VIDEO_VP9.equals(format.sampleMimeType)) {
            return PKVideoCodec.VP9;
        } else if (VIDEO_AV1.equals(format.sampleMimeType)) {
            return PKVideoCodec.AV1;
        }

        return PKVideoCodec.AVC; //VIDEO_H264
    }

    private PKAudioCodec getAudioCodec(Format format) {
        String codec = format.codecs;
        if (codec != null) {
            if (codec.startsWith("mp4a")) {
                return PKAudioCodec.AAC;
            } else if (codec.startsWith("ac-3") || codec.startsWith("dac3")) {
                return PKAudioCodec.AC3;
            } else if (codec.startsWith("ec-3") || codec.startsWith("dec3")) {
                return PKAudioCodec.E_AC3;
            } else if (codec.startsWith("opus")) {
                return PKAudioCodec.OPUS;
            }
        }
        return PKAudioCodec.AAC;
    }

    private boolean isValidPreferredAudioConfig() {
        if (playerSettings == null) {
            return false;
        }

        PKTrackConfig preferredAudioTrackConfig = playerSettings.getPreferredAudioTrackConfig();
        return !(preferredAudioTrackConfig == null ||
                preferredAudioTrackConfig.getPreferredMode() == null ||
                preferredAudioTrackConfig.getPreferredMode() == PKTrackConfig.Mode.OFF ||
                (preferredAudioTrackConfig.getPreferredMode() == PKTrackConfig.Mode.SELECTION && preferredAudioTrackConfig.getTrackLanguage() == null));
    }

    private boolean isValidPreferredTextConfig() {
        if (playerSettings == null) {
            return false;
        }

        PKTrackConfig preferredTextTrackConfig = playerSettings.getPreferredTextTrackConfig();
        return !(preferredTextTrackConfig == null ||
                preferredTextTrackConfig.getPreferredMode() == null ||
                (preferredTextTrackConfig.getPreferredMode() == PKTrackConfig.Mode.SELECTION && preferredTextTrackConfig.getTrackLanguage() == null));
    }

    private boolean assertTrackSelectorIsNull(String methodName) {
        if (selector == null) {
            String nullTrackSelectorMsgFormat = "Attempt to invoke '%s' on null instance of the track selector";
            log.w(String.format(nullTrackSelectorMsgFormat, methodName));
            return true;
        }
        return false;
    }

    public void applyPlayerSettings(PlayerSettings settings) {
        if (settings != null) {
            this.playerSettings = settings;
        }
    }

    protected void hasExternalSubtitles(boolean hasExternalSubtitles) {
        this.hasExternalSubtitles = hasExternalSubtitles;
    }

    public static boolean isCodecSupported(@NonNull String codecs, @Nullable TrackType type, boolean allowSoftware) {

        if (type == TrackType.TEXT) {
            return true;    // always supported
        }

        if (codecs == null) {
            log.w("isFormatSupported: codecs==null, assuming supported");
            return true;
        }

        if (type == null) {
            // type==null: HLS muxed track with a <video,audio> tuple
            final String[] split = TextUtils.split(codecs, ",");
            boolean result = true;
            switch (split.length) {
                case 0: return false;
                case 2: result = PKCodecSupport.hasDecoder(split[1], false, allowSoftware);
                    // fallthrough
                case 1: result &= PKCodecSupport.hasDecoder(split[0], false, allowSoftware);
            }
            return result;

        } else {
            return PKCodecSupport.hasDecoder(codecs, false, allowSoftware);
        }
    }

    private int findImageIndex(List<Pair<Long, Long>> sortedRangesList, int listSize, long positionMS) {
        int low = 0, high = listSize - 1;

        // Binary search
        while (low <= high)
        {
            // Find the mid element
            int mid = (low + high) >> 1;
            // If element is found
            if (positionMS >= sortedRangesList.get(mid).first &&
                    positionMS <= sortedRangesList.get(mid).second)
                return mid;

                // Check in first half
            else if (positionMS < sortedRangesList.get(mid).first)
                high = mid - 1;
                // Check in second half
            else
                low = mid + 1;
        }
        // Not found
        return -1;
    }

    private int getExoTrackType(int rendererIndex) {
        int exoTrackType = -1;
        switch(rendererIndex) {
            case TRACK_TYPE_VIDEO:
                exoTrackType = C.TRACK_TYPE_VIDEO;
                break;
            case TRACK_TYPE_AUDIO:
                exoTrackType = C.TRACK_TYPE_AUDIO;
                break;
            case TRACK_TYPE_TEXT:
                exoTrackType = C.TRACK_TYPE_TEXT;
                break;
            case TRACK_TYPE_IMAGE:
                exoTrackType = C.TRACK_TYPE_IMAGE;
                break;
        }
        return exoTrackType;
    }

    private void disableTrack(int rendererIndex) {
        switch (rendererIndex) {
            case TRACK_TYPE_VIDEO:
                disableVideoTracks(true);
                break;
            case TRACK_TYPE_AUDIO:
                disableAudioTracks(true);
                break;
            case TRACK_TYPE_TEXT:
                disableTextTracks(true);
                break;
            default:
                break;
        }
    }

    public void disableVideoTracks(boolean isDisabled) {
        log.d("disableVideoTracks isDisabled:" + isDisabled);
        disableTracks(TRACK_TYPE_VIDEO, isDisabled);
    }

    public void disableAudioTracks(boolean isDisabled) {
        log.d("disableAudioTracks isDisabled:" + isDisabled);
        disableTracks(TRACK_TYPE_AUDIO, isDisabled);
    }

    public void disableTextTracks(boolean isDisabled) {
        log.d("disableTextTracks isDisabled:" + isDisabled);
        disableTracks(TRACK_TYPE_TEXT, isDisabled);
    }

    private void disableTracks(int trackType, boolean isDisabled) {

        if (trackType == TRACK_TYPE_VIDEO && videoTracks.isEmpty()) {
            return;
        }

        if (trackType == TRACK_TYPE_AUDIO && audioTracks.isEmpty()) {
            return;
        }

        if (trackType == TRACK_TYPE_TEXT && textTracks.isEmpty()) {
            return;
        }

        if (isDisabled) {
            trackSelectionParametersBuilder.setTrackTypeDisabled(getExoTrackType(trackType), true);
            trackSelectionParametersBuilder.clearOverridesOfType(getExoTrackType(trackType));
            lastDisabledTrackIds[trackType] = DISABLED;
        } else {
            String uniqueTrackId = lastSelectedTrackIds[trackType];
            log.d("uniqueTrackId :" + uniqueTrackId);
            if (!NONE.equals(uniqueTrackId)) {
                int[] parsedUniqueId = parseUniqueId(uniqueTrackId);
                int rendererIndex = parsedUniqueId[RENDERER_INDEX];
                int groupIndex = parsedUniqueId[GROUP_INDEX];
                TrackGroup trackGroup = mappedTrackInfo.getTrackGroups(rendererIndex).get(groupIndex);

                List<Integer> selectedTrackIndices = retrieveOverrideSelection(parsedUniqueId);

                TrackSelectionOverride trackSelectionOverride = new TrackSelectionOverride(trackGroup, selectedTrackIndices);
                trackSelectionParametersBuilder.setOverrideForType(trackSelectionOverride);
                lastDisabledTrackIds[trackType] = NONE;
            }
            trackSelectionParametersBuilder.setTrackTypeDisabled(getExoTrackType(trackType), false);
        }
        selector.setParameters(trackSelectionParametersBuilder.build());
    }
}
