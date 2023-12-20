package com.kaltura.androidx.media3.exoplayer.dashmanifestparser;

import android.net.Uri;
import androidx.annotation.Nullable;
import com.kaltura.androidx.media3.common.C;
import com.kaltura.androidx.media3.exoplayer.offline.FilterableManifest;
import com.kaltura.androidx.media3.common.StreamKey;
import com.kaltura.androidx.media3.exoplayer.dash.manifest.ProgramInformation;
import com.kaltura.androidx.media3.exoplayer.dash.manifest.ServiceDescriptionElement;
import com.kaltura.androidx.media3.exoplayer.dash.manifest.UtcTimingElement;
import com.kaltura.androidx.media3.common.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents a DASH media presentation description (mpd), as defined by ISO/IEC 23009-1:2014
 * Section 5.3.1.2.
 */
public class CustomDashManifest implements FilterableManifest<CustomDashManifest> {

    /**
     * The {@code availabilityStartTime} value in milliseconds since epoch, or {@link C#TIME_UNSET} if
     * not present.
     */
    public final long availabilityStartTimeMs;

    /**
     * The duration of the presentation in milliseconds, or {@link C#TIME_UNSET} if not applicable.
     */
    public final long durationMs;

    /**
     * The {@code minBufferTime} value in milliseconds, or {@link C#TIME_UNSET} if not present.
     */
    public final long minBufferTimeMs;

    /**
     * Whether the manifest has value "dynamic" for the {@code type} attribute.
     */
    public final boolean dynamic;

    /**
     * The {@code minimumUpdatePeriod} value in milliseconds, or {@link C#TIME_UNSET} if not
     * applicable.
     */
    public final long minUpdatePeriodMs;

    /**
     * The {@code timeShiftBufferDepth} value in milliseconds, or {@link C#TIME_UNSET} if not
     * present.
     */
    public final long timeShiftBufferDepthMs;

    /**
     * The {@code suggestedPresentationDelay} value in milliseconds, or {@link C#TIME_UNSET} if not
     * present.
     */
    public final long suggestedPresentationDelayMs;

    /**
     * The {@code publishTime} value in milliseconds since epoch, or {@link C#TIME_UNSET} if
     * not present.
     */
    public final long publishTimeMs;

    /**
     * The {@link UtcTimingElement}, or null if not present. Defined in DVB A168:7/2016, Section
     * 4.7.2.
     */
    @Nullable public final UtcTimingElement utcTiming;

    /** The {@link ServiceDescriptionElement}, or null if not present. */
    @Nullable public final ServiceDescriptionElement serviceDescription;

    /** The location of this manifest, or null if not present. */
    @Nullable public final Uri location;

    /** The {@link ProgramInformation}, or null if not present. */
    @Nullable public final ProgramInformation programInformation;

    private final List<CustomPeriod> periods;

    public CustomDashManifest(
            long availabilityStartTimeMs,
            long durationMs,
            long minBufferTimeMs,
            boolean dynamic,
            long minUpdatePeriodMs,
            long timeShiftBufferDepthMs,
            long suggestedPresentationDelayMs,
            long publishTimeMs,
            @Nullable ProgramInformation programInformation,
            @Nullable UtcTimingElement utcTiming,
            @Nullable ServiceDescriptionElement serviceDescription,
            @Nullable Uri location,
            List<CustomPeriod> periods) {
        this.availabilityStartTimeMs = availabilityStartTimeMs;
        this.durationMs = durationMs;
        this.minBufferTimeMs = minBufferTimeMs;
        this.dynamic = dynamic;
        this.minUpdatePeriodMs = minUpdatePeriodMs;
        this.timeShiftBufferDepthMs = timeShiftBufferDepthMs;
        this.suggestedPresentationDelayMs = suggestedPresentationDelayMs;
        this.publishTimeMs = publishTimeMs;
        this.programInformation = programInformation;
        this.utcTiming = utcTiming;
        this.location = location;
        this.serviceDescription = serviceDescription;
        this.periods = periods == null ? Collections.emptyList() : periods;
    }

    public final int getPeriodCount() {
        return periods.size();
    }

    public final CustomPeriod getPeriod(int index) {
        return periods.get(index);
    }

    public final long getPeriodDurationMs(int index) {
        return index == periods.size() - 1
                ? (durationMs == C.TIME_UNSET ? C.TIME_UNSET : (durationMs - periods.get(index).startMs))
                : (periods.get(index + 1).startMs - periods.get(index).startMs);
    }

    public final long getPeriodDurationUs(int index) {
        return Util.msToUs(getPeriodDurationMs(index));
    }

    @Override
    public final CustomDashManifest copy(List<StreamKey> streamKeys) {
        LinkedList<StreamKey> keys = new LinkedList<>(streamKeys);
        Collections.sort(keys);
        keys.add(new StreamKey(-1, -1, -1)); // Add a stopper key to the end

        ArrayList<CustomPeriod> copyPeriods = new ArrayList<>();
        long shiftMs = 0;
        for (int periodIndex = 0; periodIndex < getPeriodCount(); periodIndex++) {
            if (keys.peek().periodIndex != periodIndex) {
                // No representations selected in this period.
                long periodDurationMs = getPeriodDurationMs(periodIndex);
                if (periodDurationMs != C.TIME_UNSET) {
                    shiftMs += periodDurationMs;
                }
            } else {
                CustomPeriod period = getPeriod(periodIndex);
                ArrayList<CustomAdaptationSet> copyAdaptationSets =
                        copyAdaptationSets(period.adaptationSets, keys);
                CustomPeriod copiedPeriod = new CustomPeriod(period.id, period.startMs - shiftMs, copyAdaptationSets,
                        period.eventStreams);
                copyPeriods.add(copiedPeriod);
            }
        }
        long newDuration = durationMs != C.TIME_UNSET ? durationMs - shiftMs : C.TIME_UNSET;
        return new CustomDashManifest(
                availabilityStartTimeMs,
                newDuration,
                minBufferTimeMs,
                dynamic,
                minUpdatePeriodMs,
                timeShiftBufferDepthMs,
                suggestedPresentationDelayMs,
                publishTimeMs,
                programInformation,
                utcTiming,
                serviceDescription,
                location,
                copyPeriods);
    }

    private static ArrayList<CustomAdaptationSet> copyAdaptationSets(
            List<CustomAdaptationSet> adaptationSets, LinkedList<StreamKey> keys) {
        StreamKey key = keys.poll();
        int periodIndex = key.periodIndex;
        ArrayList<CustomAdaptationSet> copyAdaptationSets = new ArrayList<>();
        do {
            int adaptationSetIndex = key.groupIndex;
            CustomAdaptationSet adaptationSet = adaptationSets.get(adaptationSetIndex);

            List<CustomRepresentation> representations = adaptationSet.representations;
            ArrayList<CustomRepresentation> copyRepresentations = new ArrayList<>();
            do {
                CustomRepresentation representation = representations.get(key.streamIndex);
                copyRepresentations.add(representation);
                key = keys.poll();
            } while (key.periodIndex == periodIndex && key.groupIndex == adaptationSetIndex);

            copyAdaptationSets.add(
                    new CustomAdaptationSet(
                            adaptationSet.id,
                            adaptationSet.type,
                            copyRepresentations,
                            adaptationSet.accessibilityDescriptors,
                            adaptationSet.essentialProperties,
                            adaptationSet.supplementalProperties));
        } while(key.periodIndex == periodIndex);
        // Add back the last key which doesn't belong to the period being processed
        keys.addFirst(key);
        return copyAdaptationSets;
    }

}
