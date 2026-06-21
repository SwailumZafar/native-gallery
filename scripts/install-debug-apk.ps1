param(
    [string]$Device = ""
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
$apk = Join-Path $repoRoot "app\build\outputs\apk\debug\app-debug.apk"

if (-not (Test-Path -LiteralPath $adb)) {
    throw "ADB was not found at $adb. Install Android platform tools or Android Studio."
}

if (-not (Test-Path -LiteralPath $apk)) {
    throw "Debug APK was not found at $apk. Run scripts\rebuild-install-debug.ps1 first."
}

$deviceArgs = @()
if ($Device.Trim().Length -gt 0) {
    $deviceArgs = @("-s", $Device)
}

Write-Host "Checking connected devices..."
& $adb devices

Write-Host "Installing debug APK..."
& $adb @deviceArgs install -r -d $apk

Write-Host "Launching Native Gallery..."
& $adb @deviceArgs shell monkey -p com.example.nativegallery 1

