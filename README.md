# JokeBox

JokeBox is a multi-source joke app scaffold with:
- age-group onboarding and compliance confirmation
- async fetch/import/process pipeline
- content policy filtering, exact and near dedup
- playback controls, favorites, and TTS
- Android implementation + Harmony/Harmony NEXT aligned scaffolds

## Structure
- `android/` Android app (Kotlin, Compose, Room, DataStore, WorkManager)
- `harmony/` HarmonyOS ArkTS scaffolds
- `harmony-next/` HarmonyOS NEXT ArkTS scaffolds

## Status
Core Android flow is implemented and runnable in emulator.
Harmony and Harmony NEXT include aligned data/pipeline/UI state templates for continued implementation.

## Auto Sync (Hidden Window)
- Use `scripts/auto-sync-hidden.vbs` to run Git auto-sync without showing a PowerShell window.
- For Task Scheduler, set action to:
  - Program/script: `wscript.exe`
  - Add arguments: `"D:\fzhlian\Code\JokeBox\scripts\auto-sync-hidden.vbs"`
- Or run:
  - `powershell -ExecutionPolicy Bypass -File scripts/register-auto-sync-task.ps1`
