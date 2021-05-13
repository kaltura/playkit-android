package com.kaltura.playkit.player

import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kaltura.android.exoplayer2.source.TrackGroup
import com.kaltura.android.exoplayer2.source.TrackGroupArray
import com.kaltura.android.exoplayer2.source.dash.manifest.AdaptationSet
import com.kaltura.android.exoplayer2.source.dash.manifest.DashManifest
import com.kaltura.android.exoplayer2.trackselection.TrackSelectionArray
import com.kaltura.android.exoplayer2.upstream.BandwidthMeter
import com.kaltura.testhelper.TestUtils
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.function.ThrowingRunnable
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations.initMocks
import org.robolectric.Shadows

@RunWith(AndroidJUnit4::class)
internal class ExoPlayerWrapperTest {

    @InjectMocks
    var exoPlayerWrapper: ExoPlayerWrapper? = null
    @Mock
    var exoPlayerView: BaseExoplayerView? = null
    @Mock
    var rootPlayerView: PlayerView? = null
    @Mock
    var bandwidthMeter: BandwidthMeter? = null
    @Mock
    var trackSelectionHelper: TrackSelectionHelper? = null

    var settings: PlayerSettings? = null

    @Before
    fun setUp() {
        settings = mock(PlayerSettings::class.java, RETURNS_DEEP_STUBS)
        trackSelectionHelper = mock(TrackSelectionHelper::class.java)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        `when`(settings?.abrSettings?.initialBitrateEstimate).thenReturn(0)
        initMocks(this)
    }

    @Test
    @Throws(NullPointerException::class)
    fun onTrackChanged_TrackSelectionHelperException() {

        val dashManifest: DashManifest = TestUtils.parseLocalManifest(ApplicationProvider.getApplicationContext(),
                "testdata/mpd/sample_mpd_switching_property")
        val adaptationSets: List<AdaptationSet> = dashManifest.getPeriod(0).adaptationSets

        val emptyTrackSelectionArray = TrackSelectionArray()

        val trackGroupArray = TrackGroupArray(
                TrackGroup(
                        adaptationSets[0].representations[0].format,
                        adaptationSets[0].representations[1].format,
                        adaptationSets[2].representations[0].format,
                        adaptationSets[2].representations[1].format,
                        adaptationSets[3].representations[0].format),
                TrackGroup(adaptationSets[1].representations[0].format))

        assertThrows(NullPointerException::class.java, ThrowingRunnable { exoPlayerWrapper?.onTracksChanged(trackGroupArray, emptyTrackSelectionArray) })
    }
}