package com.kaltura.playkit

open class InterceptorEvent(type: Type?): PKEvent {
    var type: Type? = null

    init {
        this.type = type
    }

    enum class Type {
        CDN_SWITCHED,
        SOURCE_URL_SWITCHED
    }

    class CdnSwitchedEvent(eventType: Type?, val cdnCode: String?) : InterceptorEvent(eventType)

    class SourceUrlSwitched(eventType: Type?, val originalUrl: String?, val updatedUrl: String?) : InterceptorEvent(eventType)

    override fun eventType(): Enum<*>? {
        return type
    }

    companion object {
        @JvmField
        val cdnSwitched = CdnSwitchedEvent::class.java
        @JvmField
        val sourceUrlSwitched = SourceUrlSwitched::class.java
    }
}
