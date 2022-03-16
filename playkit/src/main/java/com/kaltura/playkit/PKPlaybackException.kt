package com.kaltura.playkit

import android.media.MediaCodec.CryptoException
import android.util.Pair
import com.kaltura.android.exoplayer2.ExoPlaybackException
import com.kaltura.android.exoplayer2.ExoTimeoutException
import com.kaltura.android.exoplayer2.PlaybackException
import com.kaltura.android.exoplayer2.mediacodec.MediaCodecRenderer.DecoderInitializationException
import com.kaltura.android.exoplayer2.mediacodec.MediaCodecUtil
import com.kaltura.playkit.player.PKPlayerErrorType

class PKPlaybackException {

    companion object {

        private val log = PKLog.get("PKPlaybackException")

        // Miscellaneous errors (1xxx).
        private const val MISC_ERROR_CODE = 1000
        // Input/Output errors (2xxx).
        private const val IO_ERROR_CODE = 2000
        // Content parsing errors (3xxx).
        private const val CONTENT_PARSING_ERROR_CODE = 3000
        // Decoding errors (4xxx).
        private const val DECODING_ERROR_CODE = 4000
        // AudioTrack errors (5xxx).
        private const val AUDIO_TRACK_ERROR_CODE = 5000
        // DRM errors (6xxx).
        private const val DRM_ERROR_CODE = 6000
        private const val CUSTOM_ERROR_CODE = 1000000

        @JvmStatic
        fun getPlaybackExceptionType(playbackException: PlaybackException): Pair<PKPlayerErrorType, String> {
            var errorStr = playbackException.errorCodeName

            val playerErrorType = when (playbackException.errorCode) {
                PlaybackException.ERROR_CODE_TIMEOUT -> {
                    PKPlayerErrorType.TIMEOUT
                }
                in (MISC_ERROR_CODE + 1) until IO_ERROR_CODE -> {
                    PKPlayerErrorType.MISCELLANEOUS
                }
                in (IO_ERROR_CODE + 1) until CONTENT_PARSING_ERROR_CODE -> {
                    PKPlayerErrorType.IO_ERROR
                }
                in (CONTENT_PARSING_ERROR_CODE + 1) until DECODING_ERROR_CODE -> {
                    PKPlayerErrorType.SOURCE_ERROR
                }
                in (DECODING_ERROR_CODE + 1) until AUDIO_TRACK_ERROR_CODE,
                in (AUDIO_TRACK_ERROR_CODE + 1) until DRM_ERROR_CODE -> {
                    PKPlayerErrorType.RENDERER_ERROR
                }
                in (DRM_ERROR_CODE + 1) until CUSTOM_ERROR_CODE -> {
                    PKPlayerErrorType.DRM_ERROR
                }
                else -> {
                    PKPlayerErrorType.UNEXPECTED
                }
            }

            getExoPlaybackException(playbackException, playerErrorType)?.let { errorMessage ->
                errorStr = "$errorMessage-$errorStr"
            }

            return Pair(playerErrorType, errorStr)
        }

        /**
         * If Playback exception is ExoPlaybackException then
         * fire the error details in the traditional way
         *
         * @param playbackException Exception on player error
         */
        private fun getExoPlaybackException(playbackException: PlaybackException, playerErrorType: PKPlayerErrorType): String? {
            if (playbackException is ExoPlaybackException) {
                var errorMessage = playbackException.message
                when (playbackException.type) {
                    ExoPlaybackException.TYPE_SOURCE -> {
                        errorMessage = getSourceErrorMessage(playbackException, errorMessage)
                    }

                    ExoPlaybackException.TYPE_RENDERER -> {
                        errorMessage = getRendererExceptionDetails(playbackException, errorMessage)
                    }

                    ExoPlaybackException.TYPE_UNEXPECTED -> {
                        errorMessage = getUnexpectedErrorMessage(playbackException, errorMessage)
                    }
                }
                val errorStr = errorMessage ?: "Player error: " + playerErrorType.name
                log.e(errorStr)
                return errorStr
            }

            return null
        }

        private fun getRendererExceptionDetails(
            error: ExoPlaybackException,
            errorMessage: String?
        ): String? {
            var message: String? = errorMessage
            when (val cause = error.rendererException) {
                is DecoderInitializationException -> {
                    // Special case for decoder initialization failures.
                    message = if (cause.codecInfo == null) {
                        when {
                            cause.cause is MediaCodecUtil.DecoderQueryException -> {
                                "Unable to query device decoders"
                            }
                            cause.secureDecoderRequired -> {
                                "This device does not provide a secure decoder for " + cause.mimeType
                            }
                            else -> {
                                "This device does not provide a decoder for " + cause.mimeType
                            }
                        }
                    } else {
                        "Unable to instantiate decoder" + cause.codecInfo?.name
                    }
                }
                is CryptoException -> {
                    message = cause.message ?: "MediaCodec.CryptoException occurred"
                    message = "DRM_ERROR:$message"
                }
                is ExoTimeoutException -> {
                    message = cause.message ?: "Exo timeout exception"
                    message = "EXO_TIMEOUT_EXCEPTION:$message"
                }
            }
            return message
        }

        private fun getUnexpectedErrorMessage(
            error: ExoPlaybackException,
            errorMessage: String?
        ): String? {
            var message: String? = errorMessage
            val cause: Exception = error.unexpectedException
            cause.cause?.let {
                message = it.message
            }
            return message
        }

        private fun getSourceErrorMessage(
            error: ExoPlaybackException,
            errorMessage: String?
        ): String? {
            var message: String? = errorMessage
            val cause: Exception = error.sourceException
            cause.cause?.let {
                message = it.message
            }
            return message
        }
    }
}
