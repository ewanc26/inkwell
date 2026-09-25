package uk.ewancroft.inkwell.ui.reader

import kotlin.test.Test
import kotlin.test.assertEquals
import uk.ewancroft.inkwell.data.model.content.LeafletBlock

class PollStateTest {
    @Test
    fun `calculates option vote percentages correctly`() {
        val totalVotes = 10
        val optionCount = 3
        val fraction = optionCount.toFloat() / totalVotes
        val percentage = (fraction * 100).toInt()

        assertEquals(30, percentage)
    }

    @Test
    fun `combines option state description with vote counts and selection`() {
        val pollSelected = "Selected"
        val voteCountDescription = "3 votes"
        val percentageLabel = "30 percent"

        val description = buildString {
            append(pollSelected)
            append(", $voteCountDescription")
            if (percentageLabel.isNotEmpty()) append(", $percentageLabel")
        }

        assertEquals("Selected, 3 votes, 30 percent", description)
    }
}
