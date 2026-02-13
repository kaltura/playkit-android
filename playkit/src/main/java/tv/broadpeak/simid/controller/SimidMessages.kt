package tv.broadpeak.simid.controller

typealias MessageCallback = (message: Message) -> Unit

object ProtocolMessage {
    const val CREATE_SESSION = "createSession"
    const val RESOLVE = "resolve"
    const val REJECT = "reject"
}

const val SIMID_NS = "SIMID:"

object MediaMessage {
    const val DURATION_CHANGE = "SIMID:Media:durationchange"
    const val ENDED = "SIMID:Media:ended"
    const val ERROR = "SIMID:Media:error"
    const val PAUSE = "SIMID:Media:pause"
    const val PLAY = "SIMID:Media:play"
    const val PLAYING = "SIMID:Media:playing"
    const val SEEKED = "SIMID:Media:seeked"
    const val SEEKING = "SIMID:Media:seeking"
    const val STALLED = "SIMID:Media:stalled"
    const val TIME_UPDATE = "SIMID:Media:timeupdate"
    const val VOLUME_CHANGE = "SIMID:Media:volumechange"
}

object PlayerMessage {
    const val AD_SKIPPED = "SIMID:Player:adSkipped"
    const val AD_STOPPED = "SIMID:Player:adStopped"
    const val FATAL_ERROR = "SIMID:Player:fatalError"
    const val INIT = "SIMID:Player:init"
    const val LOG = "SIMID:Player:log"
    const val RESIZE = "SIMID:Player:resize"
    const val START_CREATIVE = "SIMID:Player:startCreative"
}

object VideoEvent {
    const val DURATION_CHANGE = "durationchange"
    const val ENDED = "ended"
    const val ERROR = "error"
    const val PAUSE = "pause"
    const val PLAY = "play"
    const val PLAYING = "playing"
    const val SEEKED = "seeked"
    const val SEEKING = "seeking"
    const val STALLED = "stalled"
    const val TIME_UPDATE = "timeupdate"
    const val VOLUME_CHANGE = "volumechange"
}

/** Messages from the creative */
object CreativeMessage {
    const val CLICK_THRU = "SIMID:Creative:clickThru"
    const val EXPAND_NONLINEAR = "SIMID:Creative:expandNonlinear"
    const val COLLAPSE_NONLINEAR = "SIMID:Creative:collapseNonlinear"
    const val FATAL_ERROR = "SIMID:Creative:fatalError"
    const val GET_MEDIA_STATE = "SIMID:Creative:getMediaState"
    const val LOG = "SIMID:Creative:log"
    const val READY = "SIMID:Creative:Ready"
    const val REPORT_TRACKING = "SIMID:Creative:reportTracking"
    const val REQUEST_FULL_SCREEN = "SIMID:Creative:requestFullScreen"
    const val REQUEST_SKIP = "SIMID:Creative:requestSkip"
    const val REQUEST_STOP = "SIMID:Creative:requestStop"
    const val REQUEST_PAUSE = "SIMID:Creative:requestPause"
    const val REQUEST_PLAY = "SIMID:Creative:requestPlay"
    const val REQUEST_RESIZE = "SIMID:Creative:requestResize"
    const val REQUEST_VOLUME = "SIMID:Creative:requestVolume"
    const val REQUEST_TRACKING = "SIMID:Creative:reportTracking"
    const val REQUEST_CHANGE_AD_DURATION = "SIMID:Creative:requestChangeAdDuration"
    const val REQUEST_VIDEO_LOCATION = "SIMID:Creative:requestVideoLocation"
}

/**
 * These messages require a response (either resolve or reject).
 * All other messages do not require a response and are information only.
 */
val MessagesWithResponse = listOf(
    CreativeMessage.CLICK_THRU,
    CreativeMessage.GET_MEDIA_STATE,
    CreativeMessage.READY,
    CreativeMessage.REPORT_TRACKING,
    CreativeMessage.REQUEST_CHANGE_AD_DURATION,
    CreativeMessage.REQUEST_FULL_SCREEN,
    CreativeMessage.REQUEST_PAUSE,
    CreativeMessage.REQUEST_PLAY,
    CreativeMessage.REQUEST_RESIZE,
    CreativeMessage.REQUEST_SKIP,
    CreativeMessage.REQUEST_STOP,
    CreativeMessage.REQUEST_VIDEO_LOCATION,
    CreativeMessage.REQUEST_VOLUME,
    PlayerMessage.AD_SKIPPED,
    PlayerMessage.AD_STOPPED,
    PlayerMessage.FATAL_ERROR,
    PlayerMessage.INIT,
    PlayerMessage.START_CREATIVE,
    ProtocolMessage.CREATE_SESSION
)

// A list of errors the creative might send to the player.
object CreativeErrorCode {
    const val UNSPECIFIED: Long = 1100
    const val CANNOT_LOAD_RESOURCE: Long = 1101
    const val PLAYBACK_AREA_UNUSABLE: Long = 1102
    const val INCORRECT_VERSION: Long = 1103
    const val TECHNICAL_ERROR: Long = 1104
    const val EXPAND_NOT_POSSIBLE: Long = 1105
    const val PAUSE_NOT_HONORED: Long = 1106
    const val PLAYMODE_NOT_ADEQUATE: Long = 1107
    const val CREATIVE_INTERNAL_ERROR: Long = 1108
    const val DEVICE_NOT_SUPPORTED: Long = 1109
    const val MESSAGES_NOT_FOLLOWING_SPEC: Long = 1110
    const val PLAYER_RESPONSE_TIMEOUT: Long = 1111
}

