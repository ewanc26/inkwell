package uk.ewancroft.inkwell.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uk.ewancroft.inkwell.data.remote.InkwellNotification
import uk.ewancroft.inkwell.data.remote.InkwellNotificationManager
import javax.inject.Inject

@HiltViewModel
class InkwellNotificationViewModel @Inject constructor(
    private val notificationManager: InkwellNotificationManager,
) : ViewModel() {

    private val _unreadCount = MutableStateFlow(notificationManager.getUnreadCount())

    /** Mirrors iOS's `Tab("Read", ...).badge(notificationManager.unreadCount)`. */
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _notifications = MutableStateFlow(notificationManager.getNotifications())

    /** Backs the in-app notification list -- mirrors iOS `NotificationManager.notifications`. */
    val notifications: StateFlow<List<InkwellNotification>> = _notifications.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(notificationManager.isNotificationsEnabled())

    /** Backs SettingsScreen's toggle -- mirrors iOS `NotificationManager.notificationsEnabled`. */
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    fun setNotificationsEnabled(enabled: Boolean) {
        notificationManager.setNotificationsEnabled(enabled)
        _notificationsEnabled.value = enabled
    }

    fun schedulePeriodicPoll() {
        viewModelScope.launch {
            notificationManager.schedulePeriodicPoll()
        }
    }

    /** Re-reads persisted state -- call when the notification list or badge becomes
     *  visible again, since a background poll updates it out from under any
     *  already-collected state. */
    fun refreshUnreadCount() {
        _unreadCount.value = notificationManager.getUnreadCount()
    }

    fun refreshNotifications() {
        _notifications.value = notificationManager.getNotifications()
    }

    fun markAllAsRead() {
        notificationManager.markAllAsRead()
        refreshUnreadCount()
    }

    /** Mirrors iOS `NotificationManager.clearAll()`. */
    fun clearAll() {
        notificationManager.clearAll()
        refreshNotifications()
        refreshUnreadCount()
    }
}
