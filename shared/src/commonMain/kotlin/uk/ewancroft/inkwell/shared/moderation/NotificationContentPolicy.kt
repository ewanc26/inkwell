package uk.ewancroft.inkwell.shared.moderation

/** Whether notification metadata may be shown outside the reader warning gate. */
object NotificationContentPolicy {
    fun shouldRedact(content: FilterableContent, policy: ModerationPolicy): Boolean =
        ContentFilterEngine.evaluate(content, policy) !is ContentFilterDecision.Show
}
