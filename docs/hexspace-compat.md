# HexSpace Android 8.0–16 Compatibility Matrix (M0, Part 7 & Part 23)

This reference records per-API-level runtime behaviors and clean-room container design constraints for **M0 HexSpace** (Tier 0 rootless process hosting):

| Android Version (API) | Key Runtime / Sandbox Behavior | HexSpace Clean-Room Strategy |
|---|---|---|
| Android 8.0–8.1 (API 26–27) | Treble binder namespace isolation; hidden API warnings begin in P | Standard `ServiceManager` cache proxy + `Application` context wrapper |
| Android 9–10 (API 28–29) | Strict hidden-API greylists/blacklists; scoped storage intro | Target-aware reflection bypass only inside hosted child process; virtual storage under app data |
| Android 11 (API 30) | Scoped storage enforcement (`Android/data` restrictions); wireless ADB pairing | SAF bridge for external files + virtualized `/sdcard/Android/data` inside HexSpace |
| Android 12–13 (API 31–33) | Phantom Process Killer (32 child-process cap); `PendingIntent` mutability | Single-worker child process pooling + explicit `FLAG_IMMUTABLE`/`MUTABLE` proxy guards |
| Android 14 (API 34) | `FOREGROUND_SERVICE_DATA_SYNC` / `specialUse` enforcement; read-only DEX loading (`DCL`) | All native/DEX payloads bundled in APK (Play-compliant); NightShift FGS typed |
| Android 15–16 (API 35–36) | 16 KB page-size alignment for native `.so` libs; stricter `Intent` & background launch rules | Build native `.so` with 16 KB ELF alignment (`-Wl,-z,max-page-size=16384`) |

## Known Fragility Toggles
- **Compatibility Mode**: Disables aggressive Binder service proxies for engines that perform minimal GMS checks.
- **Crash Containment**: Child `SIGSEGV`/`SIGABRT` signals are trapped and recorded in `M9 Chronicle` without destabilizing the host `io.hexforge` UI process.
