package com.kaltura.playkit.player

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kaltura.android.exoplayer2.C
import com.kaltura.android.exoplayer2.Format
import com.kaltura.android.exoplayer2.RendererCapabilities
import com.kaltura.android.exoplayer2.dashmanifestparser.CustomDashManifest
import com.kaltura.android.exoplayer2.dashmanifestparser.CustomDashManifestParser
import com.kaltura.android.exoplayer2.dashmanifestparser.CustomFormat
import com.kaltura.android.exoplayer2.source.MediaSource
import com.kaltura.android.exoplayer2.source.TrackGroupArray
import com.kaltura.android.exoplayer2.trackselection.DefaultTrackSelector
import com.kaltura.android.exoplayer2.trackselection.TrackSelectionArray
import com.kaltura.android.exoplayer2.trackselection.TrackSelector
import com.kaltura.android.exoplayer2.trackselection.TrackSelectorResult
import com.kaltura.android.exoplayer2.upstream.BandwidthMeter
import com.kaltura.testhelper.FakeRendererCapabilities
import com.kaltura.testhelper.FakeTimeline
import com.kaltura.testhelper.TestUtils
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import kotlin.collections.ArrayList

@RunWith(AndroidJUnit4::class)
internal class TrackSelectionHelperTest {

    private val initialDashAssetPath: String = "testdata/mpd/"
    private val dashFilePrefix: String = "sample_dash"
    private val initialHlsAssetPath: String = "testdata/m3u8/"
    private val hlsFilePrefix: String = "sample_hls"

    private lateinit var actualSelector: TrackSelector
    private var mockedSelector: DefaultTrackSelector = mock(DefaultTrackSelector::class.java)

    private val lastSelectedTrackIds = arrayOf(TrackSelectionHelper.NONE, TrackSelectionHelper.NONE, TrackSelectionHelper.NONE, TrackSelectionHelper.NONE)

    private lateinit var invalidationListener: TrackSelector.InvalidationListener
    private lateinit var bandwidthMeter: BandwidthMeter
    private lateinit var playerSettings: PlayerSettings
    private lateinit var trackGroupArray: TrackGroupArray

    private val VIDEO_CAPABILITIES: RendererCapabilities = FakeRendererCapabilities(C.TRACK_TYPE_VIDEO)
    private val AUDIO_CAPABILITIES: RendererCapabilities = FakeRendererCapabilities(C.TRACK_TYPE_AUDIO)
    private val TEXT_CAPABILITIES: RendererCapabilities = FakeRendererCapabilities(C.TRACK_TYPE_TEXT)
    private val METADATA_CAPABILITIES: RendererCapabilities = FakeRendererCapabilities(C.TRACK_TYPE_METADATA)
    private val CAMERA_MOTION_CAPABILITIES: RendererCapabilities = FakeRendererCapabilities(C.TRACK_TYPE_CAMERA_MOTION)
    private val RENDERER_CAPABILITIES: Array<RendererCapabilities> =
            arrayOf(VIDEO_CAPABILITIES,
                    AUDIO_CAPABILITIES,
                    TEXT_CAPABILITIES,
                    METADATA_CAPABILITIES,
                    CAMERA_MOTION_CAPABILITIES)

    @Before
    fun setup() {
        invalidationListener = mock(TrackSelector.InvalidationListener::class.java)
        bandwidthMeter = mock(BandwidthMeter::class.java)
        playerSettings = PlayerSettings()
        `when`(bandwidthMeter.bitrateEstimate).thenReturn(100000L)
    }

    private fun buildActualTrackSelector(fileName: String): TrackSelector {
        val timeline = FakeTimeline()
        val filePath: String

        if (fileName.startsWith(dashFilePrefix)) {
            filePath = "${initialDashAssetPath}${fileName}"
            trackGroupArray = TestUtils.getTrackGroupArrayFromDashManifest(
                    ApplicationProvider.getApplicationContext(),
                    filePath)
        } else {
            filePath = "${initialHlsAssetPath}${fileName}"
            trackGroupArray = TestUtils.getTrackGroupArrayFromHlsManifest(
                    ApplicationProvider.getApplicationContext(),
                    filePath)
        }

        actualSelector = DefaultTrackSelector(ApplicationProvider.getApplicationContext() as Context)
        actualSelector.init(invalidationListener, bandwidthMeter)

        val trackSelectorResult: TrackSelectorResult = actualSelector.selectTracks(RENDERER_CAPABILITIES,
                trackGroupArray,
                MediaSource.MediaPeriodId(timeline.getUidOfPeriod(0)),
                FakeTimeline())

        actualSelector.onSelectionActivated(trackSelectorResult.info)

        return actualSelector
    }

    private fun getTrackSelectionHelper(selector: TrackSelector): TrackSelectionHelper {
        val trackSelectionHelper = TrackSelectionHelper(
                ApplicationProvider.getApplicationContext(),
                selector as DefaultTrackSelector,
                lastSelectedTrackIds)
        trackSelectionHelper.applyPlayerSettings(playerSettings)

        return trackSelectionHelper
    }

