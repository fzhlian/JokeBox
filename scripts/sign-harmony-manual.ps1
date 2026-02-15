param(
    [Parameter(Mandatory = $true)][string]$ProjectDir,
    [string]$BundleName = "fzhlian.JokeBox.app",
    [string]$CompatibleVersion = "9",
    [Parameter(Mandatory = $true)][string]$SignToolJar,
    [Parameter(Mandatory = $true)][string]$P12Path,
    [Parameter(Mandatory = $true)][string]$P12Password,
    [Parameter(Mandatory = $true)][string]$ProfileCertChainPath,
    [string]$AppKeyAlias = "openharmony application release",
    [string]$AppKeyPassword,
    [string]$ProfileKeyAlias = "openharmony application profile release",
    [string]$ProfileKeyPassword,
    [string]$JavaPath,
    [string]$KeytoolPath,
    [string]$OutFile
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($AppKeyPassword)) { $AppKeyPassword = $P12Password }
if ([string]::IsNullOrWhiteSpace($ProfileKeyPassword)) { $ProfileKeyPassword = $P12Password }

if ([string]::IsNullOrWhiteSpace($JavaPath)) {
    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCmd) { $JavaPath = $javaCmd.Source }
}
if ([string]::IsNullOrWhiteSpace($KeytoolPath)) {
    $keytoolCmd = Get-Command keytool -ErrorAction SilentlyContinue
    if ($keytoolCmd) { $KeytoolPath = $keytoolCmd.Source }
}
if ([string]::IsNullOrWhiteSpace($JavaPath) -and $env:JAVA_HOME) {
    $candidate = Join-Path $env:JAVA_HOME "bin\java.exe"
    if (Test-Path $candidate) { $JavaPath = $candidate }
}
if ([string]::IsNullOrWhiteSpace($KeytoolPath) -and $env:JAVA_HOME) {
    $candidate = Join-Path $env:JAVA_HOME "bin\keytool.exe"
    if (Test-Path $candidate) { $KeytoolPath = $candidate }
}

$requiredFiles = @(
    @{ Name = "ProjectDir"; Value = $ProjectDir },
    @{ Name = "SignToolJar"; Value = $SignToolJar },
    @{ Name = "P12Path"; Value = $P12Path },
    @{ Name = "ProfileCertChainPath"; Value = $ProfileCertChainPath },
    @{ Name = "JavaPath"; Value = $JavaPath },
    @{ Name = "KeytoolPath"; Value = $KeytoolPath }
)
foreach ($item in $requiredFiles) {
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

$projectTag = ($ProjectDir -replace '[^a-zA-Z0-9_-]', '_')
$tmp = Join-Path "release" "tmp_sign_$projectTag"
New-Item -ItemType Directory -Path $tmp -Force | Out-Null

$appCer = Join-Path $tmp "app_release.cer"
$profileJson = Join-Path $tmp "$BundleName.profile.json"
$profileP7b = Join-Path $tmp "$BundleName.profile.p7b"

& $KeytoolPath -exportcert -rfc -alias $AppKeyAlias -storetype PKCS12 -keystore $P12Path -storepass $P12Password -file $appCer | Out-Null
if (!(Test-Path $appCer)) {
    throw "Failed to export app cert from keystore alias '$AppKeyAlias'."
}

$certB64 = ((Get-Content $appCer | Where-Object { $_ -notmatch "BEGIN CERTIFICATE|END CERTIFICATE" }) -join "")
$notBefore = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$notAfter = [DateTimeOffset]::UtcNow.AddYears(3).ToUnixTimeSeconds()

@"
{
  "version-name": "1.0.0",
  "version-code": 1,
  "type": "release",
  "bundle-info": {
    "developer-id": "OpenHarmony",
    "distribution-certificate": "$certB64",
    "bundle-name": "$BundleName",
    "apl": "normal",
    "app-feature": "hos_normal_app"
  },
  "validity": {
    "not-before": $notBefore,
    "not-after": $notAfter
  },
  "permissions": { "restricted-permissions": [] },
  "acls": { "allowed-acls": [""] },
  "issuer": "pki_internal"
}
"@ | Out-File -FilePath $profileJson -Encoding utf8

& $JavaPath -jar $SignToolJar sign-profile `
    -mode localSign `
    -keyAlias $ProfileKeyAlias `
    -keyPwd $ProfileKeyPassword `
    -profileCertFile $ProfileCertChainPath `
    -inFile $profileJson `
    -signAlg SHA256withECDSA `
    -keystoreFile $P12Path `
    -keystorePwd $P12Password `
    -outFile $profileP7b

if (!(Test-Path $profileP7b)) {
    throw "sign-profile failed, output not found: $profileP7b"
}

& $JavaPath -jar $SignToolJar sign-app `
    -mode localSign `
    -keyAlias $AppKeyAlias `
    -keyPwd $AppKeyPassword `
    -appCertFile $appCer `
    -profileFile $profileP7b `
    -inFile $unsignedHap `
    -signAlg SHA256withECDSA `
    -keystoreFile $P12Path `
    -keystorePwd $P12Password `
    -outFile $OutFile `
    -compatibleVersion $CompatibleVersion

if (!(Test-Path $OutFile)) {
    throw "sign-app failed, output not found: $OutFile"
}

$verifyOutput = & $JavaPath -jar $SignToolJar verify-app -inFile $OutFile 2>&1
$verifyText = ($verifyOutput | Out-String)
if ($LASTEXITCODE -ne 0) {
    Write-Output $verifyText
    throw "Signed HAP verification failed for: $OutFile"
}

Write-Output "SIGNED_HAP: $OutFile"
