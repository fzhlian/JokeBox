param(
    [string]$Version = "v0.1.1",
    [string]$BundleName = "fzhlian.jokebox.app",
    [string]$ReleaseDir = "release\upload",
    [switch]$SkipAndroid,
    [switch]$SkipHarmony,
    [switch]$SkipHarmonyNext,

    [string]$SignToolJar,
    [string]$P12Path,
    [string]$P12Password,
    [string]$AppCertFile,
    [string]$ProfileFile,
    [string]$AppKeyAlias = "release",
    [string]$AppKeyPassword,
    [string]$JavaPath,
    [string]$CompatibleVersion = "9"
)

$ErrorActionPreference = "Stop"

function Write-Sha256([string]$filePath) {
    $hash = (Get-FileHash -Algorithm SHA256 -Path $filePath).Hash.ToLowerInvariant()
    "$hash  $([IO.Path]::GetFileName($filePath))" | Set-Content -Path "$filePath.sha256" -Encoding ASCII
}

function Resolve-ApkSigner {
    $roots = @(
        "C:\Users\fzhlian\Android\Sdk\build-tools",
        "$env:LOCALAPPDATA\Android\Sdk\build-tools",
        "$env:ANDROID_HOME\build-tools"
    ) | Where-Object { $_ -and (Test-Path $_) }

    foreach ($root in $roots) {
        $candidate = Get-ChildItem -Directory $root -ErrorAction SilentlyContinue |
            Sort-Object Name -Descending |
            ForEach-Object { Join-Path $_.FullName "apksigner.bat" } |
            Where-Object { Test-Path $_ } |
            Select-Object -First 1
        if ($candidate) { return $candidate }
    }
    throw "apksigner.bat not found in Android SDK build-tools."
}

function Build-Android([string]$outDir, [string]$version, [string]$stamp) {
    Push-Location android
    try {
        if (Test-Path ".\gradlew.bat") {
            & .\gradlew.bat :app:assembleRelease
        } else {
            $gradleCmd = Get-Command gradle -ErrorAction SilentlyContinue
            if (-not $gradleCmd) {
                throw "Neither gradlew.bat nor gradle was found. Install Gradle or add Gradle Wrapper."
            }
            & $gradleCmd.Source :app:assembleRelease
        }
    } finally {
        Pop-Location
    }

    $apk = "android\app\build\outputs\apk\release\app-release.apk"
    if (!(Test-Path $apk)) {
        throw "Signed release APK not found: $apk"
    }

    $apksigner = Resolve-ApkSigner
    & $apksigner verify --verbose --print-certs $apk
    if ($LASTEXITCODE -ne 0) {
        throw "APK signature verification failed: $apk"
    }

    $target = Join-Path $outDir "JokeBox-Android-signed-$version-$stamp.apk"
    Copy-Item $apk $target -Force
    Write-Sha256 $target
    return $target
}

function Build-HarmonyLike(
    [string]$projectDir,
    [string]$label,
    [string]$outDir,
    [string]$version,
    [string]$stamp,
    [hashtable]$signArgs
) {
    $unsignedHap = Join-Path $projectDir "entry\build\default\outputs\default\entry-default-unsigned.hap"
    $buildFailed = $false
    try {
        Push-Location $projectDir
        try {
            if (Test-Path ".\hvigorw.bat") {
                & .\hvigorw.bat assembleApp
            } else {
                & hvigor assembleApp
            }
        } finally {
            Pop-Location
        }
    } catch {
        $buildFailed = $true
        if (Test-Path $unsignedHap) {
            Write-Warning "hvigor build failed for $projectDir, using existing unsigned HAP: $unsignedHap"
        } else {
            throw
        }
    }
    if (-not $buildFailed -and !(Test-Path $unsignedHap)) {
        throw "Unsigned HAP not found after build: $unsignedHap"
    }

    $hapOut = Join-Path $outDir "JokeBox-$label-signed-$version-$stamp.hap"

    $params = @{
        ProjectDir = $projectDir
        BundleName = $BundleName
        CompatibleVersion = $CompatibleVersion
        OutFile = $hapOut
    }
    foreach ($k in $signArgs.Keys) {
        if ($null -ne $signArgs[$k] -and $signArgs[$k] -ne "") { $params[$k] = $signArgs[$k] }
    }

    & "scripts\sign-harmony-manual.ps1" @params
    if ($LASTEXITCODE -ne 0 -or !(Test-Path $hapOut)) {
        throw "Signing failed for $projectDir"
    }

    $stage = Join-Path $outDir "tmp_app_$label"
    if (Test-Path $stage) { Remove-Item -Recurse -Force $stage }
    New-Item -ItemType Directory -Path $stage | Out-Null
    Copy-Item $hapOut (Join-Path $stage "entry-default-signed.hap")

    $zipPath = Join-Path $outDir "JokeBox-$label-signed-$version-$stamp.zip"
    if (Test-Path $zipPath) { Remove-Item $zipPath -Force }
    Compress-Archive -Path (Join-Path $stage "*") -DestinationPath $zipPath

    $appPath = Join-Path $outDir "JokeBox-$label-signed-$version-$stamp.app"
    if (Test-Path $appPath) { Remove-Item $appPath -Force }
    Move-Item $zipPath $appPath
    Remove-Item -Recurse -Force $stage

    Write-Sha256 $hapOut
    Write-Sha256 $appPath

    return @($hapOut, $appPath)
}

if ([string]::IsNullOrWhiteSpace($AppKeyPassword)) { $AppKeyPassword = $P12Password }

$stamp = Get-Date -Format "yyyyMMdd_HHmmss"
if (Test-Path $ReleaseDir) {
    Remove-Item -Recurse -Force $ReleaseDir
}
New-Item -ItemType Directory -Path $ReleaseDir -Force | Out-Null

$artifacts = @()

if (-not $SkipAndroid) {
    Write-Output "[Android] Building signed release APK..."
    $artifacts += Build-Android -outDir $ReleaseDir -version $Version -stamp $stamp
}

$signConfig = @{
    SignToolJar = $SignToolJar
    P12Path = $P12Path
    P12Password = $P12Password
    AppCertFile = $AppCertFile
    ProfileFile = $ProfileFile
    AppKeyAlias = $AppKeyAlias
    AppKeyPassword = $AppKeyPassword
    JavaPath = $JavaPath
}

if ((-not $SkipHarmony) -or (-not $SkipHarmonyNext)) {
    $missingSign = @()
    foreach ($required in @("SignToolJar","P12Path","P12Password","AppCertFile","ProfileFile")) {
        if ([string]::IsNullOrWhiteSpace($signConfig[$required])) { $missingSign += $required }
    }
    if ($missingSign.Count -gt 0) {
        throw "Harmony AppGallery signing parameters missing: $($missingSign -join ', ')"
    }
}

if (-not $SkipHarmony) {
    Write-Output "[Harmony] Building + signing + verifying..."
    $artifacts += Build-HarmonyLike -projectDir "harmony" -label "Harmony" -outDir $ReleaseDir -version $Version -stamp $stamp -signArgs $signConfig
}

if (-not $SkipHarmonyNext) {
    Write-Output "[Harmony NEXT] Building + signing + verifying..."
    $artifacts += Build-HarmonyLike -projectDir "harmony-next" -label "HarmonyNext" -outDir $ReleaseDir -version $Version -stamp $stamp -signArgs $signConfig
}

Write-Output "DONE: signed upload artifacts generated in $ReleaseDir"
$artifacts | ForEach-Object { Write-Output " - $_" }
