---
page_ref: /docs/apps/termux/battery-usage-profile.html
---

# Termux Battery Usage Profile

This document describes the battery usage profile of the Termux app based on the current
implementation, and the background-idle mode that reduces app-layer battery drain when the app
is sent to the background. The figures here are derived from the code paths described below, not
from physical hardware power measurements.

## Battery usage drivers

### Foreground service defeats Doze

`TermuxService` runs as a foreground service (`startForeground`) for the entire time any terminal
session or background task exists. A foreground service keeps the process schedulable and prevents
the system from placing the app into Doze, so the app keeps a baseline cost even when idle. This is
intentional: it is what keeps the user's shell sessions and background tasks alive across activity
lifecycle events.

### Optional wake lock and Wi-Fi lock

When the user (or a plugin) activates the wake lock via the `ACTION_WAKE_LOCK` intent,
`actionAcquireWakeLock()` acquires two locks at once:

- A `PowerManager.PARTIAL_WAKE_LOCK`, which keeps the CPU running at full speed even when the screen
  is off.
- A `WifiManager.WIFI_MODE_FULL_HIGH_PERF` Wi-Fi lock, which prevents the Wi-Fi radio from entering
  power-save mode.

It also calls `requestDisableBatteryOptimizations()`, opting the app out of Doze entirely. These
locks are the single largest discretionary battery driver because they prevent both the CPU and the
Wi-Fi radio from sleeping. Before this change, the locks were held continuously until the user
explicitly released them or the service was destroyed, including while the app was in the
background.

### Idle shell session baseline

Each terminal session and background task is a running process that continues regardless of whether
the activity is in the foreground. A shell that is blocked waiting for input consumes negligible
CPU, but any active process (for example `top`, a build, or a polling script) continues to run at
full speed in the background. The maximum number of terminal sessions is 32, so the worst-case
baseline is the sum of up to 32 concurrently running processes plus their memory overhead.

### Planned in-app WebView browser (not yet measured)

The planned in-app WebView browser would add a JavaScript engine, DOM rendering, and potentially
persistent network connections per browser tab per session. With up to roughly 10 tabs per session
and up to 32 sessions, this would add CPU and radio load whenever any session with an open tab is in
the background. The exact CPU and radio cost depends on the WebView browser implementation, which is
tracked separately and is not yet built, so it cannot be characterized from the current code.

## Background-idle mode

When `TermuxActivity` enters the stopped state (`onStop()`), the app now reduces app-layer work:

- If the wake lock and Wi-Fi lock were held, they are automatically released
  (`TermuxService.onActivityBackgrounded()`), allowing the CPU and Wi-Fi radio to return to
  power-save while the app is backgrounded. The fact that they were held is remembered so they can
  be restored later.
- Terminal rendering updates are already suppressed while the activity is not visible: the session
  client's `onTextChanged()` returns early when `isVisible()` is false, so no screen redraw work is
  performed for background output.
- The bell sound pool is released on stop, since the bell is not played in the background.

When `TermuxActivity` returns to the foreground (`onStart()`),
`TermuxService.onActivityForegrounded()` re-acquires the wake lock and Wi-Fi lock only if they were
auto-released for background-idle and are not currently held. A wake lock that the user manually
released via the notification (`ACTION_WAKE_UNLOCK`) is not re-acquired, preserving user intent.

### What background-idle mode does not do

- Running shell sessions and background tasks are never killed. Background-idle mode reduces the work
  done by the app layer only; it does not interrupt the user's terminal processes.
- The foreground service is not stopped. Sessions and tasks continue to be kept alive, so the
  baseline foreground-service cost remains while sessions or tasks exist.
- `requestDisableBatteryOptimizations()` is still requested only when the wake lock is explicitly
  acquired, not eagerly at app start.
