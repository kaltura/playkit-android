package com.kaltura.playkit.player.simid

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.AccelerateDecelerateInterpolator
import android.webkit.WebView
import android.widget.RelativeLayout
import com.kaltura.playkit.MessageBus
import com.kaltura.playkit.PKLog
import com.kaltura.playkit.PlayerEvent
import com.kaltura.playkit.PlayerEvent.SimidAdBeginEvent
import com.kaltura.playkit.PlayerEvent.SimidAdEndEvent
import com.kaltura.playkit.player.PlayerController
import tv.broadpeak.simid.controller.MediaState

class SimidPlayerController(private val playerActivity: Activity,
                            context: Context?,
                            private val messageBus: MessageBus
): PlayerController(context) {
    private val log: PKLog = PKLog.get("SimidPlayerController")

    private var simidController: KalturaSimidController? = null
    private var simidWebview: WebView? = null
    private var webViewContainer: ViewGroup? = null
    private val useAnimations = true
    private var adStartPosition: Long? = null
    private var adDuration: Long? = null

    init {
        initializeSimidAdListener()
    }

    override fun dispose() {
        super.dispose()
        messageBus.removeListeners(this)
    }

    private fun initializeSimidAdListener() {
        messageBus.addListener(
            this, PlayerEvent.simidAdBegin
        ) { event: SimidAdBeginEvent ->
            log.d("PlayerEvent.simidAdBegin")
            loadSimid(
                event.controller,
                event.creativeUri,
                event.adParameters,
                event.adStartPosition,
                event.adDuration
            )
        }
        messageBus.addListener(
            this, PlayerEvent.simidAdEnd
        ) {
            log.d("PlayerEvent.simidAdEnd")
            if (simidController != null) {
                simidController!!.reset()
                simidController = null
            }
        }
    }

    private fun loadSimid(
        controller: SimidControllerProxy,
        creativeUri: String,
        adParameters: String,
        adStartPosition: Long,
        adDuration: Long
    ) {
        Handler(Looper.getMainLooper()).post {
            if (rootPlayerView == null) {
                return@post
            }
            val duration = adDuration.toFloat() / 1000.0f
            val playerRect = Rect(
                rootPlayerView.left,
                rootPlayerView.top,
                rootPlayerView.width,
                rootPlayerView.height
            )
            this.adStartPosition = adStartPosition
            this.adDuration = adDuration
            log.d("Load SIMID: " + playerRect.toShortString() + " " + creativeUri + " " + duration)
            simidController = KalturaSimidController(
                playerActivity,
                context,
                playerRect,
                creativeUri,
                adParameters,
                duration,
                false
            )
            simidController?.let { simidController ->
                simidController.onGetMediaState { this.getMediaState() }
                simidController.onAddSimid { webview: WebView -> this.addSimidWebview(webview) }
                simidController.onShowSimid { show: Boolean -> this.showSimidWebView(show) }
                simidController.onResizeSimid { dimensions: Rect -> this.resizeSimid(dimensions) }
                simidController.onResizePlayer { dimensions: Rect -> this.resizePlayer(dimensions) }
                simidController.onComplete { skipped: Boolean -> this.completeAd(skipped) }
                simidController.setController(controller)
                simidController.load()
            }
        }
    }

    private fun getMediaState(): MediaState {
        return MediaState(
            "",
            player.currentPosition.toFloat() / 1000.0f,
            player.duration.toFloat() / 1000.0f,
            false,
            player.isDeviceMuted,
            !player.isPlaying,
            player.volume,
            true
        )
    }

    private fun addSimidWebview(webview: WebView) {
        this.simidWebview = webview
    }

    private fun showSimidWebView(show: Boolean) {
        if (simidWebview == null) {
            return
        }

        Handler(Looper.getMainLooper()).post {
            if (show) {
                simidWebview!!.visibility = WebView.VISIBLE

                webViewContainer = RelativeLayout(context)
                webViewContainer?.apply {
                    layoutParams = RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT
                    )
                    setPadding(0, 0, 0, 0)
                }
                webViewContainer?.addView(simidWebview)
                rootPlayerView.addView(webViewContainer)
            } else if (webViewContainer != null) {
                simidWebview?.loadUrl("about:blank")
                simidWebview?.clearHistory()
                simidWebview?.clearCache(true)
                webViewContainer?.removeView(simidWebview)
                rootPlayerView.removeView(webViewContainer)
                webViewContainer = null
            }
        }
    }

    private fun resizeSimid(dimensions: Rect): Boolean {
        if (simidWebview == null) {
            return false
        }
        log.d("Resize SIMID: " + dimensions.toShortString())

        // Check if requested SIMID dimensions is not outside original player dimensions
        val playerRect = Rect(
            rootPlayerView.left,
            rootPlayerView.top,
            rootPlayerView.width,
            rootPlayerView.height
        )
        val widthFits = dimensions.top + dimensions.width() <= playerRect.width()
        val heightFits = dimensions.left + dimensions.height() <= playerRect.height()
        if (!widthFits || !heightFits) {
            return false
        }

        Handler(Looper.getMainLooper()).post {
            webViewContainer?.top = dimensions.top
            webViewContainer?.left = dimensions.left
            webViewContainer?.layoutParams?.width = dimensions.width()
            webViewContainer?.layoutParams?.height = dimensions.height()
            webViewContainer?.requestLayout()
        }

        return true
    }

    private fun resizePlayer(dimensions: Rect): Boolean {
        if (player.view == null) {
            return false
        }

        Handler(Looper.getMainLooper()).post {
            val currentLayoutParams = player.view.layoutParams as MarginLayoutParams
            if (!useAnimations) {
                currentLayoutParams.leftMargin = dimensions.left
                currentLayoutParams.topMargin = dimensions.top
                currentLayoutParams.width = dimensions.width()
                currentLayoutParams.height = dimensions.height()
                player.view.layoutParams = currentLayoutParams
                player.view.requestLayout()
            } else {
                val currentLeft = currentLayoutParams.leftMargin
                val currentTop = currentLayoutParams.topMargin
                val currentWidth = currentLayoutParams.width
                val currentHeight = currentLayoutParams.height

                val duration = 300L // Animation duration in milliseconds

                val leftAnimator = ValueAnimator.ofInt(currentLeft, dimensions.left)
                val topAnimator = ValueAnimator.ofInt(currentTop, dimensions.top)
                val widthAnimator = ValueAnimator.ofInt(currentWidth, dimensions.width())
                val heightAnimator = ValueAnimator.ofInt(currentHeight, dimensions.height())

                leftAnimator.addUpdateListener { animation: ValueAnimator ->
                    val value = animation.animatedValue as Int
                    val layoutParams = player.view.layoutParams as MarginLayoutParams
                    layoutParams.leftMargin = value
                    player.view.layoutParams = layoutParams
                    player.view.requestLayout()
                }
                leftAnimator.interpolator = AccelerateDecelerateInterpolator()
                leftAnimator.setDuration(duration)
                leftAnimator.start()

                topAnimator.addUpdateListener { animation: ValueAnimator ->
                    val value = animation.animatedValue as Int
                    val layoutParams = player.view.layoutParams as MarginLayoutParams
                    layoutParams.topMargin = value
                    player.view.layoutParams = layoutParams
                    player.view.requestLayout()
                }
                topAnimator.interpolator = AccelerateDecelerateInterpolator()
                topAnimator.setDuration(duration)
                topAnimator.start()

                widthAnimator.addUpdateListener { animation: ValueAnimator ->
                    val value = animation.animatedValue as Int
                    val layoutParams = player.view.layoutParams as MarginLayoutParams
                    layoutParams.width = value
                    player.view.layoutParams = layoutParams
                    player.view.requestLayout()
                }
                widthAnimator.interpolator = AccelerateDecelerateInterpolator()
                widthAnimator.setDuration(duration)
                widthAnimator.start()

                heightAnimator.addUpdateListener { animation: ValueAnimator ->
                    val value = animation.animatedValue as Int
                    val layoutParams = player.view.layoutParams as MarginLayoutParams
                    layoutParams.height = value
                    player.view.layoutParams = layoutParams
                    player.view.requestLayout()
                }
                heightAnimator.interpolator = AccelerateDecelerateInterpolator()
                heightAnimator.setDuration(duration)
                heightAnimator.start()
            }
        }

        return true
    }

    private fun completeAd(skipped: Boolean) {
        log.d("Complete ad, skipped: $skipped")
        if (skipped && simidController != null) {
            skipCurrentAd()
        }
        simidController = null
    }

    private fun skipCurrentAd() {
        player.seekTo(adStartPosition!! + adDuration!!)
    }
}