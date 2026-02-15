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

## Emulator Regression
- Build Android debug package:
  - `cd android`
  - `gradle :app:assembleDebug`
- Run one-click emulator smoke test (install + launch + refetch + log check):
  - `powershell -ExecutionPolicy Bypass -File scripts/test-android-emulator.ps1`

## Signed-Only Packaging
- App package/bundle name is now unified as: `fzhlian.jokebox.app`.
- Use one-click template and only fill certificate paths/passwords:
  - `powershell -ExecutionPolicy Bypass -File scripts/release-oneclick-template.ps1`
- Or run signed-only release directly:
  - `powershell -ExecutionPolicy Bypass -File scripts/release-signed-only.ps1 -Version v0.1.1 -SignToolJar <hap-sign-tool.jar> -P12Path <agc-release-key.p12> -P12Password <password> -AppCertFile <agc-release-cert.cer> -ProfileFile <agc-profile-release.p7b>`
- The release pipeline now enforces:
  - Android output must pass `apksigner verify`.
  - Harmony/Harmony NEXT output must pass `hap-sign-tool verify-app`.
  - Artifacts are generated to `release/upload/` with `-signed-` naming only.

