package com.kaltura.playkit.player

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kaltura.android.exoplayer2.source.TrackGroupArray
import com.kaltura.android.exoplayer2.trackselection.TrackSelectionArray
import com.kaltura.playkit.PKRequestConfiguration
import com.kaltura.playkit.PKWakeMode
import com.kaltura.testhelper.TestUtils
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.function.ThrowingRunnable
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.Mockito.*
import org.robolectric.Shadows

@RunWith(AndroidJUnit4::class)
internal class ExoPlayerWrapperTest {

    private var exoPlayerView: BaseExoplayerView? = null
    private var rootPlayerView: PlayerView? = null
    private var eventListener: PlayerEngine.EventListener? = null

    private lateinit var exoPlayerWrapper: ExoPlayerWrapper
    private lateinit var trackSelectionHelper: TrackSelectionHelper
    private lateinit var context: Context
    private lateinit var playerSettings: PlayerSettings
    private lateinit var emptyTrackSelectionArray: TrackSelectionArray
    private lateinit var trackGroupArray: TrackGroupArray

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        playerSettings = mock(PlayerSettings::class.java, RETURNS_DEEP_STUBS)
        exoPlayerView = mock(BaseExoplayerView::class.java)
        rootPlayerView = mock(PlayerView::class.java)
        eventListener = mock(PlayerEngine.EventListener::class.java)
        trackSelectionHelper = mock(TrackSelectionHelper::class.java)

        `when`(playerSettings.abrSettings?.initialBitrateEstimate).thenReturn(0)
        `when`(playerSettings.pkRequestConfiguration).thenReturn(PKRequestConfiguration())
        `when`(playerSettings.wakeMode).thenReturn(PKWakeMode.NONE)
        `when`(playerSettings.aspectRatioResizeMode).thenReturn(PKAspectRatioResizeMode.fit)

        prepareClassInstances()

        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    @Throws(NullPointerException::class)
    fun onTrackChanged_TrackSelectionHelperException() {
        assertThrows(NullPointerException::class.java, ThrowingRunnable { exoPlayerWrapper.onTracksChanged(trackGroupArray, emptyTrackSelectionArray) })
    }

    @Test
    fun onTrackChanged_TrackSelectionHelper_Working() {
        exoPlayerWrapper.load(mock(PKMediaSourceConfig::class.java))
        val spiedExoPlayerWrapper = spy(exoPlayerWrapper)
        //exoPlayerWrapper.onTracksChanged(trackGroupArray, emptyTrackSelectionArray)
        `when`(spiedExoPlayerWrapper.onTracksChanged(trackGroupArray, emptyTrackSelectionArray)).then(Answers.RETURNS_SMART_NULLS)
        //doNothing().`when`(spiedExoPlayerWrapper).onTracksChanged(trackGroupArray, emptyTrackSelectionArray)
        //   verify(exoPlayerWrapper, times(1)).onTracksChanged(trackGroupArray, emptyTrackSelectionArray)
    }

    private fun prepareClassInstances() {
        emptyTrackSelectionArray = TrackSelectionArray()
        trackGroupArray = TestUtils.manifestParsingSetup(ApplicationProvider.getApplicationContext(),
                "testdata/mpd/sample_mpd_switching_property")
        exoPlayerWrapper = ExoPlayerWrapper(context, exoPlayerView, playerSettings, rootPlayerView)
        exoPlayerWrapper.setEventListener(eventListener)
    }
}
