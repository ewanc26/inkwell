package uk.ewancroft.inkwell.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uk.ewancroft.inkwell.data.remote.InkwellNotificationManager
import javax.inject.Inject

@HiltViewModel
class InkwellNotificationViewModel @Inject constructor(
    private val notificationManager: InkwellNotificationManager,
) : ViewModel() {

    private val _unreadCount = MutableStateFlow(notificationManager.getUnreadCount())

    /** Mirrors iOS's `Tab("Read", ...).badge(notificationManager.unreadCount)`. */
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    fun schedulePeriodicPoll() {
        viewModelScope.launch {
            notificationManager.schedulePeriodicPoll()
        }
    }

    /** Re-reads the persisted count -- call when the Reader tab becomes visible again,
     *  since a background poll updates it out from under any already-collected state. */
    fun refreshUnreadCount() {
        _unreadCount.value = notificationManager.getUnreadCount()
    }

    fun markAllAsRead() {
        notificationManager.markAllAsRead()
        refreshUnreadCount()
    }
}
