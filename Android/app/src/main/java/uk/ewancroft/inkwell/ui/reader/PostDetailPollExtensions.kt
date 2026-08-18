package uk.ewancroft.inkwell.ui.reader

import android.util.Log
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.ewancroft.inkwell.data.model.common.StrongRef
import uk.ewancroft.inkwell.data.repository.createPollVote
import uk.ewancroft.inkwell.data.repository.getPollDefinition
import uk.ewancroft.inkwell.data.repository.listPollVotes
import uk.ewancroft.inkwell.shared.AtUri

fun PostDetailViewModel.loadPoll(pollRef: StrongRef) {
    val pollUri = pollRef.uri
    val cached = pollDataInternal.value[pollUri]
    if (cached != null) return

    viewModelScope.launch {
        try {
            val parsed = AtUri.parse(pollUri) ?: return@launch
            val did = parsed.did
            val rkey = parsed.recordKey ?: return@launch

            val definition = runCatching {
                pdsRepository.getPollDefinition(did, rkey)
            }.getOrNull() ?: return@launch

            val votes = runCatching {
                pdsRepository.listPollVotes(did, rkey)
            }.getOrNull() ?: emptyList()

            val voteCounts = mutableMapOf<String, Int>()
            votes.forEach { vote ->
                vote.option?.forEach { option ->
                    voteCounts[option] = (voteCounts[option] ?: 0) + 1
                }
            }

            val myVote = votes.firstOrNull { it.poll.uri == pollUri }?.option

            val totalVotes = voteCounts.values.sum()
            val data = PostDetailViewModel.PollData(
                definition = definition,
                voteCounts = voteCounts,
                myVote = myVote,
                totalVotes = totalVotes,
            )

            pollDataInternal.value = pollDataInternal.value + (pollUri to data)
        } catch (e: Exception) {
            Log.w("PostDetailVM", "Failed to load poll data for $pollUri", e)
        }
    }
}

fun PostDetailViewModel.castVote(pollUri: String, options: List<String>) {
    val currentData = pollDataInternal.value[pollUri] ?: return
    viewModelScope.launch {
        try {
            val parsed = AtUri.parse(pollUri) ?: return@launch
            pdsRepository.createPollVote(parsed.did, pollUri, options)

            val newVoteCounts = currentData.voteCounts.toMutableMap()
            options.forEach { option ->
                newVoteCounts[option] = (newVoteCounts[option] ?: 0) + 1
            }

            val updated = currentData.copy(
                voteCounts = newVoteCounts,
                myVote = options,
                totalVotes = currentData.totalVotes + options.size,
            )
            pollDataInternal.value = pollDataInternal.value + (pollUri to updated)
        } catch (e: Exception) {
            Log.e("PostDetailVM", "Failed to cast vote on $pollUri", e)
        }
    }
}
