package uk.ewancroft.inkwell.shared.feed

import platform.CoreFoundation.CFAbsoluteTimeGetCurrent

internal actual fun currentTimeMillis(): Long =
    ((CFAbsoluteTimeGetCurrent() + 978307200.0) * 1000).toLong()
