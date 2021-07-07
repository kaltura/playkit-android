package com.kaltura.testhelper

import com.kaltura.android.exoplayer2.C.FORMAT_HANDLED
import com.kaltura.android.exoplayer2.C.FORMAT_UNSUPPORTED_TYPE
import com.kaltura.android.exoplayer2.Format
import com.kaltura.android.exoplayer2.RendererCapabilities
import com.kaltura.android.exoplayer2.RendererCapabilities.ADAPTIVE_SEAMLESS
import com.kaltura.android.exoplayer2.RendererCapabilities.TUNNELING_NOT_SUPPORTED
import com.kaltura.android.exoplayer2.util.MimeTypes
import com.kaltura.android.exoplayer2.util.Util

/**
 * Returns [FakeRendererCapabilities] that advertises support level using given value for
 * all tracks of the given type.
 *
 * @param trackType the track type of all formats that this renderer capabilities advertises
 * support for.
 * @param supportValue the [Capabilities] that will be returned for formats with the given
 * type.
 */
class FakeRendererCapabilities(private var trackType: Int) : RendererCapabilities {

    @RendererCapabilities.Capabilities
    private var supportValue = 0

    init {
        this.supportValue = RendererCapabilities.create(FORMAT_HANDLED, ADAPTIVE_SEAMLESS, TUNNELING_NOT_SUPPORTED)
    }

    override fun getName(): String {
        return "FakeRenderer(" + Util.getTrackTypeString(trackType).toString() + ")"
    }

    override fun getTrackType(): Int {
        return trackType
    }

    @RendererCapabilities.Capabilities
    override fun supportsFormat(format: Format): Int {
        return if (MimeTypes.getTrackType(format.sampleMimeType) === trackType) supportValue else RendererCapabilities.create(FORMAT_UNSUPPORTED_TYPE)
    }

    @RendererCapabilities.AdaptiveSupport
    override fun supportsMixedMimeTypeAdaptation(): Int {
        return ADAPTIVE_SEAMLESS
    }
}
