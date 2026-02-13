# JokeBox

JokeBox is a multi-source joke app scaffold with:
- age-group onboarding and compliance confirmation
- async fetch/import/process pipeline
- content policy filtering, exact and near dedup
- playback controls, favorites, and TTS
- Android implementation + Harmony/Harmony NEXT projects

## Structure
- `android/` Android app (Kotlin, Compose, Room, DataStore, WorkManager)
- `harmony/` HarmonyOS Stage project (ArkUI + RDB)
- `harmony-next/` HarmonyOS NEXT Stage project (ArkUI + RDB)

## Status
Core Android flow is implemented and runnable in emulator.
Harmony and Harmony NEXT now contain full Stage-model project structures with runnable ArkUI pages and RDB initialization.

## Auto Sync (Hidden Window)
- Use `scripts/auto-sync-hidden.vbs` to run Git auto-sync without showing a PowerShell window.
- For Task Scheduler, set action to:
  - Program/script: `wscript.exe`
  - Add arguments: `"D:\fzhlian\Code\JokeBox\scripts\auto-sync-hidden.vbs"`
- Or run:
  - `powershell -ExecutionPolicy Bypass -File scripts/register-auto-sync-task.ps1`
