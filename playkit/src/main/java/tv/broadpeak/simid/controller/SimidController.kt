package tv.broadpeak.simid.controller

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.util.Log
import android.view.View
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

/**
 * Set up the SIMID controller starts listening for messages from the creative.
 * @param playerDimensions the main player dimensions
 * @param creativeDimensions the initial creative dimensions the application/player will set
 * @param creativeUri The creative URI
 * @param adParameters the creative ad parameters
 * @param adDuration the display duration of the creative (0 by default, meaning no requested duration)
 * @param adSkippable true if the linear ad is skippable (false by default)
 * @param mediaTimeupdateInterval the interval in ms to send media timeupdate message to the creative (250ms by default, -1 to disable)
 */
public open class SimidController (
    private val activity: Activity,
    private val context: Context,
    private val playerDimensions: Rect,
    private var creativeDimensions: Rect,
    private val creativeUri: String,
    private val adParameters: String = "",
    private val adDuration: Float = 0.0F,
    private val adSkippable: Boolean = false,
    private val mediaTimeupdateInterval: Long = MEDIA_TIMEUPDATE_INTERVAL_MS
) : SimidComponent(SIMID_COMPONENT_TYPE) {

    companion object {
        private const val TAG = "SimidController"
        private const val SIMID_COMPONENT_TYPE = "Player"
        public const val VERSION = "0.5.0"//BuildConfig.VERSION // TODO: hardcoded as was taken out from the BPK simid repo. Keep up-to-date and review when time comes
        public const val MEDIA_TIMEUPDATE_INTERVAL_MS = 250L
    }

    // The WebView used to load the SIMID creative
    private var webView: WebView? = null

    private var _autoStart: Boolean = true
    private var _initialized: Boolean = false

    private var _nonLinearStartTime: Float = 0.0F
    private var _isStopping: Boolean = false
    private var _timerMediaTimeupdate: Timer? = null

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

    fun onPlayMedia(cb: () -> Boolean) {
        this.onPlayMedia = cb
    }

    fun onPauseMedia(cb: () -> Boolean) {
        this.onPauseMedia = cb
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
    fun getVersion(): String {
        return VERSION
    }

    /**
     * Initialize and load ad. This should be called before an ad plays.
     * Creates an iframe with the creative in it, then uses a promise to call init on the creative as soon as the creative initializes a session.
     * @param autoStart true to start the creative once initialized
     */
    fun load(autoStart: Boolean = true) {
        _autoStart = autoStart
        createWebView()
    }

    /**
     * Start the loaded creative
     */
    fun start() {
        if (!_initialized) {
            // start() my be called before creative has been fully initialized, then start it automatically when ready
            _autoStart = true
            return
        }
        startCreative()
    }

    /**
     * Stop and reset the SIMID controller
     */
    fun reset() {
        stopAd()
    }

    /**
     * Notify the SIMID controller any changes any of ad components’ size
     * @param playerDimensions the new player dimensions
     * @param creativeDimensions the new creative dimensions
     * @param fullscreen true if in fullscreen mode
     */
    fun notifyResize(playerDimensions: Rect, creativeDimensions: Rect, fullscreen: Boolean) {
        if (!this._initialized) {
            return
        }
        val args = PlayerResizeMessageArgs(dimensions(playerDimensions), dimensions(creativeDimensions), fullscreen)
        this.sendMessage(PlayerMessage.RESIZE, args)
    }

    override fun postMessage(message: String) {
        Log.v(TAG, "[SIMID][Player][S]: $message")

        val script =
            """
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
        this.addMessageListener(CreativeMessage.EXPAND_NONLINEAR, ::onCreativeExpandNonlinear)
        this.addMessageListener(CreativeMessage.COLLAPSE_NONLINEAR, ::onCreativeCollapseNonlinear)
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
        if (!_initialized) {
            Log.w(TAG, "Session not initialized, requestPause ignored")
            return
        }
        if (onPauseMedia?.invoke() == true) resolveMessage(message) else rejectMessage(message)
    }

    private fun onCreativeRequestPlay(message: Message) {
        if (!_initialized) {
            Log.w(TAG, "Session not initialized, requestPlay ignored")
            return
        }
        if (onPlayMedia?.invoke() == true) resolveMessage(message) else rejectMessage(message)
    }

    private fun onCreativeRequestResize(message: Message) {
        if (onResizeSimid == null || onResizePlayer == null) {
            this.rejectMessage(message, PlayerErrorCode.UNSPECIFIED, "Resize not supported by the player")
            return
        }
        val args: CreativeRequestResizeMessageArgs = Gson().fromJson(message.args.toString(), CreativeRequestResizeMessageArgs::class.java)

        var dim = args.creativeDimensions
        val creativeRect = Rect(dim.x, dim.y, dim.x + dim.width, dim.y + dim.height)
        // Resize SIMID iframe
        if (onResizeSimid?.invoke(creativeRect) == false) {
            rejectMessage(message, PlayerErrorCode.UNSPECIFIED, "The player is unable to complete the Creative resizing")
            return
        }
        // Store creative dimensions (reused when collapsed)
        this.creativeDimensions = creativeRect

        // If creative successfully resized then resize the main player
        dim = args.mediaDimensions
        val playerRect = Rect(dim.x, dim.y, dim.x + dim.width, dim.y + dim.height)
        onResizePlayer?.invoke(playerRect)

        resolveMessage(message)
    }

    private fun onCreativeExpandNonlinear(message: Message) {
        if (!_initialized) {
            Log.w(TAG, "Session not initialized, expandNonlinear ignored")
            return
        }
        // Under normal circumstances, the player pauses the media.
        // In cases when the content is video, the player resizes the creative iframe to the dimensions of the video
        // and places the expanded creative at video zero coordinates.
        onPauseMedia?.invoke()
        if (onResizeSimid?.invoke(playerDimensions) == true)
            resolveMessage(message) else
                rejectMessage(message, PlayerErrorCode.UNSPECIFIED, "Unable to expand nonlinear ad")
    }

    private fun onCreativeCollapseNonlinear(message: Message) {
        if (!_initialized) {
            Log.w(TAG, "Session not initialized, collapseNonlinear ignored")
            return
        }
        // Under normal circumstances, the player pauses the media.
        // In cases when the content is video, the player resizes the creative iframe to the dimensions of the video
        // and places the expanded creative at video zero coordinates.
        onPlayMedia?.invoke()
        if (onResizeSimid?.invoke(creativeDimensions) == true)
            resolveMessage(message) else
            rejectMessage(message, PlayerErrorCode.UNSPECIFIED, "Unable to collapse nonlinear ad")
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
                    visibility = View.GONE
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

        val environmentData = EnvironmentData(
            dimensions(playerDimensions),
            dimensions(creativeDimensions),
            false,
            true,
            true,
            if (adSkippable) SkippableState.AD_HANDLES else SkippableState.NOT_SKIPPABLE,
            null,
            protocolVersion,
            null, // This is not relevant on desktop
            null, // This should be filled in for sdks and players
            null, // This should be filled in on mobile
            null, // This should be filled in on mobile
            false, // player.isDeviceMuted,
            1.0F, // player.volume,
            null, // NavigationSupport.NOT_SUPPORTED,
            null, // CloseButtonSupport.AD_HANDLES,
            adDuration
        )

        // Escape characters to avoid JSON parsing failure in Creative
        val adParams = StringEscapeUtils.escapeJava(adParameters)

        val creativeData = CreativeData(adParams,"")
        val args = PlayerInitMessageArgs(environmentData, creativeData)

        try {
            mainScope.launch {
                sendMessage(PlayerMessage.INIT, args).await()
                _initialized = true
                if (_autoStart) {
                    startCreative()
                }
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
                startMediaTimeupdateInterval()
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
        if (_isStopping || webView == null) {
            resetSession()
            return
        }
        _isStopping = true
        stopMediaTimeupdateInterval()
        onShowSimid?.invoke(false)

        completeAd(skipped)

        // Wait for the SIMID creative to acknowledge stop and then clean up the iframe.
        mainScope.launch {
            if (_initialized) {
                (when (skipped) {
                    true -> sendMessage(PlayerMessage.AD_SKIPPED)
                    false -> sendMessage(PlayerMessage.AD_STOPPED, PlayerAdStoppedMessageArgs(reason))
                }).await()
            }
            clearWebView()
            resetSession()
        }
    }

    private fun clearWebView() {
        webView?.loadUrl("about:blank")
        webView?.clearHistory()
        webView?.clearCache(true)
        webView = null
    }

    private fun completeAd(skipped: Boolean = false) {
        // Resize the main player to its original dimensions
        onResizePlayer?.invoke(playerDimensions)

        // Notify player ad is complete, if skipped this enable player to seek after the current linear ad
        onComplete?.invoke(skipped)

        // Resume main video playback
        onPlayMedia?.invoke()
    }

    //region MAIN VIDEO STATE
    private fun startMediaTimeupdateInterval() {
        stopMediaTimeupdateInterval()

        if (mediaTimeupdateInterval == -1L) {
            return
        }
        if (adDuration <= 0) {
            return
        }

        _timerMediaTimeupdate = Timer()
        _timerMediaTimeupdate?.schedule(object : TimerTask() {
            override fun run() {
                activity.runOnUiThread {
                    val mediaState = onGetMediaState?.invoke()
                    if (mediaState != null) {
                        mediaTimeUpdated(mediaState.currentTime!!)
                    }
                }
            }
        }, mediaTimeupdateInterval, mediaTimeupdateInterval)
    }

    private fun stopMediaTimeupdateInterval() {
        _timerMediaTimeupdate?.cancel()
        _timerMediaTimeupdate = null
    }

    private fun mediaTimeUpdated(currentTime: Float) {

        this.sendMessage(MediaMessage.TIME_UPDATE, MediaTimeUpdateMessageArgs(currentTime))

        // For nonlinear ads, stop the ad once requested duration is over
        if (adDuration > 0 &&
            _nonLinearStartTime > 0 &&
            currentTime - _nonLinearStartTime > adDuration) {
            _nonLinearStartTime = 0.0F
            stopAd(StopCode.NON_LINEAR_DURATION_COMPLETE)
        }
    }

    private fun dimensions(rect: Rect): Dimensions {
        return Dimensions(rect.top, rect.left, rect.width(), rect.height())
    }
    //endregion MAIN VIDEO STATE
}
