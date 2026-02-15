param(
    [Parameter(Mandatory = $true)][string]$ProjectDir,
    [string]$BundleName = "fzhlian.jokebox.app",
    [string]$CompatibleVersion = "9",
    [Parameter(Mandatory = $true)][string]$SignToolJar,
    [Parameter(Mandatory = $true)][string]$P12Path,
    [Parameter(Mandatory = $true)][string]$P12Password,
    [Parameter(Mandatory = $true)][string]$AppCertFile,
    [Parameter(Mandatory = $true)][string]$ProfileFile,
    [string]$AppKeyAlias,
    [string]$AppKeyPassword,
    [string]$JavaPath,
    [string]$OutFile
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($AppKeyAlias)) {
    $AppKeyAlias = "release"
}
if ([string]::IsNullOrWhiteSpace($AppKeyPassword)) {
    $AppKeyPassword = $P12Password
}

if ([string]::IsNullOrWhiteSpace($JavaPath)) {
    $candidate = "C:\Users\fzhlian\tools\jdk-17.0.14+7\bin\java.exe"
    if (Test-Path $candidate) {
        $JavaPath = $candidate
    } else {
        $javaCmd = Get-Command java -ErrorAction SilentlyContinue
        if ($javaCmd) { $JavaPath = $javaCmd.Source }
    }
}

$requiredPaths = @(
    @{ Name = "ProjectDir"; Value = $ProjectDir },
    @{ Name = "SignToolJar"; Value = $SignToolJar },
    @{ Name = "P12Path"; Value = $P12Path },
    @{ Name = "AppCertFile"; Value = $AppCertFile },
    @{ Name = "ProfileFile"; Value = $ProfileFile },
    @{ Name = "JavaPath"; Value = $JavaPath }
)
foreach ($item in $requiredPaths) {
    if ([string]::IsNullOrWhiteSpace($item.Value) -or !(Test-Path $item.Value)) {
        throw "Missing required path: $($item.Name) = '$($item.Value)'"
    }
}

$unsignedHap = Join-Path $ProjectDir "entry\build\default\outputs\default\entry-default-unsigned.hap"
if (!(Test-Path $unsignedHap)) {
    throw "Unsigned HAP not found: $unsignedHap. Run hvigor assembleApp first."
}

if ([string]::IsNullOrWhiteSpace($OutFile)) {
    $OutFile = Join-Path $ProjectDir "entry\build\default\outputs\default\entry-default-signed.hap"
}

$signOutput = & $JavaPath -jar $SignToolJar sign-app `
    -mode localSign `
    -keyAlias $AppKeyAlias `
    -keyPwd $AppKeyPassword `
    -appCertFile $AppCertFile `
    -profileFile $ProfileFile `
    -inFile $unsignedHap `
    -signAlg SHA256withECDSA `
    -keystoreFile $P12Path `
    -keystorePwd $P12Password `
    -outFile $OutFile `
    -compatibleVersion $CompatibleVersion 2>&1

$signOutput | ForEach-Object { Write-Output $_ }

if (!(Test-Path $OutFile)) {
    throw "sign-app failed, output not found: $OutFile"
}

$tmp = Join-Path "release" ("tmp_verify_" + ($ProjectDir -replace '[^a-zA-Z0-9_-]', '_'))
New-Item -ItemType Directory -Path $tmp -Force | Out-Null
$outCertChain = Join-Path $tmp "certchain.cer"
$outProfile = Join-Path $tmp "profile.p7b"

$verifyOutput = & $JavaPath -jar $SignToolJar verify-app `
    -inFile $OutFile `
    -outCertChain $outCertChain `
    -outProfile $outProfile 2>&1

$verifyOutput | ForEach-Object { Write-Output $_ }

if ($LASTEXITCODE -ne 0) {
    throw "Signed HAP verification failed for: $OutFile"
}

# Verify profile details for bundleName match (defense against wrong AGC profile)
$profileJson = Join-Path $tmp "verify_profile.json"
& $JavaPath -jar $SignToolJar verify-profile -inFile $outProfile -outFile $profileJson | Out-Null
if (Test-Path $profileJson) {
    $text = Get-Content -Raw $profileJson
    if ($text -notmatch [regex]::Escape($BundleName)) {
        throw "Profile bundle name mismatch. Expected: $BundleName"
    }
}

Write-Output "SIGNED_HAP: $OutFile"
