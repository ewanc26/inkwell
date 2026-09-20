package uk.ewancroft.inkwell.shared.moderation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationContentPolicyTest {
    @Test
    fun `visible content can be included in notification metadata`() {
        assertFalse(
            NotificationContentPolicy.shouldRedact(
                FilterableContent(title = "A calm day"),
                ModerationPolicy(hiddenLabels = setOf("nsfw"))
            )
        )
    }

    @Test
    fun `warning and hidden content are both redacted`() {
        val content = FilterableContent(labels = listOf(ModerationLabel("spoiler")))
        assertTrue(NotificationContentPolicy.shouldRedact(content, ModerationPolicy(warningLabels = setOf("spoiler"))))
        assertTrue(NotificationContentPolicy.shouldRedact(content, ModerationPolicy(hiddenLabels = setOf("spoiler"))))
    }

    @Test
    fun `labels not selected by the user remain visible`() {
        val content = FilterableContent(labels = listOf(ModerationLabel("custom-label")))
        val policy = ModerationPolicy(
            warningLabels = setOf("spoiler"),
            hiddenLabels = setOf("nsfw")
        )

        assertFalse(NotificationContentPolicy.shouldRedact(content, policy))
    }

    @Test
    fun `publication and document labels are evaluated together`() {
        val publicationLabels = listOf(ModerationLabel("publication-warning"))
        val documentLabels = listOf(ModerationLabel("document-warning"))
        val policy = ModerationPolicy(hiddenLabels = setOf("publication-warning"))

        assertTrue(
            NotificationContentPolicy.shouldRedact(
                FilterableContent(labels = publicationLabels + documentLabels),
                policy
            )
        )
    }
}
