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

        private val log = PKLog.get("PKExceptionExtractor")

        // Miscellaneous errors (1xxx).
        private val miscErrorCode = 1000
        // Input/Output errors (2xxx).
        private val ioErrorCode = 2000
        // Content parsing errors (3xxx).
        private val contentParsingErrorCode = 3000
        // Decoding errors (4xxx).
        private val decodingErrorCode = 4000
        // AudioTrack errors (5xxx).
        private val audioTrackErrorCode = 5000
        // DRM errors (6xxx).
        private val drmErrorCode = 6000
        private val customErrorCode = 1000000

        @JvmStatic
        fun getPlaybackExceptionType(playbackException: PlaybackException): Pair<PKPlayerErrorType, String> {
            var errorStr = playbackException.errorCodeName

            var playerErrorType = when (playbackException.errorCode) {
                PlaybackException.ERROR_CODE_TIMEOUT -> {
                    PKPlayerErrorType.TIMEOUT
                }
                in (miscErrorCode + 1) until ioErrorCode -> {
                    PKPlayerErrorType.MISCELLANEOUS
                }
                in 0 until contentParsingErrorCode -> {
                    PKPlayerErrorType.SOURCE_ERROR
                }
                in (contentParsingErrorCode + 1) until decodingErrorCode -> {
                    PKPlayerErrorType.SOURCE_ERROR
                }
                in (decodingErrorCode + 1) until audioTrackErrorCode -> {
                    PKPlayerErrorType.RENDERER_ERROR
                }
                in (audioTrackErrorCode + 1) until drmErrorCode -> {
                    PKPlayerErrorType.RENDERER_ERROR
                }
                in (drmErrorCode + 1) until customErrorCode -> {
                    PKPlayerErrorType.DRM_ERROR
                }
                else -> {
                    PKPlayerErrorType.UNEXPECTED
                }
            }

            getExoPlaybackException(playbackException, playerErrorType)?.let { exceptionPair ->
                playerErrorType = exceptionPair.first
                errorStr = exceptionPair.second
            }

            return Pair(playerErrorType, errorStr)
        }

        /**
         * If Playback exception is ExoPlaybackException then
         * fire the error details in the traditional way
         *
         * @param playbackException Exception on player error
         */
        private fun getExoPlaybackException(playbackException: PlaybackException, playerErrorType: PKPlayerErrorType): Pair<PKPlayerErrorType, String>? {
            var errorType = playerErrorType

            if (playbackException is ExoPlaybackException) {
                var errorMessage = playbackException.message
                when (playbackException.type) {
                    ExoPlaybackException.TYPE_SOURCE -> {
                        errorType = PKPlayerErrorType.SOURCE_ERROR
                        errorMessage = getSourceErrorMessage(playbackException, errorMessage)
                    }

                    ExoPlaybackException.TYPE_RENDERER -> {
                        errorMessage = getRendererExceptionDetails(playbackException, errorMessage)
                        errorType = PKPlayerErrorType.RENDERER_ERROR
                        errorMessage?.let {
                            if (it.startsWith("DRM_ERROR:")) {
                                errorType = PKPlayerErrorType.DRM_ERROR
                            } else if (it.startsWith("EXO_TIMEOUT_EXCEPTION:")) {
                                errorType = PKPlayerErrorType.TIMEOUT
                            }
                        }
                    }

                    ExoPlaybackException.TYPE_REMOTE -> errorType =
                        PKPlayerErrorType.REMOTE_COMPONENT_ERROR

                    ExoPlaybackException.TYPE_UNEXPECTED -> {
                        if (playbackException.errorCode == PlaybackException.ERROR_CODE_TIMEOUT) {
                            errorType = PKPlayerErrorType.TIMEOUT
                        } else {
                            errorType = PKPlayerErrorType.UNEXPECTED
                        }
                        errorMessage = getUnexpectedErrorMessage(playbackException, errorMessage)
                    }
                }
                val errorStr = errorMessage ?: "Player error: " + errorType.name
                log.e(errorStr)
                return Pair(errorType, errorStr)
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