    @Test
    fun buildTracks_mappedTrackInfo() {
        val trackSelectionHelper = getTrackSelectionHelper(buildActualTrackSelector("sample_dash_clear_h264_tears"))
        trackSelectionHelper.prepareTracks(TrackSelectionArray(), null).let {
            assertTrue(it)
        }

        val trackSelectionHelperHls = getTrackSelectionHelper(buildActualTrackSelector("sample_hls_harold"))
        trackSelectionHelperHls.prepareTracks(TrackSelectionArray(), null).let {
            assertTrue(it)
        }
    }

    @Test
    fun buildTracks_mappedTrackInfo_instream_subtitles() {
        // Dash
        val trackSelectionHelper = getTrackSelectionHelper(buildActualTrackSelector("sample_dash_with_subtitle"))
        val spyTrackSelectionHelper = spy(trackSelectionHelper)
        spyTrackSelectionHelper?.prepareTracks(TrackSelectionArray(), null)
        verify(spyTrackSelectionHelper)?.buildTracks(ArrayList<CustomFormat>())

        val pkTracks: PKTracks? = spyTrackSelectionHelper?.buildTracks(null)
        assertEquals(6, pkTracks?.textTracks?.size)

        // HLS
        val trackSelectionHelperHls = getTrackSelectionHelper(buildActualTrackSelector("sample_hls_harold"))
        val spyTrackSelectionHelperHls = spy(trackSelectionHelperHls)
        spyTrackSelectionHelperHls?.prepareTracks(TrackSelectionArray(), null)
        verify(spyTrackSelectionHelperHls)?.buildTracks(ArrayList<CustomFormat>())

        val pkTracksHls: PKTracks? = spyTrackSelectionHelperHls?.buildTracks(null)
        assertEquals(9, pkTracksHls?.textTracks?.size)
    }

    @Test
    fun buildTracks_mappedTrackInfo_is_null() {
        val trackSelectionHelper = getTrackSelectionHelper(mockedSelector)
        trackSelectionHelper.prepareTracks(TrackSelectionArray(), null).let {
            assertFalse(it)
        }
    }

    @Test
    fun buildTracks_mappedTrackInfo_checkTracksUnavailability_false() {
        val trackSelectionHelper = getTrackSelectionHelper(buildActualTrackSelector("sample_dash_no_tracks"))
        trackSelectionHelper.setTracksErrorListener(mock(TrackSelectionHelper.TracksErrorListener::class.java))
        trackSelectionHelper.prepareTracks(TrackSelectionArray(), null).let {
            assertFalse(it)
        }
    }

    fun buildTracks_warnAboutUnsupportedRendererTypes() {
        //TODO
    }

    @Test
    fun buildTracks_rawImageTracks() {
        val dashManifestString: String = TestUtils.getManifestString(ApplicationProvider.getApplicationContext() as Context, "${initialDashAssetPath}sample_dash_custom_manifest_image_tracks")
        val customDashManifest: CustomDashManifest = CustomDashManifestParser().parse(Uri.parse("http://dash.edgesuite.net/akamai/bbb_30fps/bbb_with_multiple_tiled_thumbnails.mpd"),
                dashManifestString)

        val trackSelectionHelper = getTrackSelectionHelper(buildActualTrackSelector("sample_dash_custom_manifest_without_image_tracks"))
        trackSelectionHelper.prepareTracks(TrackSelectionArray(), customDashManifest).let {
            assertTrue(it)
        }
    }

    @Test
    fun getExternalSubtitleLanguage() {
        val format = Format.Builder().setLanguage("eng-application/mp4").build();
        val trackSelectionHelper = getTrackSelectionHelper(mockedSelector)
        assertEquals("en", trackSelectionHelper.getExternalSubtitleLanguage(format))

        val isExternalSubtitleformat = format.buildUpon().setLanguage("eng-application/mp4").setSampleMimeType("application/mp4").build()
        assertEquals(true, trackSelectionHelper.isExternalSubtitle(isExternalSubtitleformat.language, isExternalSubtitleformat.sampleMimeType))

        val nullFormat = format.buildUpon().setLanguage(null).build()
        assertNull(trackSelectionHelper.getExternalSubtitleLanguage(nullFormat))
    }

    @Test(expected = StringIndexOutOfBoundsException::class)
    fun getExternalSubtitleLanguage_null_exception() {
        val format = Format.Builder().build();
        val trackSelectionHelper = getTrackSelectionHelper(mockedSelector)
        val nullExternalSubtitleformat = format.buildUpon().setLanguage("engapplication/mp4").setSampleMimeType("application/mp4").build()
        trackSelectionHelper.getExternalSubtitleLanguage(nullExternalSubtitleformat)
    }
}
