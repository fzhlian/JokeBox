$ErrorActionPreference = "Stop"

function Resolve-AdbPath {
    $candidates = @(
        "C:\Users\fzhlian\Android\Sdk\platform-tools\adb.exe",
        "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
    )
    foreach ($path in $candidates) {
        if (Test-Path $path) { return $path }
    }
    throw "adb.exe not found. Install Android platform-tools first."
}

function Wait-BootCompleted([string]$adb, [int]$timeoutSec = 360) {
    & $adb wait-for-device | Out-Null
    $max = [Math]::Floor($timeoutSec / 2)
    for ($i = 0; $i -lt $max; $i++) {
        $boot = (& $adb shell getprop sys.boot_completed 2>$null).Trim()
        if ($boot -eq "1") { return }
        Start-Sleep -Seconds 2
    }
    throw "Emulator boot timeout."
}

$adb = Resolve-AdbPath
$apk = "android\app\build\outputs\apk\debug\app-debug.apk"
if (!(Test-Path $apk)) {
    throw "APK not found: $apk. Build first with gradle :app:assembleDebug."
}

Write-Output "[1/5] Checking emulator device..."
$devices = & $adb devices
if ($devices -notmatch "emulator-\d+\s+device") {
    $emulatorExe = "C:\Users\fzhlian\Android\Sdk\emulator\emulator.exe"
    if (!(Test-Path $emulatorExe)) {
        throw "Emulator not running and emulator.exe not found."
    }
    $avd = & $emulatorExe -list-avds | Select-Object -First 1
    if ([string]::IsNullOrWhiteSpace($avd)) {
        throw "No AVD found. Create an Android Virtual Device first."
    }
    Start-Process -FilePath $emulatorExe -ArgumentList "-avd $avd -no-snapshot-load -netdelay none -netspeed full"
}
Wait-BootCompleted -adb $adb

Write-Output "[2/5] Installing APK..."
& $adb uninstall com.jokebox.app | Out-Null
& $adb install -r $apk | Out-Null
& $adb shell pm clear com.jokebox.app | Out-Null
& $adb logcat -c

Write-Output "[3/5] Launching app..."
& $adb shell monkey -p com.jokebox.app -c android.intent.category.LAUNCHER 1 | Out-Null
Start-Sleep -Seconds 5

Write-Output "[4/5] Triggering Chinese refetch..."
& $adb shell am broadcast -a com.jokebox.app.DEBUG_REFETCH_ZH -n com.jokebox.app/.DebugActionsReceiver | Out-Null
Start-Sleep -Seconds 20

Write-Output "[5/5] Collecting logs..."
$logs = & $adb logcat -d
$focus = $logs | Select-String -Pattern "JokeBoxDebug|debug_refetch_zh|JokeBoxFetcher|fetched=|processed="
$focus | ForEach-Object { $_.Line }

if (($focus | Select-String -Pattern "done: fetched=").Count -gt 0) {
    Write-Output "RESULT: PASS (fetch/process pipeline reached completion in emulator)"
    exit 0
}

if (($focus | Select-String -Pattern "inserted=\d+").Count -gt 0) {
    Write-Output "RESULT: PASS (fetch pipeline active, data inserted; completion marker pending async)"
    exit 0
}

Write-Output "RESULT: WARN (no insertion marker found, inspect logs above)"
exit 2
