# Tombstone-Based Bidirectional Sync Architecture

## 1. Overview

Both mobile and wear maintain their own local Room databases. Sync occurs when the devices connect via the Wearable Data Layer. The architecture uses tombstones (soft delete) instead of hard deletes, with a 7-day retention period and trash/restore UI.

## 2. Data Model Changes

### Reminder / WatchReminder entities
Add two new fields:
- `createdBy` (String, default "mobile" or "watch") — which device originally created this reminder
- `lastModifiedBy` (String, default "mobile" or "watch") — which device last modified this reminder

These are metadata fields that help with conflict resolution and debugging.

### DeletedReminder entity (new)
Both sides get a `DeletedReminder` tombstone table:

```kotlin
@Entity(tableName = "deleted_reminders")
data class DeletedReminder(
    @PrimaryKey val id: String,              // same ID as the original reminder
    val originalTitle: String,               // for display in trash UI
    val deletedAt: Instant,                  // when it was deleted
    val deletedBy: String,                   // "mobile" or "watch"
    val originalUpdatedAt: Instant           // the reminder's updatedAt at time of deletion
)
```

## 3. Sync Flow

### 3.1 State Exchange Protocol
When devices connect, they exchange their full state:

1. Watch sends `SYNC_STATE_REQUEST` to phone
2. Phone responds with `SYNC_STATE_RESPONSE` containing:
   - All active reminders (with createdBy/lastModifiedBy)
   - All tombstones
   - Phone's deviceId
3. Watch runs SyncEngine.reconcile() with local vs remote state
4. Watch applies reconciliation results locally
5. Watch sends `SYNC_STATE_COMPLETE` with its own state back
6. Phone runs SyncEngine.reconcile() and applies results

### 3.2 Reconciliation Rules (SyncEngine)

For each remote active reminder:
- If local doesn't have it → INSERT
- If local has it → compare updatedAt timestamps, newer wins (UPDATE)
- If local has it tombstoned → check if tombstone.originalUpdatedAt > remote.updatedAt. If yes, mark remote for deletion. If no, the remote edit wins and we remove the tombstone (restore).

For each remote tombstone:
- If local has the reminder active AND tombstone.originalUpdatedAt >= local.updatedAt → mark for local deletion (move to tombstone)
- If local doesn't have the reminder → tombstone is irrelevant, but store it for future reference

### 3.3 Completion Status
Completed reminders sync as UPDATES (isCompleted=true). They are NOT deleted. The receiving side updates the local copy's isCompleted field.

### 3.4 Deletion Flow
When a user deletes a reminder:
1. Move reminder to DeletedReminder tombstone table (copy id, title, deletedAt, deletedBy, original updatedAt)
2. Delete from active reminders table
3. If connected, immediately send tombstone to the other device via `SYNC_TOMBSTONE` message
4. On next full sync, tombstone will be exchanged as part of state

### 3.5 Trash UI
- Both mobile and wear have a Trash screen showing deleted reminders from the last 7 days
- Users can restore deleted reminders (moves back to active table)
- Tombstones older than 7 days are automatically cleaned up

## 4. Data Layer Paths

```
SYNC_STATE_REQUEST  = "/sync/state-request"   // watch → phone
SYNC_STATE_RESPONSE = "/sync/state-response"  // phone → watch
SYNC_STATE_COMPLETE = "/sync/state-complete"   // watch → phone
SYNC_TOMBSTONE      = "/sync/tombstone"        // either direction
```

## 5. DTOs

```kotlin
@Serializable
data class DeletedReminderDto(
    val id: String,
    val originalTitle: String,
    val deletedAt: Long,          // epoch millis
    val deletedBy: String,
    val originalUpdatedAt: Long   // epoch millis
)

@Serializable
data class SyncStateDto(
    val activeReminders: List<ReminderDto>,
    val tombstones: List<DeletedReminderDto>,
    val deviceId: String
)
```

## 6. Critical Rules

- Never hard-delete a reminder during sync. Always use tombstones.
- Completed reminders sync as updates, not deletions.
- Tombstones older than 7 days can be purged.
- `createdBy` and `lastModifiedBy` are set at creation time and on every update respectively.
- Database migrations must use explicit Migration objects (never fallbackToDestructiveMigration).
- Do NOT use @Serializable on Room entities (KSP2 bug).
