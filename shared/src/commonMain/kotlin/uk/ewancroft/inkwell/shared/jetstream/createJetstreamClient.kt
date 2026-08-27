package uk.ewancroft.inkwell.shared.jetstream

/**
 * Creates a platform-specific [JetstreamClient] instance.
 *
 * - **Android**: uses Ktor + OkHttp.
 * - **iOS**: uses Ktor + Darwin.
 */
expect fun createJetstreamClient(): JetstreamClient
