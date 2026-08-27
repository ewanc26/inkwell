package uk.ewancroft.inkwell.shared.jetstream

/**
 * Android factory — creates a [JetstreamClient] backed by Ktor + OkHttp.
 */
actual fun createJetstreamClient(): JetstreamClient = JetstreamClientAndroid()
