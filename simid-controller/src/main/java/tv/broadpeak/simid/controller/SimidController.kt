package tv.broadpeak.simid.controller

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.RelativeLayout
import com.google.gson.Gson
import java.util.Timer
import java.util.TimerTask
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.apache.commons.text.StringEscapeUtils

//import tv.broadpeak.smartlib.ad.simid.GenericSimidControllerApi

const val MEDIA_STATE_POLL_INTERVAL_MS = 500L

public open class SimidController (
    private val activity: Activity,
    private val context: Context,
    private val mainPlayerDimensions: Rect,
    private val creativeUri: String,
    private val adParameters: String = "",
    private val adDuration: Float = 0.0F,
    private val adSkippable: Boolean = false
) : SimidComponent(SIMID_COMPONENT_TYPE) /*: GenericSimidControllerApi()*/ {

//    override fun getSimidControllerName(): String {
//        return "Bpk SIMID Controller"
//    }

    companion object {
        private const val TAG = "SimidController"
        private const val SIMID_COMPONENT_TYPE = "Player"
    }

    // The WebView used to load the SIMID creative
    private var webView: WebView? = null
    private var webViewContainer: ViewGroup? = null

    private var _duration: Float = 0.0F
    private var _nonLinearStartTime: Float = 0.0F
    private var _startPosition: Long = 0L
    private var _isStopping: Boolean = false
    private var _timerMediaState: Timer? = null

    private var onGetMediaState: (() -> MediaState)? = null
    private var onPlayMedia: (() -> Boolean)? = null
    private var onPauseMedia: (() -> Boolean)? = null
    private var onAddSimid: ((WebView) -> Unit)? = null
    private var onShowSimid: ((Boolean) -> Unit)? = null
    private var onResizeSimid: ((Rect) -> Boolean)? = null
    private var onResizePlayer: ((Rect) -> Unit)? = null
    private var onComplete: ((Boolean) -> Unit)? = null

    private val mainScope = MainScope()

    init {
        addCreativeMessageListeners()
    }

    //region Callbacks
    fun onGetMediaState(cb: () -> MediaState) {
        this.onGetMediaState = cb
    }

    fun onAddSimid(cb: (WebView) -> Unit) {
        this.onAddSimid = cb
    }

    fun onShowSimid(cb: (Boolean) -> Unit) {
        this.onShowSimid = cb
    }

    fun onResizeSimid(cb: (Rect) -> Boolean) {
        this.onResizeSimid = cb
    }

    fun onResizePlayer(cb: (Rect) -> Unit) {
        this.onResizePlayer = cb
    }

    fun onComplete(cb: (Boolean) -> Unit) {
        this.onComplete = cb
    }
    //endregion Callbacks

    @SuppressLint("SetJavaScriptEnabled")

    fun load() {
        createWebView()
    }

    fun reset() {
        stopAd()
    }

    override fun postMessage(message: String) {
        Log.v(TAG, "[SIMID] Send message: $message")

        val script =
            """
            console.log('[Android] postMessage', '$message')
            window.originalPostMessage('$message', '*');
            """.trimIndent()

        activity.runOnUiThread {
            webView?.evaluateJavascript(script, null)
        }
    }

    private fun addCreativeMessageListeners() {
        this.addMessageListener(ProtocolMessage.CREATE_SESSION, ::onCreateSession)
        this.addMessageListener(CreativeMessage.FATAL_ERROR, ::onCreativeFatalError)
        this.addMessageListener(CreativeMessage.GET_MEDIA_STATE, ::onCreativeGetMediaState)
        this.addMessageListener(CreativeMessage.REQUEST_PAUSE, ::onCreativeRequestPause)
        this.addMessageListener(CreativeMessage.REQUEST_PLAY, ::onCreativeRequestPlay)
        this.addMessageListener(CreativeMessage.REQUEST_RESIZE, ::onCreativeRequestResize)
        this.addMessageListener(CreativeMessage.REQUEST_SKIP, ::onCreativeRequestSkip)
        this.addMessageListener(CreativeMessage.REQUEST_STOP, ::onCreativeRequestStop)
    }

    //region CREATIVE MESSAGE HANDLERS
    private fun onCreateSession(message: Message) {
        // [3] - createSession sent by the creative (message resolved in SimidComponent::receiveMessage())
        // [4] - send Player:init message
        sendInitMessage()
    }

    private fun onCreativeFatalError(message: Message) {
        this.resolveMessage(message)
        this.stopAd(StopCode.CREATIVE_INITIATED)
    }

    private fun onCreativeGetMediaState(message: Message) {
        activity.runOnUiThread {
            val mediaState: MediaState? = onGetMediaState?.invoke()
            this.resolveMessage(message, mediaState)
        }
    }

    private fun onCreativeRequestPause(message: Message) {
        if (onPauseMedia?.invoke() == true) resolveMessage(message) else rejectMessage(message)
    }

    private fun onCreativeRequestPlay(message: Message) {
        if (onPlayMedia?.invoke() == true) resolveMessage(message) else rejectMessage(message)
    }

    private fun onCreativeRequestResize(message: Message) {
        val args: CreativeRequestResizeMessageArgs = Gson().fromJson(message.args.toString(), CreativeRequestResizeMessageArgs::class.java)

        var dim = args.creativeDimensions
        val creativeRect = Rect(dim.x, dim.y, dim.x + dim.width, dim.y + dim.height)
        // Resize SIMID iframe
        if (onResizeSimid?.invoke(creativeRect) == false) {
            rejectMessage(message)
        } else {
            // Then if successfull, resize the main player
            dim = args.mediaDimensions
            val playerRect = Rect(dim.x, dim.y, dim.x + dim.width, dim.y + dim.height)
            onResizePlayer?.invoke(playerRect)
            resolveMessage(message)
        }
    }

    private fun onCreativeRequestSkip(message: Message) {
        resolveMessage(message)
        skipAd()
    }

    protected fun onCreativeRequestStop(message: Message) {
        this.resolveMessage(message)
        stopAd(StopCode.CREATIVE_INITIATED)
    }
    //endregion CREATIVE MESSAGE HANDLERS

    //region IFRAME/WEBVIEW MANAGEMENT
    private fun createWebView() {
        activity.runOnUiThread {

//            WebView.setWebContentsDebuggingEnabled(true)

            webView = WebView(context);

            webView?.let { sWebView ->

                sWebView.apply {
                    layoutParams = RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT
                    )
                    setPadding(0, 0, 0, 0)
                    isFocusable = true
                    isFocusableInTouchMode = true
                    webViewClient = WebViewClient()
                    webChromeClient = WebChromeClient()
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.useWideViewPort = true
                }

                sWebView.setBackgroundColor(Color.TRANSPARENT);

                sWebView.addJavascriptInterface(object {
                    @android.webkit.JavascriptInterface
                    fun postMessage(message: String) {
                        receiveMessage(message)
                    }
                }, "Android")

                sWebView.webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        view?.evaluateJavascript(
                            """
                            console.log("[Android] override postMessage")
                            window.originalPostMessage = window.postMessage;
                            window.postMessage = function(message) {
                                console.log("[Creative] postMessage:", message)
                                // Send the message to the Android interface
                                Android.postMessage(message);
                            };
                            """.trimIndent(), null
                        )
                    }
                }

                sWebView.loadUrl(creativeUri)
                sWebView.requestFocus()

                onAddSimid?.invoke(sWebView)
            }
        }
    }
    //endregion IFRAME/WEBVIEW MANAGEMENT

    private fun logMessage(message: String) {
        activity.runOnUiThread {
            webView?.evaluateJavascript("console.log('$message');", null)
        }
    }

    /**********************************************************************************************
     * MESSAGE LISTENERS
     *********************************************************************************************/

    private fun sendInitMessage() {
        // [4] - send Player:init message

        // Since the creative starts as hidden it will take on the main player/video element dimensions, so tell the ad about those dimensions
        val videoDimensions = Dimensions(mainPlayerDimensions.top, mainPlayerDimensions.left, mainPlayerDimensions.width(), mainPlayerDimensions.height())

        val environmentData = EnvironmentData(
            videoDimensions,
            videoDimensions,
            false,
            true,
            true,
            if (adSkippable) SkippableState.AD_HANDLES else SkippableState.NOT_SKIPPABLE,
            null,
            version,
            null, // This is not relevant on desktop
            null, // This should be filled in for sdks and players
            null, // This should be filled in on mobile
            null, // This should be filled in on mobile
            false, // player.isDeviceMuted,
            1.0F, // player.volume,
            null, // NavigationSupport.NOT_SUPPORTED,
            null, // CloseButtonSupport.AD_HANDLES,
            null // _duration / 1000.0,
        )

        // Escape characters to avoid JSON parsing failure in Creative
        val adParams = StringEscapeUtils.escapeJava(adParameters)

        val creativeData = CreativeData(adParams,"")
        val args = PlayerInitMessageArgs(environmentData, creativeData)

        try {
            mainScope.launch {
                sendMessage(PlayerMessage.INIT, args).await()
                startCreative()
            }
        } catch (e: Exception) {
            Log.v(TAG, "Init failed: " + e.message)
            stopSession()
        }
    }

    private fun startCreative() {
        activity.runOnUiThread {
            val mediaState = onGetMediaState?.invoke()
            this._nonLinearStartTime = mediaState?.currentTime!!
        }

        try {
            mainScope.launch {
                sendMessage(PlayerMessage.START_CREATIVE).await()
                onShowSimid?.invoke(true)
                startPollingMediaState()
            }
        } catch (e: Exception) {
            Log.v(TAG, "Failed to start creative: " + e.message)
        }
    }

    private fun stopAd(reason: Int = StopCode.PLAYER_INITATED) {
        if (webView == null) {
            return
        }
        stopSession(false, reason)
    }

    private fun skipAd() {
        if (webView == null) {
            return
        }
        stopSession(true)
    }

    /**
     * Stop/reset session
     * Remove and destroy the SIMID creative iframe and resumes video playback.
     */
    private fun stopSession(skipped: Boolean = false, reason: Int = StopCode.PLAYER_INITATED) {
        if (_isStopping) {
            return
        }
        _isStopping = true
        stopPollingMediaState()
        onShowSimid?.invoke(false)

        completeAd(skipped)

        // Wait for the SIMID creative to acknowledge stop and then clean up the iframe.
        mainScope.launch {
            if (skipped) {
                sendMessage(PlayerMessage.AD_SKIPPED).await()
            } else {
                val args = PlayerAdStoppedMessageArgs(reason)
                sendMessage(PlayerMessage.AD_STOPPED, args).await()
            }
            // Delete webview since issue with clearing webview content (loadUrl("about:blank"))
            webView = null
            resetSession()
        }
    }

    private fun completeAd(skipped: Boolean = false) {
        // Resize the main player to its original dimensions
        onResizePlayer?.invoke(mainPlayerDimensions)

        // Notify player ad is complete, if skipped this enable player to seek after the current linear ad
        onComplete?.invoke(skipped)

        // Resume main video playback
        onPlayMedia?.invoke()
    }

    //region MAIN VIDEO STATE
    private fun startPollingMediaState() {
        stopPollingMediaState()

        if (adDuration <= 0) {
            return
        }

        _timerMediaState = Timer()
        _timerMediaState?.schedule(object : TimerTask() {
            override fun run() {
                activity.runOnUiThread {
                    val mediaState = onGetMediaState?.invoke()
                    if (mediaState != null) {
                        mediaTimeUpdated(mediaState.currentTime!!)
                    }
                }
            }
        }, MEDIA_STATE_POLL_INTERVAL_MS, MEDIA_STATE_POLL_INTERVAL_MS)
    }

    private fun stopPollingMediaState() {
        _timerMediaState?.cancel()
        _timerMediaState = null
    }

    private fun mediaTimeUpdated(currentTime: Float) {
        // For non-linear ads, stop the ad once requested duration is over
        if (adDuration > 0 &&
            _nonLinearStartTime > 0 &&
            currentTime - _nonLinearStartTime > adDuration) {
            _nonLinearStartTime = 0.0F
            stopAd(StopCode.NON_LINEAR_DURATION_COMPLETE)
        }
    }
    //endregion MAIN VIDEO STATE
}
