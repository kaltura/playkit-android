package tv.broadpeak.simid.controller

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import java.util.Calendar

abstract class SimidComponent (
    // The protocol actor type ('Player' or 'Creative')
    protected val type: String
) {
    companion object {
        private const val TAG = "SimidComponent"
    }

    // The SIMID protocol supported version
    protected val version: String = "1.1"

    // The session ID
    protected var sessionId: String = ""

    // The next message ID to use when sending a message
    var nextMessageId: Int = 1

    // Sent messages response listeners
    val messageListeners: MutableMap<String, ArrayList<MessageCallback>> = mutableMapOf()

    // Response listeners for sent messages
    val responseListeners: MutableMap<Int, MessageCallback> = mutableMapOf()

    protected fun addMessageListener(messageType: String, callback: MessageCallback) {
        if (!messageListeners.contains(messageType)) {
            messageListeners[messageType] = ArrayList<MessageCallback>()
        }
        messageListeners[messageType]?.add(callback)
    }

    protected fun sendMessage(type: String, args: kotlin.Any? = null): Deferred<Message?> {
        val message: Message = createMessage(type, args)
        return sendSimidMessage(message)
    }

    protected abstract fun postMessage(message: String)

    protected open fun receiveMessage(messageStr: String) {
        Log.v(SimidComponent.TAG, "[SIMID][$type] Receive Message: $messageStr")

        val message: Message = Gson().fromJson(messageStr, Message::class.java);

        // A sessionId is valid in one of two cases:
        // 1. It is not set and the message type is createSession.
        // 2. The session ids match exactly.
        val isCreatingSession: Boolean = sessionId == "" && message.type == ProtocolMessage.CREATE_SESSION
        val isSessionIdMatch: Boolean = sessionId == message.sessionId
        val validSessionId: Boolean = isCreatingSession || isSessionIdMatch

        if (!validSessionId) {
            // Ignore invalid messages.
            return
        }

        // There are 2 types of messages to handle:
        // 1. Protocol messages (like resolve, reject and createSession)
        // 2. Messages starting with SIMID:
        // All other messages are ignored.
        when (message.type) {
            ProtocolMessage.CREATE_SESSION -> {
                sessionId = message.sessionId
                resolveMessage(message)
                invokeMessageListeners(message)
            }
            ProtocolMessage.RESOLVE -> invokeResponseListener(message)
            ProtocolMessage.REJECT -> invokeResponseListener(message)
            else -> {
                if (message.type.startsWith(SIMID_NS)) {
                    invokeMessageListeners(message)
                }
            }
        }
    }

    protected fun resolveMessage(incomingMessage: Message, outgoingArgs: Any? = null) {
        val args = ResolveMessageArgs(incomingMessage.messageId, outgoingArgs)
        val message = createMessage(ProtocolMessage.RESOLVE, args)
        postMessage(message)
    }

    protected fun rejectMessage(incomingMessage: Message, errorCode: Long = PlayerErrorCode.UNSPECIFIED, errorMessage: String = "") {
        val value = RejectMessageValue(errorCode, errorMessage)
        val args = RejectMessageArgs(incomingMessage.messageId, value)
        val message = createMessage(ProtocolMessage.REJECT, args)
        postMessage(message)
    }

    protected fun resetSession() {
        messageListeners.clear()
        sessionId = ""
        nextMessageId = 1
        // TODO: Perhaps we should reject all associated promises.
        responseListeners.clear()
    }

    //region PRIVATE METHODS
    private fun createMessage(type: String, args: kotlin.Any?): Message {

        // Incrementing between messages keeps each message id unique.
        val messageId: Int = nextMessageId++

        // Only create session does not need to be in the SIMID name space because it is part of the protocol.
        val nameSpacedMessage: String = if (type == ProtocolMessage.CREATE_SESSION) type else (SIMID_NS + type)

        val message: Message = Message(
            nameSpacedMessage,
            sessionId,
            messageId,
            Calendar.getInstance().time.time,
            args
        )

        return message
    }

    private fun sendSimidMessage(message: Message?): Deferred<Message?> {

        val deferred = CompletableDeferred<Message?>()

        if (message == null) {
            deferred.complete(null)
            return deferred
        }

        if (MessagesWithResponse.contains(message.type)) {
            // If the message requires a callback this code will set
            // up a promise that will call resolve or reject with its parameters.
            addResponseListener(message.messageId, deferred)
        } else {
            // A default promise will just resolve immediately.
            // It is assumed no one would listen to these promises, but if they do it will "just work".
            deferred.complete(null)
        }

        postMessage(message)

        return deferred
    }

    private fun postMessage(message: Message) {
        val messageStr = Gson().toJson(message)
        postMessage(messageStr)
    }

    private fun addResponseListener(messageId: Int, deferred: CompletableDeferred<Message?>) {
        val listener: MessageCallback = fun (response: Message) {
            if (response.type == ProtocolMessage.RESOLVE) {
                deferred.complete(response)
            } else if (response.type == ProtocolMessage.REJECT) {
                val rejectMessageArgs: RejectMessageArgs = Gson().fromJson(response.args.toString(), RejectMessageArgs::class.java)
                val exception: Exception = Exception(rejectMessageArgs.value.message)
                deferred.completeExceptionally(exception)
            }
        }
        responseListeners[messageId] = listener
    }

    private fun invokeResponseListener(message: Message) {
        val args: ResolveMessageArgs = Gson().fromJson(message.args.toString(), ResolveMessageArgs::class.java)
        val correlatingId = args.messageId
        responseListeners[correlatingId]?.invoke(message)
        responseListeners.remove(correlatingId)
    }

    private fun invokeMessageListeners(message: Message) {
        val type = message.type.replace(SIMID_NS, "")
        messageListeners[type]?.forEach { listener -> listener(message) }
    }

    //endregion PRIVATE METHODS
}