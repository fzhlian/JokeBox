param(
    [string]$Version = "v0.1.1"
)

$ErrorActionPreference = "Stop"

# Keep P12 password out of source. Set once in user env:
# [Environment]::SetEnvironmentVariable("JOKEBOX_P12_PASSWORD", "<your-password>", "User")
# Current request value has been written by assistant.
$p12PasswordFromEnv = $env:JOKEBOX_P12_PASSWORD
if ([string]::IsNullOrWhiteSpace($p12PasswordFromEnv)) {
    $p12PasswordFromEnv = [Environment]::GetEnvironmentVariable("JOKEBOX_P12_PASSWORD", "User")
}
if ([string]::IsNullOrWhiteSpace($p12PasswordFromEnv)) {
    throw "Environment variable JOKEBOX_P12_PASSWORD is empty. Set it before running."
}

# Fill only this section, then run this script.
$cfg = @{
    Version = $Version
    BundleName = "fzhlian.jokebox.app"

    # Required for Harmony/Harmony NEXT AppGallery signing
    SignToolJar = "C:\Users\fzhlian\AppData\Local\Huawei\Sdk\openharmony\9\toolchains\lib\hap-sign-tool.jar"
    P12Path = "D:\fzhlian\Code\JokeBox\release\JokeBox.p12"
    P12Password = $p12PasswordFromEnv
    AppCertFile = "D:\fzhlian\Code\JokeBox\release\fzhlian.jokebox.app.cer"
    ProfileFile = "D:\fzhlian\Code\JokeBox\release\JokeBoxRelease.p7b"

    # Alias/password for private key in P12
    AppKeyAlias = "ul"
    AppKeyPassword = ""

    # Optional: keep empty to auto detect
    JavaPath = "C:\Users\fzhlian\tools\jdk-17.0.14+7\bin\java.exe"
    CompatibleVersion = "9"

    ReleaseDir = "release\upload"
}

& "scripts\release-signed-only.ps1" -SkipAndroid @cfg
