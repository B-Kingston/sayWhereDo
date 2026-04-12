package com.example.reminders.sync

import android.util.Log
import com.example.reminders.data.repository.ReminderRepository
import com.example.reminders.wearable.WearableDataSender

class WearableSyncClient(
    private val wearableDataSender: WearableDataSender,
    private val reminderRepository: ReminderRepository
) : ReminderSyncClient {

    override suspend fun syncReminderUpdate(reminderId: String) {
        val reminder = reminderRepository.getReminderById(reminderId)
        if (reminder == null) {
            Log.w(TAG, "Cannot sync update — reminder $reminderId not found")
            return
        }

        if (reminder.isCompleted) {
            wearableDataSender.deleteReminderFromWatch(reminderId)
        } else {
            wearableDataSender.syncReminderToWatch(reminder)
        }
    }

    override suspend fun syncReminderDeletion(reminderId: String) {
        wearableDataSender.deleteReminderFromWatch(reminderId)
    }

    companion object {
        private const val TAG = "WearableSyncClient"
    }
}
