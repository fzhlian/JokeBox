param(
    [string]$Version = "v0.1.1",
    [string]$BundleName = "fzhlian.JokeBox.app",
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

function Resolve-AppPackingTool([string]$signToolJar) {
    if ([string]::IsNullOrWhiteSpace($signToolJar)) {
        throw "SignToolJar is required to resolve app_packing_tool.jar"
    }
    $dir = Split-Path -Parent $signToolJar
    $tool = Join-Path $dir "app_packing_tool.jar"
    if (!(Test-Path $tool)) {
        throw "app_packing_tool.jar not found: $tool"
    }
    return $tool
}

function Export-HapPackInfo([string]$hapPath, [string]$packInfoPath) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem -ErrorAction SilentlyContinue
    $zip = [System.IO.Compression.ZipFile]::OpenRead($hapPath)
    try {
        $entry = $zip.Entries | Where-Object { $_.FullName -eq "pack.info" } | Select-Object -First 1
        if ($entry) {
            $reader = New-Object System.IO.StreamReader($entry.Open())
            $content = $reader.ReadToEnd()
            $reader.Close()
            if (-not [string]::IsNullOrWhiteSpace($content)) {
                $content | Set-Content -Path $packInfoPath -Encoding ASCII
                return
            }
        }
    } finally {
        $zip.Dispose()
    }
    "{}" | Set-Content -Path $packInfoPath -Encoding ASCII
}

function Ensure-UnsignedHapBundleName([string]$hapPath, [string]$bundleName) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem -ErrorAction SilentlyContinue
    $tmpRoot = Join-Path (Split-Path -Parent $hapPath) ("tmp_fix_bundle_" + [Guid]::NewGuid().ToString("N"))
    $unpackDir = Join-Path $tmpRoot "unpack"
    $stagingZip = Join-Path $tmpRoot "unsigned.zip"
    $rebuiltZip = Join-Path $tmpRoot "unsigned_rebuilt.zip"
    New-Item -ItemType Directory -Path $unpackDir -Force | Out-Null

    try {
        Copy-Item $hapPath $stagingZip -Force
        [System.IO.Compression.ZipFile]::ExtractToDirectory($stagingZip, $unpackDir)

        $changed = $false
        $modulePath = Join-Path $unpackDir "module.json"
        if (Test-Path $modulePath) {
            $moduleObj = Get-Content -Raw $modulePath | ConvertFrom-Json
            if ($moduleObj.app.bundleName -cne $bundleName) {
                $moduleObj.app.bundleName = $bundleName
                $changed = $true
            }
            $moduleObj | ConvertTo-Json -Depth 100 -Compress | Set-Content -Path $modulePath -Encoding ASCII
        }

        $packInfoPath = Join-Path $unpackDir "pack.info"
        if (Test-Path $packInfoPath) {
            $packObj = Get-Content -Raw $packInfoPath | ConvertFrom-Json
            if ($packObj.summary.app.bundleName -cne $bundleName) {
                $packObj.summary.app.bundleName = $bundleName
                $changed = $true
            }
            $packObj | ConvertTo-Json -Depth 100 -Compress | Set-Content -Path $packInfoPath -Encoding ASCII
        }

        if ($changed) {
            if (Test-Path $rebuiltZip) { Remove-Item $rebuiltZip -Force }
            [System.IO.Compression.ZipFile]::CreateFromDirectory(
                $unpackDir,
                $rebuiltZip,
                [System.IO.Compression.CompressionLevel]::Optimal,
                $false
            )
            Move-Item -Path $rebuiltZip -Destination $hapPath -Force
            Write-Output "Patched unsigned HAP bundleName -> $bundleName : $hapPath"
        }
    } finally {
        Remove-Item -Recurse -Force $tmpRoot -ErrorAction SilentlyContinue
    }
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

function Resolve-UnsignedHap([string]$projectDir) {
    $candidateA = Join-Path $projectDir "entry\build\default\outputs\default\app\entry-default.hap"
    $candidateB = Join-Path $projectDir "entry\build\default\outputs\default\entry-default-unsigned.hap"
    $existing = @($candidateA, $candidateB) | Where-Object { Test-Path $_ }
    if ($existing.Count -eq 0) { return $null }
    if ($existing.Count -eq 1) { return $existing[0] }

    $sorted = $existing |
        ForEach-Object { Get-Item $_ } |
        Sort-Object Length -Descending
    return $sorted[0].FullName
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
    $unsignedHap = $null
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
        $unsignedHap = Resolve-UnsignedHap -projectDir $projectDir
        if ($unsignedHap) {
            Write-Warning "hvigor build failed for $projectDir, using existing unsigned HAP: $unsignedHap"
        } else {
            throw
        }
    }
    if (-not $buildFailed) {
        $unsignedHap = Resolve-UnsignedHap -projectDir $projectDir
    }
    if ([string]::IsNullOrWhiteSpace($unsignedHap) -or !(Test-Path $unsignedHap)) {
        throw "Unsigned HAP not found after build in: $projectDir"
    }
    $unsignedSizeKb = [math]::Round((Get-Item $unsignedHap).Length / 1KB, 2)
    if ($unsignedSizeKb -lt 300) {
        Write-Warning "Unsigned HAP seems very small ($unsignedSizeKb KB): $unsignedHap"
    }
    Ensure-UnsignedHapBundleName -hapPath $unsignedHap -bundleName $BundleName

    $hapOut = Join-Path $outDir "JokeBox-$label-signed-$version-$stamp.hap"

    $params = @{
        ProjectDir = $projectDir
        UnsignedHap = $unsignedHap
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

    $appPath = Join-Path $outDir "JokeBox-$label-signed-$version-$stamp.app"
    if (Test-Path $appPath) { Remove-Item $appPath -Force }

    $packDir = Join-Path $outDir "tmp_pack_$label"
    if (Test-Path $packDir) { Remove-Item -Recurse -Force $packDir }
    New-Item -ItemType Directory -Path $packDir | Out-Null

    # Keep app inner hap filename aligned with pack.info package name (entry-default)
    $packHap = Join-Path $packDir "entry-default.hap"
    Copy-Item $hapOut $packHap -Force
    $packInfo = Join-Path $packDir "pack.info"
    Export-HapPackInfo -hapPath $packHap -packInfoPath $packInfo
    $appPackingTool = Resolve-AppPackingTool -signToolJar $signArgs["SignToolJar"]
    $javaExe = $signArgs["JavaPath"]
    if ([string]::IsNullOrWhiteSpace($javaExe)) {
        $javaCmd = Get-Command java -ErrorAction SilentlyContinue
        if (-not $javaCmd) { throw "Java runtime not found for app_packing_tool.jar" }
        $javaExe = $javaCmd.Source
    }

    & $javaExe -jar $appPackingTool `
        --mode app `
        --hap-path $packHap `
        --pack-info-path $packInfo `
        --out-path $appPath `
        --force true
    if ($LASTEXITCODE -ne 0 -or !(Test-Path $appPath)) {
        throw "app_packing_tool failed for $projectDir"
    }
    Remove-Item -Recurse -Force $packDir -ErrorAction SilentlyContinue

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
