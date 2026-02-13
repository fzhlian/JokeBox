Option Explicit

Dim fso, shell, scriptDir, repoDir, psFile, cmd
Set fso = CreateObject("Scripting.FileSystemObject")
Set shell = CreateObject("WScript.Shell")

scriptDir = fso.GetParentFolderName(WScript.ScriptFullName)
repoDir = fso.GetParentFolderName(scriptDir)
psFile = scriptDir & "\auto-sync.ps1"

cmd = "powershell.exe -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File """ & psFile & """ -RepoPath """ & repoDir & """"
shell.Run cmd, 0, False
