package com.kaltura.playkit.player

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kaltura.android.exoplayer2.source.TrackGroupArray
import com.kaltura.android.exoplayer2.trackselection.TrackSelectionArray
import com.kaltura.playkit.*
import com.kaltura.testhelper.TestUtils
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.function.ThrowingRunnable
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.Shadows

@RunWith(AndroidJUnit4::class)
internal class ExoPlayerWrapperTest {

    private var exoPlayerView: BaseExoplayerView? = null
    private var rootPlayerView: PlayerView? = null
    private var eventListener: PlayerEngine.EventListener? = null
    private val playerSettings: PlayerSettings = PlayerSettings()

    private lateinit var realExoPlayerWrapper: ExoPlayerWrapper
    private lateinit var mockExoPlayerWrapper: ExoPlayerWrapper
    private lateinit var trackSelectionHelper: TrackSelectionHelper
    private lateinit var context: Context
    private lateinit var emptyTrackSelectionArray: TrackSelectionArray
    private lateinit var trackGroupArray: TrackGroupArray

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        exoPlayerView = mock(BaseExoplayerView::class.java)
        rootPlayerView = mock(PlayerView::class.java)
        eventListener = mock(PlayerEngine.EventListener::class.java)
        trackSelectionHelper = mock(TrackSelectionHelper::class.java)
        mockExoPlayerWrapper = mock(ExoPlayerWrapper::class.java)

        prepareClassInstances()

        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    @Throws(NullPointerException::class)
    fun onTrackChanged_TrackSelectionHelper_Exception() {
        assertThrows(NullPointerException::class.java, ThrowingRunnable { realExoPlayerWrapper.onTracksChanged(trackGroupArray, emptyTrackSelectionArray) })
    }

    @Test
    fun load_flow() {
        val pkMediaSourceConfig = getMediaSourceConfig()
        mockExoPlayerWrapper.load(pkMediaSourceConfig)
        verify(mockExoPlayerWrapper).load(pkMediaSourceConfig)
    }

    @Test
    fun onTrackChanged_TrackSelectionHelper_Working() {
        //FIXME
//        realExoPlayerWrapper.load(mock(PKMediaSourceConfig::class.java))
//        val spiedExoPlayerWrapper = spy(realExoPlayerWrapper)
//        //exoPlayerWrapper.onTracksChanged(trackGroupArray, emptyTrackSelectionArray)
//        `when`(spiedExoPlayerWrapper.onTracksChanged(trackGroupArray, emptyTrackSelectionArray)).then(Answers.RETURNS_SMART_NULLS)
        //doNothing().`when`(spiedExoPlayerWrapper).onTracksChanged(trackGroupArray, emptyTrackSelectionArray)
        //   verify(exoPlayerWrapper, times(1)).onTracksChanged(trackGroupArray, emptyTrackSelectionArray)
    }

    private fun prepareClassInstances() {
        emptyTrackSelectionArray = TrackSelectionArray()
        trackGroupArray = TestUtils.getTrackGroupArrayFromDashManifest(ApplicationProvider.getApplicationContext(),
                "testdata/mpd/sample_dash_switching_property")
        realExoPlayerWrapper = ExoPlayerWrapper(context, exoPlayerView, playerSettings, rootPlayerView)
        realExoPlayerWrapper.setEventListener(eventListener)
    }

    private fun getMediaSourceConfig(): PKMediaSourceConfig {
        val mediaSourceList: ArrayList<PKMediaSource> = arrayListOf()
        val mediaSource = PKMediaSource()
        val mediaEntry = PKMediaEntry()
        val mediaConfig = PKMediaConfig()

        mediaSource.url = "https://testMedia/playlist.m3u8"
        mediaSource.mediaFormat = null

        mediaEntry.duration = 0

        mediaSourceList.add(mediaSource)
        mediaEntry.sources = mediaSourceList

        mediaConfig.mediaEntry = mediaEntry

        return PKMediaSourceConfig(mediaConfig, mediaSource, playerSettings)
    }
}
