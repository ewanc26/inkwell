package uk.ewancroft.inkwell.shared.moderation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ContentFilterEngineTest {
    @Test
    fun `hidden labels hide content regardless of capitalization`() {
        val decision = ContentFilterEngine.evaluate(
            FilterableContent(labels = listOf(ModerationLabel("Graphic-Media"))),
            ModerationPolicy(hiddenLabels = setOf("graphic-media")),
        )

        assertEquals(ContentFilterDecision.Hide(listOf(FilterMatch(FilterMatchKind.Label, "graphic-media"))), decision)
    }

    @Test
    fun `disabled labelers do not affect a decision`() {
        val decision = ContentFilterEngine.evaluate(
            FilterableContent(labels = listOf(ModerationLabel("spoiler", "did:example:labeler"))),
            ModerationPolicy(warningLabels = setOf("spoiler"), disabledLabelers = setOf("did:example:labeler")),
        )

        assertEquals(ContentFilterDecision.Show, decision)
    }

    @Test
    fun `keywords hide matching content case insensitively`() {
        val decision = ContentFilterEngine.evaluate(
            FilterableContent(title = "A NIGHT IN THE WOODS"),
            ModerationPolicy(hiddenKeywords = setOf("night")),
        )

        val hidden = assertIs<ContentFilterDecision.Hide>(decision)
        assertEquals(FilterMatch(FilterMatchKind.Keyword, "night"), hidden.matches.single())
    }

    @Test
    fun `hide takes precedence over a warning`() {
        val decision = ContentFilterEngine.evaluate(
            FilterableContent(labels = listOf(ModerationLabel("spoiler"), ModerationLabel("gore"))),
            ModerationPolicy(hiddenLabels = setOf("gore"), warningLabels = setOf("spoiler")),
        )

        assertEquals(ContentFilterDecision.Hide(listOf(FilterMatch(FilterMatchKind.Label, "gore"))), decision)
    }
}
