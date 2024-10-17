package com.kaltura.androidx.media3.exoplayer.dashmanifestparser;

import com.kaltura.androidx.media3.common.C;
import com.kaltura.androidx.media3.exoplayer.dash.manifest.Descriptor;
import com.kaltura.androidx.media3.exoplayer.dash.manifest.Representation;

import java.util.Collections;
import java.util.List;

/**
 * Represents a set of interchangeable encoded versions of a media content component.
 */
public class CustomAdaptationSet {

    /**
     * Value of {@link #id} indicating no value is set.=
     */
    public static final int ID_UNSET = -1;

    /**
     * A non-negative identifier for the adaptation set that's unique in the scope of its containing
     * period, or {@link #ID_UNSET} if not specified.
     */
    public final int id;

    /** The {@link C.TrackType track type} of the adaptation set. */
    public final @C.TrackType int type;

    /**
     * {@link Representation}s in the adaptation set.
     */
    public final List<CustomRepresentation> representations;

    /**
     * Accessibility descriptors in the adaptation set.
     */
    public final List<Descriptor> accessibilityDescriptors;

    /** Essential properties in the adaptation set. */
    public final List<Descriptor> essentialProperties;

    /** Supplemental properties in the adaptation set. */
    public final List<Descriptor> supplementalProperties;

    /**
     * @param id A non-negative identifier for the adaptation set that's unique in the scope of its
     *     containing period, or {@link #ID_UNSET} if not specified.
     * @param type The {@link C.TrackType track type} of the adaptation set.
     * @param representations {@link Representation}s in the adaptation set.
     * @param accessibilityDescriptors Accessibility descriptors in the adaptation set.
     * @param essentialProperties Essential properties in the adaptation set.
     * @param supplementalProperties Supplemental properties in the adaptation set.
     */
    public CustomAdaptationSet(
            int id,
            @C.TrackType int type,
            List<CustomRepresentation> representations,
            List<Descriptor> accessibilityDescriptors,
            List<Descriptor> essentialProperties,
            List<Descriptor> supplementalProperties) {
        this.id = id;
        this.type = type;
        this.representations = Collections.unmodifiableList(representations);
        this.accessibilityDescriptors = Collections.unmodifiableList(accessibilityDescriptors);
        this.essentialProperties = Collections.unmodifiableList(essentialProperties);
        this.supplementalProperties = Collections.unmodifiableList(supplementalProperties);
    }
}
