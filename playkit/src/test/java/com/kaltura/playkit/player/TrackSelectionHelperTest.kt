package com.kaltura.playkit.player

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kaltura.android.exoplayer2.C
import com.kaltura.android.exoplayer2.RendererCapabilities
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock


@RunWith(AndroidJUnit4::class)
internal class TrackSelectionHelperTest {

    private lateinit var trackSelectionHelper: TrackSelectionHelper
    private lateinit var selector: TrackSelector
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
            arrayOf<RendererCapabilities>(VIDEO_CAPABILITIES,
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

        trackGroupArray = TestUtils.manifestParsingSetup(
                ApplicationProvider.getApplicationContext(),
                "testdata/mpd/sample_mpd_clear_h264_tears")

        buildTrackSelector()

        trackSelectionHelper = TrackSelectionHelper(
                ApplicationProvider.getApplicationContext(),
                selector as DefaultTrackSelector,
                lastSelectedTrackIds)
        trackSelectionHelper.applyPlayerSettings(playerSettings)
    }

    @Test
    fun buildTracks_mappedTrackInfo() {
        assertTrue(trackSelectionHelper.prepareTracks(TrackSelectionArray(), null))
    }

    private fun buildTrackSelector() {
        val timeline = FakeTimeline()
        selector = DefaultTrackSelector(ApplicationProvider.getApplicationContext() as Context)
        selector.init(invalidationListener, bandwidthMeter)
        val trackSelectorResult: TrackSelectorResult = selector.selectTracks(RENDERER_CAPABILITIES,
                trackGroupArray,
                MediaSource.MediaPeriodId(timeline.getUidOfPeriod(0)),
                FakeTimeline())

        selector.onSelectionActivated(trackSelectorResult.info)
    }
}