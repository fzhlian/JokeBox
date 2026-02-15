$ErrorActionPreference = "Stop"

# Fill only this section, then run this script.
$cfg = @{
    Version = "v0.1.1"
    BundleName = "fzhlian.JokeBox.app"

    # Required for Harmony/Harmony NEXT signing
    SignToolJar = "C:\\Path\\to\\hap-sign-tool.jar"
    P12Path = "C:\\Path\\to\\your-release.p12"
    P12Password = "<P12_PASSWORD>"
    ProfileCertChainPath = "C:\\Path\\to\\your-profile-release.pem"

    # Optional: keep default if your aliases are standard
    AppKeyAlias = "openharmony application release"
    AppKeyPassword = ""
    ProfileKeyAlias = "openharmony application profile release"
    ProfileKeyPassword = ""

    # Optional: leave empty to auto-detect from JAVA_HOME/PATH
    JavaPath = ""
    KeytoolPath = ""

    CompatibleVersion = "9"
    ReleaseDir = "release\\upload"
}

powershell -ExecutionPolicy Bypass -File "scripts\\release-signed-only.ps1" @cfg
