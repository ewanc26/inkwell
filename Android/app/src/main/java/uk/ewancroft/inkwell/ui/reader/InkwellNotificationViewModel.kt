package uk.ewancroft.inkwell.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import uk.ewancroft.inkwell.data.remote.InkwellNotificationManager
import javax.inject.Inject

@HiltViewModel
class InkwellNotificationViewModel @Inject constructor(
    private val notificationManager: InkwellNotificationManager,
) : ViewModel() {

    fun schedulePeriodicPoll() {
        viewModelScope.launch {
            notificationManager.schedulePeriodicPoll()
        }
    }

    fun getUnreadCount(): Int = notificationManager.getUnreadCount()
}
