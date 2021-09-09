package com.kaltura.playkit

open class InterceptorEvent(type: Type?): PKEvent {
    var type: Type? = null

    init {
        this.type = type
    }

    enum class Type {
        CDN_SWITCHED
    }

    class CdnSwitchedEvent(eventType: Type?, val cdnCode: String?) : InterceptorEvent(eventType)

    override fun eventType(): Enum<*>? {
        return type
    }

    companion object {
        @JvmField
        val cdnCode = CdnSwitchedEvent::class.java
    }
}
