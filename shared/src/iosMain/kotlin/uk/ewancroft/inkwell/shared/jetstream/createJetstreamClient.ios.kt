package uk.ewancroft.inkwell.shared.jetstream

/**
 * iOS factory — creates a [JetstreamClient] backed by Ktor + Darwin.
 */
actual fun createJetstreamClient(): JetstreamClient = JetstreamClientIos()