// A list of errors the player might send to the creative.
object PlayerErrorCode {
    const val UNSPECIFIED: Long = 1200
    const val WRONG_VERSION: Long = 1201
    const val UNSUPPORTED_TIME: Long = 1202
    const val UNSUPPORTED_FUNCTIONALITY_REQUEST: Long = 1203
    const val UNSUPPORTED_ACTIONS: Long = 1204
    const val POSTMESSAGE_CHANNEL_OVERLOADED: Long = 1205
    const val VIDEO_COULD_NOT_LOAD: Long = 1206
    const val VIDEO_TIME_OUT: Long = 1207
    const val RESPONSE_TIMEOUT: Long = 1208
    const val MEDIA_NOT_SUPPORTED: Long = 1209
    const val SPEC_NOT_FOLLOWED_ON_INIT: Long = 1210
    const val SPEC_NOT_FOLLOWED_ON_MESSAGES: Long = 1211
}

// A list of reasons a player could stop the ad.
object StopCode {
    const val UNSPECIFIED = 0
    const val USER_INITIATED = 1
    const val MEDIA_PLAYBACK_COMPLETE = 2
    const val PLAYER_INITATED = 3
    const val CREATIVE_INITIATED = 4
    const val NON_LINEAR_DURATION_COMPLETE = 5
}

object SkippableState {
    const val PLAYER_HANDLES = "playerHandles"
    const val AD_HANDLES = "adHandles"
    const val NOT_SKIPPABLE = "notSkippable"
}

object NavigationSupport {
    const val AD_HANDLES = "adHandles"
    const val PLAYER_HANDLES = "playerHandles"
    const val NOT_SUPPORTED = "notSupported"
}

object CloseButtonSupport {
    const val AD_HANDLES = "adHandles"
    const val PLAYER_HANDLES = "playerHandles"
}

// Data structures
class Dimensions(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

class MediaState(
    val currentSrc: String?,
    val currentTime: Float?,
    val duration: Float?,
    val ended: Boolean?,
    val muted: Boolean?,
    val paused: Boolean?,
    val volume: Float?,
    val fullscreen: Boolean?
)

data class Message(
    val type: String,
    val sessionId: String,
    val messageId: Int,
    val timestamp: Long,
    val args: kotlin.Any?
)

data class ResolveMessageArgs(
    val messageId: Int,
    val value: kotlin.Any?
)

data class RejectMessageValue(
    val errorCode: Long,
    val message: String
)

data class RejectMessageArgs(
    val messageId: Int,
    val value: RejectMessageValue
)

data class DurationMessageArgs(
    val duration: Int
)

data class ErrorMessageArgs(
    val errorCode: Int,
    val errorMessage: String
)

data class LogMessageArgs(
    val message: String
)

data class VolumeChangeMessageArgs(
    val volume: Int,
    val muted: Boolean
)

// Media message args

data class MediaDurationChangeMessageArgs(
    val duration: Float
)

data class MediaErrorMessageArgs(
    val error: Int,
    val message: String
)

data class MediaTimeUpdateMessageArgs(
    val currentTime: Float
)

data class MediaVolumeChangeMessageArgs(
    val volume: Int,
    val muted: Boolean
)

// Player message args
data class PlayerAdStoppedMessageArgs(
    val code: Int
)

data class PlayerFatalErrorMessageArgs(
    val errorCode: Int,
    val errorMessage: String
)

data class CreativeData(
    val adParameters: String,
    val clickThruUrl: String
)

data class EnvironmentData(
    val videoDimensions: Dimensions,
    val creativeDimensions: Dimensions,
    val fullscreen: Boolean,
    val fullscreenAllowed: Boolean,
    val variableDurationAllowed: Boolean,
    val skippableState: String,
    val skipoffset: String?,
    val version: String,
    val siteUrl: String?,
    val appId: String?,
    val useragent: String?,
    val deviceId: String?,
    val muted: Boolean?,
    val volume: Float?,
    val navigationSupport: String?,
    val closeButtonSupport: String?,
    val nonlinearDuration: Float?
)

data class PlayerInitMessageArgs(
    val environmentData: EnvironmentData,
    val creativeData: CreativeData
)

data class PlayerLogMessageArgs(
    val message: String
)

data class PlayerResizeMessageArgs(
    val videoDimensions: Dimensions,
    val creativeDimensions: Dimensions,
    val fullscreen: Boolean
)

// Creative message args
data class CreativeClickThruMessageArgs(
    val x: Int?,
    val y: Int?,
    val playerHandles: Boolean?,
    val url: String?
)

data class CreativeExpandNonLinearResolveMessageArgs(
    val creativeDimensions: Dimensions
)

data class CreativeFatalErrorMessageArgs(
    val errorCode: Int,
    val errorMessage: String
)

data class CreativeGetMediaStateMessageArgs(
    val mediaState: MediaState
)

data class CreativeLogMessageArgs(
    val message: String
)

data class CreativeReportTrackingMessageArgs(
    val urls: List<String>
)

data class CreativeRequestChangeAdDurationMessageArgs(
    val duration: Float
)

data class CreativeRequestChangeVolumeMessageArgs(
    val volume: Float,
    val muted: Boolean
)

data class CreativeRequestNavigationMessageArgs(
    val uri: String
)

data class CreativeRequestResizeMessageArgs(
    val mediaDimensions: Dimensions,
    val creativeDimensions: Dimensions
)
