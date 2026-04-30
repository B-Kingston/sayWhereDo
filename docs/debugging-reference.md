# ADB & Logcat Debugging Reference

Comprehensive CLI debugging techniques for Android and Wear OS. All commands assume `adb` is on `$PATH` and a single device/emulator is connected. For multi-device setups, prefix with `adb -s <serial>` or `adb -d` (device) / `adb -e` (emulator).

---

## Table of Contents

1. [ADB Diagnostic Commands](#1-adb-diagnostic-commands)
2. [Wear OS Specific Debugging](#2-wear-os-specific-debugging)
3. [Google Play Services Debugging](#3-google-play-services-debugging)
4. [Network Debugging](#4-network-debugging)
5. [Room/Database Debugging](#5-roomdatabase-debugging)
6. [Process and Memory Debugging](#6-process-and-memory-debugging)
7. [Logcat Advanced Usage](#7-logcat-advanced-usage)
8. [Permissions Debugging](#8-permissions-debugging)
9. [Alarm/JobScheduler/WorkManager Debugging](#9-alarmjobschedulerworkmanager-debugging)
10. [Geofence/Location Debugging](#10-geofencelocation-debugging)
11. [Quick Reference Cheat Sheet](#11-quick-reference-cheat-sheet)

---

## 1. ADB Diagnostic Commands

### 1.1 `adb dumpsys` — System Service Dumps

`dumpsys` queries every registered Android system service. Output is verbose; pipe through `grep` or redirect to file.

**List all available services:**
```bash
adb shell dumpsys -l            # list all dumpsys-able services
adb shell service list          # alternative: all registered services
```

**Key subsystems for app debugging:**

| Service | Command | Use Case |
|---------|---------|----------|
| `activity` | `dumpsys activity` | Activities, services, receivers, processes, recent tasks |
| `battery` | `dumpsys battery` | Battery level, charging state, power source |
| `package` | `dumpsys package <pkg>` | Permissions, providers, services, version info |
| `alarm` | `dumpsys alarm` | Pending alarms, alarm history, wakeup stats |
| `window` | `dumpsys window` | Current focus, display info, window tokens |
| `meminfo` | `dumpsys meminfo <pkg>` | Detailed memory breakdown per process |
| `procstats` | `dumpsys procstats` | Process stats over 3-24 hour windows |
| `netstats` | `dumpsys netstats` | Network usage stats by UID/app |
| `connectivity` | `dumpsys connectivity` | Active network, link properties, capabilities |
| `location` | `dumpsys location` | GPS/Network providers, last known location, geofences |
| `notification` | `dumpsys notification` | Active and historical notifications |
| `wearable` | `dumpsys wearable` | Data Layer nodes, connections, capability info |
| `jobscheduler` | `dumpsys jobscheduler` | Scheduled jobs, constraints, execution history |
| `content` | `dumpsys content` | Content provider state |
| `dbinfo` | `dumpsys dbinfo` | Database connection info per process |
| `appops` | `dumpsys appops` | App ops (runtime permission tracking) |
| `device_policy` | `dumpsys device_policy` | Device admin policies |
| `usagestats` | `dumpsys usagestats` | App usage history |

**Reduce output verbosity:**
```bash
adb shell dumpsys activity --help    # service-specific flags (varies by service)
adb shell dumpsys activity recents   # only recent tasks
adb shell dumpsys activity services com.example.reminders  # filter to package
```

### 1.2 `adb shell dumpsys activity` — Activity Manager Deep Dive

**Four sub-commands with distinct outputs:**

```bash
# Activities (task stacks)
adb shell dumpsys activity activities
# Shows: all task stacks, which activity is focused, back stack state

# Services
adb shell dumpsys activity services
# Shows: all running services, binding info, start count
adb shell dumpsys activity services com.example.reminders
# Filter to your package only

# Broadcast receivers
adb shell dumpsys activity broadcasts
# Shows: pending and dispatched broadcasts, registered receivers
adb shell dumpsys activity broadcasts com.example.reminders

# Processes
adb shell dumpsys activity processes
# Shows: process importance (foreground/service/background), adj values, oom score
```

**Key sections to look for:**
- `mResumedActivity` — which activity is currently resumed
- `mFocusedActivity` — which activity has focus
- `Hist #N` entries — task history with intent data
- `ServiceRecord` — service start/bind state, last activity time
- `BroadcastQueue` — pending/delayed broadcasts (useful for debugging BOOT_COMPLETED, etc.)
- `ProcessRecord` — process state, `oom_adj` score, last CPU usage

**Quick "what's running" check:**
```bash
adb shell dumpsys activity activities | grep -E "mResumed|mFocused|Hist"
adb shell dumpsys activity services | grep "ServiceRecord"
```

### 1.3 `adb shell am` — Activity Manager Commands

```bash
# Force-stop an app (kills process, cleans state)
adb shell am force-stop com.example.reminders

# Clear app data (equivalent to Clear Data in Settings)
adb shell pm clear com.example.reminders

# Start an activity
adb shell am start -n com.example.reminders/.MainActivity
adb shell am start -n com.example.reminders/.MainActivity -d "reminders://edit/123"
adb shell am start -a android.intent.action.VIEW -d "reminders://edit/123" com.example.reminders
adb shell am start --activity-clear-top -n com.example.reminders/.MainActivity

# Start a service (deprecated on Android 8+ but still works for foreground services)
adb shell am startservice -n com.example.reminders/.sync.WearableListenerServiceImpl

# Send a broadcast
adb shell am broadcast -a android.intent.action.BOOT_COMPLETED com.example.reminders
adb shell am broadcast -a com.example.reminders.ACTION_SYNC_COMPLETE
adb shell am broadcast -a android.intent.action.MY_PACKAGE_REPLACED

# Monitor activity manager events (real-time, Ctrl+C to stop)
adb shell am monitor
# Shows: activity starts, crashes, ANRs, GC events

# Profile / trace
adb shell am start -S -n com.example.reminders/.MainActivity  # -S = force stop before start

# Trigger a crash (for testing crash handlers)
adb shell am crash com.example.reminders

# Set debug app (waits for debugger on launch)
adb shell am set-debug-app -w com.example.reminders
adb shell am clear-debug-app

# Watch device state transitions
adb shell am stack list     # list all activity stacks (multi-window)
```

### 1.4 `adb shell pm` — Package Manager Commands

```bash
# List packages
adb shell pm list packages                         # all packages
adb shell pm list packages -f                      # with APK path
adb shell pm list packages -d                      # disabled packages
adb shell pm list packages -e                      # enabled packages
adb shell pm list packages -s                      # system packages only
adb shell pm list packages -3                      # third-party (user) packages only
adb shell pm list packages | grep reminders        # find your package

# Full package info dump (permissions, activities, services, providers, receivers)
adb shell dumpsys package com.example.reminders

# Specific package info
adb shell pm path com.example.reminders            # APK install path
adb shell pm dump com.example.reminders            # full dump (same as dumpsys package)

# Install/uninstall
adb install app-debug.apk                          # install
adb install -r app-debug.apk                       # reinstall (keep data)
adb install -r -g app-debug.apk                    # reinstall + auto-grant permissions
adb install -t app-debug.apk                       # allow test APKs
adb shell pm uninstall com.example.reminders       # uninstall

# Clear data
adb shell pm clear com.example.reminders

# Permissions
adb shell pm grant com.example.reminders android.permission.ACCESS_FINE_LOCATION
adb shell pm revoke com.example.reminders android.permission.ACCESS_FINE_LOCATION
adb shell pm list permissions -g                    # all permissions by group

# Disable/enable components (useful for testing without uninstalling)
adb shell pm disable-component com.example.reminders/.sync.WearableListenerServiceImpl
adb shell pm enable-component com.example.reminders/.sync.WearableListenerServiceImpl

# Query activities that handle an intent
adb shell pm query-activities -a android.intent.action.TTS_SERVICE
```

### 1.5 `adb shell settings` — System Settings

```bash
# Three namespaces: system, global, secure
# Read a setting
adb shell settings get global airplane_mode_on
adb shell settings get secure location_mode            # 0=off, 1=device only, 2=battery saving, 3=high accuracy
adb shell settings get system screen_brightness

# Write a setting
adb shell settings put global airplane_mode_on 1
adb shell settings put global development_settings_enabled 1

# Useful settings for debugging
adb shell settings put global always_finish_activities 1    # Developer option: don't keep activities
adb shell settings put global window_animation_scale 0      # Disable animations (faster testing)
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

# List all settings in a namespace
adb shell settings list system
adb shell settings list global
adb shell settings list secure
```

### 1.6 `adb shell cmd` — Subsystem Commands

```bash
# Jobscheduler
adb shell cmd jobscheduler

# Network
adb shell cmd netd

# SurfaceFlinger
adb shell cmd surfaceflinger

# Battery properties
adb shell cmd battery set level 5           # simulate low battery
adb shell cmd battery set ac 1              # simulate AC charging
adb shell cmd battery reset                 # reset to real state

# Notification listener services
adb shell cmd notification allow_listener com.example.reminders/.notification.MyListener
adb shell cmd notification disallow_listener com.example.reminders/.notification.MyListener

# Device idle (Doze mode testing)
adb shell cmd deviceidle force-idle          # force into Doze
adb shell cmd deviceidle step                # step through Doze states
adb shell cmd deviceidle unforce             # exit forced Doze

# App standby testing
adb shell cmd appops set com.example.reminders RUN_IN_BACKGROUND allow
adb shell cmd appops set com.example.reminders RUN_IN_BACKGROUND ignore

# Sync adapter
adb shell cmd sync
```

---

## 2. Wear OS Specific Debugging

### 2.1 Wearable Data Layer Debugging

**Check Data Layer connection state:**
```bash
# Full wearable service dump
adb shell dumpsys wearable

# Look for these sections in the output:
# - Nodes: connected watch/phone nodes
# - Capabilities: registered capability listeners
# - DataItems: synced data items
# - Channels: open channels
# - MessageClient: pending messages
```

**Check connected nodes:**
```bash
# On phone — shows connected watches
adb shell dumpsys wearable | grep -A 20 "Nodes"

# On watch — shows connected phone
adb -s <watch_serial> shell dumpsys wearable | grep -A 20 "Nodes"
```

**Check capability discovery:**
```bash
# Shows what capabilities are registered and which nodes advertise them
adb shell dumpsys wearable | grep -A 30 "Capability"
```

**Check DataItem sync state:**
```bash
# Shows pending syncs, synced items, and their versions
adb shell dumpsys wearable | grep -A 50 "DataItem"
```

### 2.2 Wear OS Pairing Verification

```bash
# Check if Wear OS companion app is installed on phone
adb shell pm list packages | grep google.android.apps.wear

# Check Play Services wearable module
adb shell dumpsys package com.google.android.gms | grep -A 5 "wearable"

# Verify both devices see each other
# On phone:
adb shell dumpsys wearable | grep "connected\|NodeId"
# On watch:
adb -s <watch_serial> shell dumpsys wearable | grep "connected\|NodeId"

# Check if WearableListenerService is registered properly
adb shell dumpsys package com.example.reminders | grep -A 10 "WearableListenerService"
```

### 2.3 `adb forward tcp:5601 tcp:5601` — Emulator Data Layer Bridge

**When needed:** Only when using the Wear OS emulator. The emulator communicates with the phone companion app via a TCP bridge over ADB.

```bash
# Forward port 5601 (Wear OS companion protocol)
adb forward tcp:5601 tcp:5601

# Check active forwards
adb forward --list

# Remove the forward when done
adb forward --remove tcp:5601
```

**How it works:**
1. The Wear OS emulator's Play Services listens on `localhost:5601`
2. `adb forward` bridges the emulator's port 5601 to the host machine's port 5601
3. The phone's Wear OS companion app connects to `localhost:5601` on the host
4. This creates a virtual Data Layer connection between emulator and phone

**Prerequisites for emulator pairing:**
1. Wear OS emulator must be running
2. Phone must have Wear OS companion app installed
3. Port 5601 must be forwarded
4. Phone and emulator must be on same Wi-Fi network (or phone connected via USB)
5. Pair the emulator from the Wear OS companion app on the phone

### 2.4 Wear OS Emulator Pairing via Android Studio

**Steps:**
1. Create a Wear OS emulator in Android Studio Device Manager (API 33+)
2. Start the emulator
3. On the physical phone, open the **Wear OS** companion app (install from Play Store if needed)
4. The emulator should appear as a paired device in the companion app
5. If not, ensure `adb forward tcp:5601 tcp:5601` is active
6. In the companion app, tap "Pair with new watch" and select the emulator

**Verify pairing:**
```bash
# On emulator — should show the phone node
adb -e shell dumpsys wearable | grep -E "Node|connected"

# On phone — should show the emulator node
adb -d shell dumpsys wearable | grep -E "Node|connected"
```

### 2.5 Known Emulator Data Layer Issues and Workarounds

| Issue | Symptom | Workaround |
|-------|---------|------------|
| Data Layer disconnects | `dumpsys wearable` shows no nodes | Restart adb (`adb kill-server && adb start-server`), re-forward port |
| Messages not delivered | `MessageClient.sendMessage` returns success but nothing received | Check `dumpsys wearable` for stale channels; restart Play Services on watch |
| Capability not found | `CapabilityClient.getCapability()` returns empty | Ensure both sides advertise the same capability string; check case sensitivity |
| Slow sync | DataItems take 30+ seconds | Known issue; reduce DataItem payload size; avoid frequent updates |
| Emulator loses pairing | After reboot, Data Layer broken | Re-run `adb forward tcp:5601 tcp:5601`; re-pair in companion app |
| Play Services crash on emulator | `Google Play Services has stopped` | Update Play Services on emulator via Play Store; wipe emulator data and recreate |

### 2.6 Checking WearableListenerService Registration

```bash
# Verify the service is in the manifest with correct intent-filter
adb shell dumpsys package com.example.reminders | grep -B 5 -A 15 "WearableListenerService"

# Look for in the output:
# Service{...WearableListenerServiceImpl}
#   intent-filter:
#     action: "com.google.android.gms.wearable.BIND_LISTENER"
#     data: "wear://*/..." (if path-based filtering)

# Check if the service is actually running
adb shell dumpsys activity services com.example.reminders | grep -A 5 "WearableListener"

# Force the service to restart
adb shell am force-stop com.example.reminders
# Then trigger a data layer event to auto-start the service
```

---

## 3. Google Play Services Debugging

### 3.1 `dumpsys activity service com.google.android.gms`

```bash
# Full Play Services dump (very large — redirect to file)
adb shell dumpsys activity service com.google.android.gms > gms_dump.txt

# Wearable service specifically
adb shell dumpsys activity service com.google.android.gms | grep -A 100 "WearableService"
adb shell dumpsys activity service com.google.android.gms | grep -A 100 "Wearable"

# Connection states
adb shell dumpsys activity service com.google.android.gms | grep -A 50 "Connection"
```

**What to look for:**
- `WearableService` — the core Data Layer service
- `ConnectionState` — CONNECTED, CONNECTING, DISCONNECTED
- `GoogleApiClient` connections — which clients are connected, pending connections
- `PendingResult` — operations waiting for completion

### 3.2 Checking Play Services Version

```bash
# Full package info
adb shell dumpsys package com.google.android.gms | grep -E "versionName|versionCode"

# Quick version check
adb shell dumpsys package com.google.android.gms | grep versionName

# Check if Wearable API is available
adb shell dumpsys package com.google.android.gms | grep -i wearable
```

### 3.3 WearableService — Connection States and Stuck Connections

**Diagnosing stuck connections:**
```bash
# Check if WearableService is running
adb shell dumpsys activity services com.google.android.gms | grep -B 2 -A 20 "Wearable"

# Check process state
adb shell dumpsys activity processes | grep -A 10 "com.google.android.gms"

# If you see the service stuck in CONNECTING state:
# 1. Check network connectivity
adb shell dumpsys connectivity | grep -A 10 "NetworkAgentInfo"

# 2. Check if Play Services is battery-optimized (should be whitelisted)
adb shell dumpsys deviceidle whitelist | grep gms
```

### 3.4 Restarting WearableService Without Clearing Data

**Option 1: Force-stop Play Services (safe, retains data)**
```bash
adb shell am force-stop com.google.android.gms
# The system will auto-restart it when needed. Your app's Data Layer will reconnect.
# Wait 5-10 seconds, then open your app to trigger reconnection.
```

**Option 2: Kill the GMS process (also safe)**
```bash
# Find PID
adb shell pidof com.google.android.gms
# Or
adb shell ps -A | grep google.android.gms

# Kill it (SIGTERM)
adb shell kill <pid>

# System restarts it automatically
```

**Option 3: Restart your app's GMS connection (least disruptive)**
```bash
# Just restart your app — it will create a new GoogleApiClient
adb shell am force-stop com.example.reminders
# Then launch your app
adb shell am start -n com.example.reminders/.MainActivity
```

> **WARNING:** NEVER run `adb shell pm clear com.google.android.gms` — this clears ALL Google account data, Wear OS pairing, and requires full device setup again.

---

## 4. Network Debugging

### 4.1 `adb shell dumpsys connectivity`

```bash
# Full connectivity state
adb shell dumpsys connectivity

# Key sections:
# - NetworkAgentInfo — active networks, their capabilities and link properties
# - Default network — which network is default
# - Network requests — what apps have requested
# - Network callbacks — registered NetworkCallbacks

# Quick: what network am I on?
adb shell dumpsys connectivity | grep -A 5 "NetworkAgentInfo"

# Check if network is metered (affects background work)
adb shell dumpsys connectivity | grep -i "metered"

# Check Wi-Fi specifically
adb shell dumpsys wifi
adb shell dumpsys wifi | grep -E "SSID|mWifiInfo|signal"

# DNS servers
adb shell dumpsys connectivity | grep -i "dns"
```

### 4.2 `adb shell dumpsys netstats`

```bash
# Network usage stats
adb shell dumpsys netstats

# Per-app usage (look for your UID)
adb shell dumpsys netstats | grep -A 10 "com.example.reminders"
# Or by UID:
adb shell dumpsys netstats | grep -A 10 "uid=10"

# Stats summary
adb shell dumpsys netstats --full    # full history
```

### 4.3 OkHttp/HTTP Debugging via Interceptors and Logcat

**Add an HttpLoggingInterceptor (in debug builds only):**
```kotlin
// In your OkHttp client setup (debug builds only)
val client = OkHttpClient.Builder()
    .addInterceptor(HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    })
    .build()
```

**Then filter logcat:**
```bash
# OkHttp logs with tag "OkHttp"
adb logcat -s OkHttp

# With full body
adb logcat -s OkHttp:V

# Filter to your API calls only
adb logcat -s OkHttp | grep "api.example"
```

**Alternative: use `adb shell` to inspect ongoing connections:**
```bash
# Active TCP connections
adb shell netstat -tulpn 2>/dev/null || adb shell cat /proc/net/tcp

# DNS cache (if accessible)
adb shell getprop net.dns1
adb shell getprop net.dns2
```

### 4.4 SSL/TLS Debugging Flags

```bash
# Enable SSL/TLS debugging in logcat (for your app process)
adb shell setprop log.tag.org.conscrypt VERBOSE
adb shell setprop log.tag.OkHttpClient VERBOSE

# SSL handshake debugging
adb logcat -s org.conscrypt OkHttp

# Network security config debugging (Android 7+)
adb shell setprop log.tag.NetworkSecurity VERBOSE

# Common issue: cleartext traffic not permitted
# Check if your network_security_config.xml allows cleartext for your domain
```

### 4.5 `adb shell cmd netd` Commands

```bash
# Network daemon control
adb shell cmd netd interface list              # list network interfaces
adb shell cmd netd interface readrxCounter rmnet0  # read bytes received

# IP rules and routes
adb shell ip route show
adb shell ip addr show
adb shell ip rule show

# iptables (for VPN/firewall debugging, requires root on most devices)
adb shell iptables -L -n -v
```

---

## 5. Room/Database Debugging

### 5.1 `adb shell run-as` — Access App-Private Data

```bash
# Enter a shell as your app's user (works for debuggable apps)
adb shell run-as com.example.reminders

# Once inside, you're in the app's data directory:
# /data/user/0/com.example.reminders/
# You can navigate:
cd databases/
ls -la

# Run commands without entering interactive shell:
adb shell run-as com.example.reminders ls -la databases/
adb shell run-as com.example.reminders ls -la shared_prefs/
adb shell run-as com.example.reminders ls -la files/
```

**Requirements:**
- App must be debuggable (`android:debuggable="true"` in manifest, or debug build)
- Cannot access other apps' data
- Works on non-rooted devices

### 5.2 Copying Room Database Files Off Device

**Method 1: Via run-as and cat (no root needed, debuggable app)**
```bash
# Copy database to sdcard first (accessible via run-as)
adb shell run-as com.example.reminders cat databases/reminders.db > /tmp/reminders.db

# Or use this two-step approach:
adb shell "run-as com.example.reminders cp databases/reminders.db /sdcard/reminders.db"
adb pull /sdcard/reminders.db ./reminders.db
adb shell rm /sdcard/reminders.db
```

**Method 2: Direct pull (requires root)**
```bash
adb pull /data/user/0/com.example.reminders/databases/reminders.db ./reminders.db
# Also pull the WAL and SHM files for a complete snapshot:
adb pull /data/user/0/com.example.reminders/databases/reminders.db-wal ./reminders.db-wal
adb pull /data/user/0/com.example.reminders/databases/reminders.db-shm ./reminders.db-shm
```

**Method 3: Using adb backup (no root, works for any app with backup enabled)**
```bash
# Create backup
adb backup -f reminders.ab com.example.reminders

# Extract (requires android-backup-extractor)
java -jar abe.jar unpack reminders.ab reminders.tar
tar xf reminders.tar
```

### 5.3 Using `sqlite3` via ADB Shell

```bash
# Open interactive sqlite3 session
adb shell run-as com.example.reminders sqlite3 databases/reminders.db

# Inside sqlite3:
sqlite> .tables                          # list all tables
sqlite> .schema reminders                # show CREATE TABLE statement
sqlite> SELECT * FROM reminders LIMIT 5; # peek at data
sqlite> SELECT COUNT(*) FROM reminders;  # row count
sqlite> .headers on                      # show column names
sqlite> .mode column                     # column-aligned output
sqlite> .mode csv                        # CSV output
sqlite> .output /sdcard/export.csv       # write results to file
sqlite> SELECT * FROM reminders;
sqlite> .output stdout

# One-liner queries
adb shell run-as com.example.reminders sqlite3 databases/reminders.db "SELECT COUNT(*) FROM reminders;"
adb shell run-as com.example.reminders sqlite3 databases/reminders.db "SELECT id, title, trigger_time FROM reminders WHERE completed = 0;"

# Check Room migration state
adb shell run-as com.example.reminders sqlite3 databases/reminders.db "SELECT * FROM room_master_table;"
# Output shows: identity_hash, version — confirms which migration was applied

# Export full database to SQL
adb shell run-as com.example.reminders sqlite3 databases/reminders.db .dump > reminders_dump.sql
```

### 5.4 Checking Migration State

```bash
# Room stores migration info in room_master_table
adb shell run-as com.example.reminders sqlite3 databases/reminders.db "SELECT * FROM room_master_table;"

# Check schema version
adb shell run-as com.example.reminders sqlite3 databases/reminders.db "PRAGMA user_version;"
# This returns the schema version number (e.g., 2 for mobile, 3 for watch)

# Check table schemas
adb shell run-as com.example.reminders sqlite3 databases/reminders.db ".schema"

# Check if specific column exists (useful after migration)
adb shell run-as com.example.reminders sqlite3 databases/reminders.db "PRAGMA table_info(reminders);"
```

---

## 6. Process and Memory Debugging

### 6.1 `adb shell dumpsys meminfo`

```bash
# Memory for your app
adb shell dumpsys meminfo com.example.reminders

# Key sections in output:
# - Java Heap: Java/Kotlin allocations
# - Native Heap: malloc allocations
# - Graphics: GPU memory
# - Stack: thread stacks
# - Code: .so, .dex, .oat files
# - Private Other: private allocations not classified
# - System: shared allocations
# - TOTAL: sum of all above
# - Views, ViewRootImpl, AppContexts, Activities — leak indicators

# Summary view
adb shell dumpsys meminfo --package com.example.reminders

# Compare before/after to detect leaks:
adb shell dumpsys meminfo com.example.reminders > before.txt
# ... perform actions ...
adb shell dumpsys meminfo com.example.reminders > after.txt
diff before.txt after.txt
```

**Key numbers to watch:**
- `TOTAL` — overall memory usage
- `Views` / `Activities` — should decrease when you navigate away
- `Java Heap` — should stabilize; continuous growth = leak

### 6.2 `adb shell dumpsys procstats`

```bash
# Process stats over time (3-24 hour windows)
adb shell dumpsys procstats

# Stats for your package
adb shell dumpsys procstats com.example.reminders

# Key output:
# - How long the process has been running
# - Memory stats: min/avg/max RAM, cached memory
# - Process states: TOP, FOREGROUND, BACKGROUND, CACHED, etc.
# - Execution times in each state

# Reset stats
adb shell dumpsys procstats --reset

# Current state only
adb shell dumpsys procstats --current com.example.reminders
```

### 6.3 Finding PIDs and Process Management

```bash
# Find your app's PID
adb shell pidof com.example.reminders

# Alternative
adb shell ps -A | grep com.example.reminders

# Detailed process info
adb shell ps -A -p $(adb shell pidof com.example.reminders) -o pid,uid,rss,vsz,cmd

# List all processes with memory usage
adb shell procrank      # requires root or debug build
adb shell showmap <pid>  # detailed memory map (requires root)

# Check thread count
adb shell ps -T -p $(adb shell pidof com.example.reminders) | wc -l

# List threads
adb shell ps -T -p $(adb shell pidof com.example.reminders)
```

### 6.4 Sending Signals

```bash
# Graceful termination
adb shell kill <pid>

# Force kill (SIGKILL)
adb shell kill -9 <pid>

# Force-stop is preferred over kill — it's cleaner
adb shell am force-stop com.example.reminders
```

### 6.5 ANR Trace Files

```bash
# Read ANR traces (requires root or run-as for your app)
adb shell cat /data/anr/traces.txt

# Pull the file
adb pull /data/anr/traces.txt ./anr_traces.txt

# Recent ANRs only
adb shell cat /data/anr/traces.txt | grep -A 50 "com.example.reminders"

# Check for ANRs in dropbox (system log)
adb shell dumpsys dropbox --print
# Or more specifically:
adb shell logcat -b events -d | grep "am_anr"

# ANR in logcat
adb logcat -d | grep -i "anr\|not responding"
```

---

## 7. Logcat Advanced Usage

### 7.1 Format Options (`--format`)

```bash
# Available formats:
adb logcat --format=brief      # "D/TAG ( PID): message" (default)
adb logcat --format=process    # "PID priority TAG: message"
adb logcat --format=tag        # "priority/TAG (PID): message"
adb logcat --format=thread     # "priority (PID:threadID) TAG: message"
adb logcat --format=raw        # "message" only (no metadata)
adb logcat --format=time       # "datetime priority/TAG (PID): message"
adb logcat --format=threadtime # "datetime PID-TID priority TAG: message" (best for debugging)
adb logcat --format=long       # multi-line with full metadata (good for crash traces)

# Recommended for production debugging:
adb logcat --format=threadtime  # shows thread IDs, essential for async debugging
```

### 7.2 Buffer Selection (`-b`)

```bash
# Main buffer (default — most app logs)
adb logcat -b main

# System buffer (system-level messages)
adb logcat -b system

# Radio buffer (telephony/RIL)
adb logcat -b radio

# Events buffer (system events, binary format)
adb logcat -b events

# Crash buffer (crash logs only)
adb logcat -b crash

# Kernel buffer
adb logcat -b kernel

# Multiple buffers at once
adb logcat -b main,system,crash

# All buffers
adb logcat -b all
```

### 7.3 Dump Mode vs Streaming

```bash
# Dump current contents and exit (non-streaming)
adb logcat -d                # dump main buffer
adb logcat -d -b crash       # dump crash buffer only
adb logcat -D                # dump and exit (same as -d, alias)

# Stream live logs (default behavior)
adb logcat                   # streams continuously, Ctrl+C to stop

# Last N lines
adb logcat -t 100            # show last 100 lines, then exit (like tail)
adb logcat -t "12-25 10:00:00.000"  # show logs since this timestamp
```

### 7.4 Filtering by Tag, Priority, and PID

**Priority levels:**
| Letter | Level | Value |
|--------|-------|-------|
| V | Verbose | 2 |
| D | Debug | 3 |
| I | Info | 4 |
| W | Warn | 5 |
| E | Error | 6 |
| F | Fatal | 7 |
| S | Silent (suppress) | 8 |

**Tag-based filtering:**
```bash
# Single tag at Debug level
adb logcat -s "SyncEngine:D"

# Multiple tags
adb logcat -s "SyncEngine:D" "PipelineOrchestrator:D" "FormattingProvider:D"

# Suppress all except your tags (S = Silent, suppresses everything else)
adb logcat -s "SyncEngine:D" "PipelineOrchestrator:D" "*:S"

# Wildcard — all tags at minimum priority
adb logcat "*:W"              # only Warn and above from all tags
adb logcat "*:E"              # only Error and above

# Tag regex
adb logcat --pid=$(adb shell pidof com.example.reminders) | grep -E "Sync|Geofence|Alarm"
```

**PID-based filtering (recommended for app-specific logs):**
```bash
# Filter to your app's process only
adb logcat --pid=$(adb shell pidof com.example.reminders)

# When app isn't running yet, use a wrapper:
adb logcat --pid=$(adb shell pidof com.example.reminders) &
sleep 1
adb shell am start -n com.example.reminders/.MainActivity
```

### 7.5 Regex Filtering

```bash
# Built-in regex support
adb logcat -e "SyncEngine|GeofenceManager"        # match pattern in message
adb logcat --regex="SyncEngine|GeofenceManager"    # same as -e

# Case-insensitive regex
adb logcat -e "(?i)sync|geofence"

# Pipe through grep for more control
adb logcat | grep -E "SyncEngine|PipelineResult"
adb logcat | grep -v "AudioFlinger\|SurfaceFlinger"  # exclude noisy tags

# Exclude patterns (suppress noise)
adb logcat | grep -vE "^.{0,30}(AudioFlinger|gralloc|SurfaceFlinger|BLASTBuffer|Choreographer)"
```

### 7.6 Writing to File with Rotation

```bash
# Write to file
adb logcat > debug.log

# With rotation (rotate through N files, max size M bytes)
adb logcat -f debug.log -r 1024 -n 5
# -f: output file on device
# -r 1024: rotate every 1 KB (use 16384 for 16MB)
# -n 5: keep 5 rotated files (debug.log, debug.log.1, ..., debug.log.4)

# Pull the log file from device
adb pull /data/local/debug.log ./debug.log

# Write directly to host file
adb logcat -v threadtime > "$(date +%Y%m%d_%H%M%S)_logcat.txt"
```

### 7.7 Buffer Size Management

```bash
# Check current buffer size
adb logcat -g

# Set buffer size
adb logcat -G 16M        # 16 megabytes
adb logcat -G 1M         # 1 megabyte (tight — may lose logs quickly)
adb logcat -G 64M        # 64 megabytes (generous, uses more memory)

# Default is typically 256KB-4MB depending on device
# For intensive debugging, increase to at least 16M
```

### 7.8 Practical Logcat Combos

```bash
# App-only logs with thread info, written to timestamped file
adb logcat -v threadtime --pid=$(adb shell pidof com.example.reminders) > debug_$(date +%s).log

# All errors + warnings system-wide
adb logcat "*:W" -v time

# Watch sync and formatting subsystems specifically
adb logcat -v threadtime -s "SyncEngine" "PipelineOrchestrator" "FormattingProvider" "WearableSyncClient"

# Crash-focused: crash buffer + fatal from main
adb logcat -b crash -v long
adb logcat "*:E" -v time

# Real-time monitoring of geofence events
adb logcat --pid=$(adb shell pidof com.example.reminders) | grep -i "geofence\|location\|fused"

# Clear logcat buffer before starting fresh
adb logcat -c && adb logcat -v threadtime
```

---

## 8. Permissions Debugging

### 8.1 Checking Granted Permissions

```bash
# All permissions for your app (granted + requested)
adb shell dumpsys package com.example.reminders | grep -A 200 "granted=true"

# Specifically check location permissions
adb shell dumpsys package com.example.reminders | grep -E "ACCESS_FINE|ACCESS_COARSE|ACCESS_BACKGROUND"

# Check all runtime permissions and their state
adb shell dumpsys package com.example.reminders | grep -A 5 "runtime permissions"

# App ops (tracks actual permission usage, not just grants)
adb shell dumpsys appops com.example.reminders
# Shows: when permissions were last used, access frequency, deny/allow state

# Quick check: is a specific permission granted?
adb shell dumpsys package com.example.reminders | grep "android.permission.ACCESS_FINE_LOCATION"
# Look for "granted=true" or "granted=false"
```

### 8.2 Granting/Revoking Runtime Permissions

```bash
# Grant a permission
adb shell pm grant com.example.reminders android.permission.ACCESS_FINE_LOCATION
adb shell pm grant com.example.reminders android.permission.ACCESS_COARSE_LOCATION
adb shell pm grant com.example.reminders android.permission.RECORD_AUDIO
adb shell pm grant com.example.reminders android.permission.POST_NOTIFICATIONS

# Revoke a permission
adb shell pm revoke com.example.reminders android.permission.ACCESS_FINE_LOCATION

# Grant all permissions at install time
adb install -r -g app-debug.apk

# Reset all runtime permissions (user must re-grant via UI)
adb shell pm reset-permissions com.example.reminders

# Grant on Wear OS watch
adb -s <watch_serial> shell pm grant com.example.reminders.wear android.permission.ACCESS_FINE_LOCATION
```

### 8.3 Checking Permission Rationale State

```bash
# Android doesn't expose "don't ask again" state directly via adb
# But you can check if the permission was permanently denied:
# Method: check if shouldShowRequestPermissionRationale returns false AND permission is not granted

# Reset the "don't ask again" state by clearing app data
adb shell pm clear com.example.reminders

# Or just re-grant the permission for testing
adb shell pm grant com.example.reminders android.permission.ACCESS_FINE_LOCATION
```

---

## 9. Alarm/JobScheduler/WorkManager Debugging

### 9.1 `dumpsys alarm`

```bash
# All alarm info
adb shell dumpsys alarm

# Key sections:
# - Pending alarm batches — alarms waiting to fire
# - Alarm history — recently fired alarms
# - Top alarms — which apps consume the most alarm budget
# - Device idle whitelisted apps

# Filter to your package
adb shell dumpsys alarm | grep -A 20 "com.example.reminders"

# Check if your alarm is scheduled
adb shell dumpsys alarm | grep -E "com.example.reminders|ReminderAlarms"

# Alarm history (recently triggered)
adb shell dumpsys alarm | grep -A 50 "Past alarms"

# Check if app is whitelisted for Doze (alarms work in Doze)
adb shell dumpsys deviceidle whitelist | grep reminders
```

**Alarm types in output:**
- `ELAPSED_REALTIME_WAKEUP` — wakes device, uses elapsed time
- `RTC_WAKEUP` — wakes device, uses wall clock
- `ELAPSED_REALTIME` — doesn't wake device
- `RTC` — doesn't wake device, uses wall clock

### 9.2 `dumpsys jobscheduler`

```bash
# All jobs
adb shell dumpsys jobscheduler

# Jobs for your package
adb shell dumpsys jobscheduler com.example.reminders

# Key sections:
# - Pending jobs — queued but not yet running
# - Active jobs — currently executing
# - Stopped jobs — recently completed or stopped
# - Job constraints — required network, charging, idle, etc.

# WorkManager shows up as jobs under your package name
# WorkManager job names look like: "com.example.reminders/androidx.work.impl.background.systemjob.SystemJobService"

# Check WorkManager-specific state
adb shell dumpsys jobscheduler | grep -A 10 "SystemJobService"

# Force a job to run immediately (Android 12+, with constraints)
adb shell cmd jobscheduler run -f com.example.reminders <job_id>
# -f flag forces the job even if constraints aren't met
```

### 9.3 WorkManager-Specific Debugging

```bash
# WorkManager stores state in Room DB under your app
adb shell run-as com.example.reminders sqlite3 databases/androidx.work.workdb "SELECT * FROM workspec;"
adb shell run-as com.example.reminders sqlite3 databases/androidx.work.workdb "SELECT id, state, worker_class_name, required_network_type FROM workspec;"

# Work states: ENQUEUED, RUNNING, SUCCEEDED, FAILED, BLOCKED, CANCELLED

# Check work preferences
adb shell run-as com.example.reminders sqlite3 databases/androidx.work.workdb "SELECT * FROM preference;"

# Trigger a specific work via adb (Android 12+)
# First find the job ID:
adb shell dumpsys jobscheduler com.example.reminders | grep "job id"
# Then run it:
adb shell cmd jobscheduler run -f com.example.reminders <job_id>
```

### 9.4 Testing Doze and App Standby

```bash
# Force Doze mode
adb shell dumpsys battery reset
adb shell cmd deviceidle force-idle
# Check state:
adb shell dumpsys deviceidle

# Step through Doze states (light then deep)
adb shell cmd deviceidle step    # each call advances state

# Exit Doze
adb shell cmd deviceidle unforce

# Force app into standby bucket
adb shell am set-standby-bucket com.example.reminders active      # no restrictions
adb shell am set-standby-bucket com.example.reminders working_set # mild deferrals
adb shell am set-standby-bucket com.example.reminders frequent    # more deferrals
adb shell am set-standby-bucket com.example.reminders restricted  # max deferrals
adb shell am set-standby-bucket com.example.reminders never       # extreme restriction

# Check current bucket
adb shell am get-standby-bucket com.example.reminders

# Test temp allow-list (exempt from Doze for a period)
adb shell cmd deviceidle tempwhitelist com.example.reminders
```

---

## 10. Geofence/Location Debugging

### 10.1 Location Services State

```bash
# Full location dump
adb shell dumpsys location

# Key sections:
# - Location Providers: which are enabled (gps, network, passive, fused)
# - Last Known Location: GPS and network
# - Geofences: registered geofences
# - Listeners: apps listening for location updates

# Quick: is GPS enabled?
adb shell dumpsys location | grep "gps:"

# Last known location
adb shell dumpsys location | grep -A 5 "Last Known"
```

### 10.2 Geofence Registration Checking

```bash
# Check registered geofences (via GMS)
adb shell dumpsys activity service com.google.android.gms | grep -A 30 -i "geofence"

# Check via location service
adb shell dumpsys location | grep -A 30 "Geofence"

# Your app's geofence registrations
adb shell dumpsys location | grep -A 20 "com.example.reminders"
```

**Key things to verify:**
- Geofences are actually registered (not just requested)
- Radius values are reasonable (100m default, 50m-250m typical)
- Transition types: `ENTER`, `EXIT`, `DWELL`
- `loiteringDelay` is set (should be 30000ms per project rules)
- `notificationResponsiveness` (how quickly transitions are reported)

### 10.3 Mock Location Providers

```bash
# Enable mock location app (one-time setup)
adb shell appops set com.example.reminders android:mock_location allow
# Or for a dedicated mock app:
adb shell appops set com.example.mocklocation android:mock_location allow

# Using adb to set a mock location (requires a mock location app or script):
# Android doesn't have a built-in CLI for setting mock locations
# Options:
# 1. Use the "Fake GPS" or similar app from Play Store
# 2. Write a small test app that uses LocationManager.setTestProviderLocation()
# 3. Use adb am broadcast with a custom receiver

# If you have a mock location provider app installed:
# Method: adb shell am broadcast
# This requires your app to have a BroadcastReceiver that applies mock locations
adb shell am broadcast -a com.example.reminders.SET_MOCK_LOCATION --es lat "37.422" --es lng "-122.084"
```

### 10.4 Location Accuracy Debugging

```bash
# Check GPS accuracy
adb shell dumpsys location | grep -A 10 "gps:" | grep -i "accuracy"

# Force a GPS fix (requires root, some devices)
adb shell settings put secure location_mode 3  # high accuracy
adb shell settings put secure location_providers_allowed +gps

# Monitor NMEA data (raw GPS, requires root usually)
adb shell cat /dev/socket/gps_nmea 2>/dev/null || echo "NMEA socket not accessible"

# Check location permissions at runtime
adb shell dumpsys appops com.example.reminders | grep -i location
```

### 10.5 Testing Geofence Transitions

Since you can't easily trigger geofence transitions via ADB, here are practical approaches:

**Approach 1: Use a mock location app**
```bash
# Install a mock location app
# Set it as the mock location provider in Developer Options
# Set location to be near your geofence center
# The system will fire ENTER transition
```

**Approach 2: Expand geofence radius temporarily**
```bash
# In your debug build, use a very large radius (e.g., 100000m = 100km)
# This ensures your current location triggers the geofence
```

**Approach 3: Send a test broadcast (if you have a test receiver)**
```bash
# Trigger your geofence handler directly
adb shell am broadcast -a com.example.reminders.ACTION_TEST_GEOFENCE --ei transition 1 --es geofenceId "test-1"
```

---

## 11. Quick Reference Cheat Sheet

### App-Specific Debugging Session

```bash
PKG=com.example.reminders
PID=$(adb shell pidof $PKG)

# 1. Start fresh logcat
adb logcat -c && adb logcat -v threadtime --pid=$PID > debug.log &

# 2. Force restart app
adb shell am force-stop $PKG
adb shell am start -n $PKG/.MainActivity

# 3. Check state
adb shell dumpsys activity activities | grep -E "mResumed|Hist" | head -5
adb shell dumpsys meminfo $PKG | head -20
adb shell dumpsys notification | grep -A 5 $PKG

# 4. Check database
adb shell run-as $PKG sqlite3 databases/reminders.db "SELECT COUNT(*) FROM reminders;"

# 5. Check permissions
adb shell dumpsys package $PKG | grep "granted=true" | head -10

# 6. Check alarms
adb shell dumpsys alarm | grep -A 5 $PKG
```

### Wear OS Debugging Session

```bash
WATCH=<watch_serial>
PHONE_PKG=com.example.reminders
WATCH_PKG=com.example.reminders.wear  # or same package name

# 1. Check Data Layer
adb -s $WATCH shell dumpsys wearable | head -50
adb shell dumpsys wearable | head -50

# 2. Check pairing
adb -s $WATCH shell dumpsys wearable | grep -E "Node|connected"
adb shell dumpsys wearable | grep -E "Node|connected"

# 3. Monitor sync logs
adb -s $WATCH logcat -v threadtime -s "SyncEngine" "DataLayerListenerService" "WearableDataSender" &
adb logcat -v threadtime -s "SyncEngine" "WearableListenerServiceImpl" "WearableSyncClient" &

# 4. Check watch database
adb -s $WATCH shell run-as $WATCH_PKG sqlite3 databases/watch_reminders.db "SELECT COUNT(*) FROM watch_reminders;"
```

### Sync Debugging Session (Phone + Watch)

```bash
# Terminal 1: Phone logcat
adb logcat -v threadtime -s "SyncEngine" "WearableSyncClient" "WearableDataSender" "WearableListenerServiceImpl" "SyncConflictResolver"

# Terminal 2: Watch logcat
adb -s <watch_serial> logcat -v threadtime -s "SyncEngine" "DataLayerListenerService" "WearDataLayerClient" "SyncConflictResolver"

# Terminal 3: Check state
adb shell run-as com.example.reminders sqlite3 databases/reminders.db "SELECT id, title, updated_at FROM reminders ORDER BY updated_at DESC LIMIT 10;"
adb -s <watch_serial> shell run-as com.example.reminders.wear sqlite3 databases/watch_reminders.db "SELECT id, title, updated_at FROM watch_reminders ORDER BY updated_at DESC LIMIT 10;"
```

### Common Diagnostic Commands

| Task | Command |
|------|---------|
| App PID | `adb shell pidof com.example.reminders` |
| App memory | `adb shell dumpsys meminfo com.example.reminders` |
| App permissions | `adb shell dumpsys package com.example.reminders \| grep granted` |
| Clear app data | `adb shell pm clear com.example.reminders` |
| Force stop | `adb shell am force-stop com.example.reminders` |
| Check database | `adb shell run-as com.example.reminders sqlite3 databases/reminders.db ".tables"` |
| Check alarms | `adb shell dumpsys alarm \| grep com.example.reminders` |
| Check network | `adb shell dumpsys connectivity` |
| Check location | `adb shell dumpsys location \| head -50` |
| Check notifications | `adb shell dumpsys notification \| grep com.example.reminders` |
| ANR traces | `adb shell cat /data/anr/traces.txt` |
| Clear logcat | `adb logcat -c` |
| Dump logcat | `adb logcat -d` |
| Device info | `adb shell getprop ro.build.version.release` |
| Screen record | `adb shell screenrecord /sdcard/debug.mp4` |
| Screenshot | `adb shell screencap /sdcard/screen.png && adb pull /sdcard/screen.png` |
| Input text | `adb shell input text "hello"` |
| Input tap | `adb shell input tap x y` |
| Input swipe | `adb shell input swipe x1 y1 x2 y2 duration_ms` |
| Battery level | `adb shell dumpsys battery` |
| Simulate low battery | `adb shell cmd battery set level 5` |
| Reset battery | `adb shell cmd battery reset` |
