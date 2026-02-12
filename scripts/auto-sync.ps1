param(
  [string]$RepoPath = "D:\fzhlian\Code\JokeBox",
  [string]$Branch = "main"
)

$ErrorActionPreference = "Stop"
$git = "C:\Program Files\Git\bin\git.exe"

if (!(Test-Path $git)) {
  throw "git not found at $git"
}

Set-Location $RepoPath

& $git add -A
$status = & $git status --porcelain
if ([string]::IsNullOrWhiteSpace(($status -join ""))) {
  & $git pull --rebase origin $Branch | Out-Null
  exit 0
}

$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
& $git commit -m "auto-sync: $timestamp"
& $git pull --rebase origin $Branch
& $git push origin $Branch
