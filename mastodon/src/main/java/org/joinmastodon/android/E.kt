package org.joinmastodon.android

import com.squareup.otto.AsyncBus

class E {
    companion object {
        private val bus = AsyncBus()

        @JvmStatic
        fun post(event: Any) {
            bus.post(event)
        }

        @JvmStatic
        fun register(listener: Any) {
            bus.register(listener)
        }

        @JvmStatic
        fun unregister(listener: Any) {
            bus.unregister(listener)
        }
    }
}