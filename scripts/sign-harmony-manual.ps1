$ErrorActionPreference = "Stop"

param(
    [string]$ProjectDir = "harmony",
    [string]$BundleName = "com.jokebox.harmony",
    [string]$CompatibleVersion = "9"
)

$java = "C:\Users\fzhlian\tools\jdk-17.0.14+7\bin\java.exe"
$keytool = "C:\Users\fzhlian\tools\jdk-17.0.14+7\bin\keytool.exe"
$signTool = "C:\Users\fzhlian\AppData\Local\Huawei\Sdk\9\toolchains\lib\hap-sign-tool.jar"
$p12 = "C:\Users\fzhlian\AppData\Local\Huawei\Sdk\9\toolchains\lib\OpenHarmony.p12"
$profileCertChain = "C:\Users\fzhlian\AppData\Local\Huawei\Sdk\9\toolchains\lib\OpenHarmonyProfileRelease.pem"

if (!(Test-Path $java) -or !(Test-Path $keytool) -or !(Test-Path $signTool) -or !(Test-Path $p12)) {
    throw "Missing signing tools or OpenHarmony materials under SDK/toolchains/lib."
}

$unsignedHap = Join-Path $ProjectDir "entry\build\default\outputs\default\entry-default-unsigned.hap"
if (!(Test-Path $unsignedHap)) {
    throw "Unsigned HAP not found: $unsignedHap. Build with hvigor assembleApp first."
}

$tmp = Join-Path "release" "tmp_sign_$($ProjectDir -replace '[^a-zA-Z0-9_-]','_')"
New-Item -ItemType Directory -Path $tmp -Force | Out-Null

$appCer = Join-Path $tmp "openharmony_app_release.cer"
& $keytool -exportcert -rfc -alias "openharmony application release" -storetype PKCS12 -keystore $p12 -storepass 123456 -file $appCer | Out-Null

$certB64 = ((Get-Content $appCer | Where-Object { $_ -notmatch "BEGIN CERTIFICATE|END CERTIFICATE" }) -join "")
$now = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$after = [DateTimeOffset]::UtcNow.AddYears(3).ToUnixTimeSeconds()
$profileJson = Join-Path $tmp "$BundleName.profile.json"
$profileP7b = Join-Path $tmp "$BundleName.profile.p7b"

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
    "not-before": $now,
    "not-after": $after
  },
  "permissions": { "restricted-permissions": [] },
  "acls": { "allowed-acls": [""] },
  "issuer": "pki_internal"
}
"@ | Out-File -FilePath $profileJson -Encoding utf8

Write-Output "Signing profile..."
& $java -jar $signTool sign-profile `
    -mode localSign `
    -keyAlias "openharmony application profile release" `
    -keyPwd 123456 `
    -profileCertFile $profileCertChain `
    -inFile $profileJson `
    -signAlg SHA256withECDSA `
    -keystoreFile $p12 `
    -keystorePwd 123456 `
    -outFile $profileP7b

$signedHap = Join-Path $ProjectDir "entry\build\default\outputs\default\entry-default-manual-signed.hap"
Write-Output "Signing app..."
& $java -jar $signTool sign-app `
    -mode localSign `
    -keyAlias "openharmony application release" `
    -keyPwd 123456 `
    -appCertFile $appCer `
    -profileFile $profileP7b `
    -inFile $unsignedHap `
    -signAlg SHA256withECDSA `
    -keystoreFile $p12 `
    -keystorePwd 123456 `
    -outFile $signedHap `
    -compatibleVersion $CompatibleVersion

if (Test-Path $signedHap) {
    Write-Output "SIGNED_HAP: $signedHap"
    exit 0
}

throw "Manual sign did not produce output: $signedHap"
