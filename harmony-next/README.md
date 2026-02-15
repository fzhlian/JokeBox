# HarmonyOS NEXT Project

This directory now contains a full Stage-model HarmonyOS NEXT project scaffold:
- App-level files: `AppScope/app.json5`, `build-profile.json5`, `hvigorfile.ts`, `oh-package.json5`
- Entry module files: `entry/src/main/module.json5`, `entry/src/main/ets/entryability/EntryAbility.ets`
- ArkUI + RDB pages and data access:
  - `entry/src/main/ets/pages/Index.ets`
  - `entry/src/main/ets/data/RdbStore.ets`
  - `entry/src/main/ets/data/Repositories.ets`
  - `entry/src/main/ets/model/Models.ets`

## Build (DevEco Studio)
1. Open the `harmony-next/` directory in DevEco Studio.
2. Let the IDE sync dependencies.
3. Build HAP: `Build > Build Hap(s)/APP(s) > Build Hap(s)`.
4. Package APP (if needed): `Build > Build Hap(s)/APP(s) > Build APP(s)`.

## Notes
- Current bundle name: `fzhlian.jokebox.app`.
- Version: `0.1.1` (code `2`).


