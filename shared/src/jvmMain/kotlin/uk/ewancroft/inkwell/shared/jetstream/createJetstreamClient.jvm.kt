package uk.ewancroft.inkwell.shared.jetstream

actual fun createJetstreamClient(): JetstreamClient = JetstreamClientJvm()
