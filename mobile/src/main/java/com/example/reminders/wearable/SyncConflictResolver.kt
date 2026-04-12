package com.example.reminders.wearable

import com.example.reminders.data.model.Reminder

class SyncConflictResolver {

    fun resolve(local: Reminder, remote: Reminder): Reminder =
        if (!remote.updatedAt.isBefore(local.updatedAt)) remote else local
}
