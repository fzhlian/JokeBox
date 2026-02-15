param(
    [string]$Version = "v0.1.1"
)

$ErrorActionPreference = "Stop"

# Fill only this section, then run this script.
$cfg = @{
    Version = $Version
    BundleName = "fzhlian.jokebox.app"

    # Required for Harmony/Harmony NEXT AppGallery signing
    SignToolJar = "C:\Users\fzhlian\AppData\Local\Huawei\Sdk\9\toolchains\lib\hap-sign-tool.jar"
    P12Path = "C:\Path\to\agc-release-key.p12"
    P12Password = "<P12_PASSWORD>"
    AppCertFile = "C:\Path\to\agc-release-cert.cer"
    ProfileFile = "C:\Path\to\agc-profile-release.p7b"

    # Alias/password for private key in P12
    AppKeyAlias = "release"
    AppKeyPassword = ""

    # Optional: keep empty to auto detect
    JavaPath = "C:\Users\fzhlian\tools\jdk-17.0.14+7\bin\java.exe"
    CompatibleVersion = "9"

    ReleaseDir = "release\upload"
}

powershell -ExecutionPolicy Bypass -File "scripts\release-signed-only.ps1" @cfg
