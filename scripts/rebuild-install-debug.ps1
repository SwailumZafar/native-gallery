param(
    [string]$Device = ""
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$gradle = "C:\Users\Amazon\.gradle\wrapper\dists\gradle-9.0.0-bin\d6wjpkvcgsg3oed0qlfss3wgl\gradle-9.0.0\bin\gradle.bat"

$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA "Android\Sdk"

if (-not (Test-Path -LiteralPath $gradle)) {
    throw "Gradle was not found at $gradle."
}

Push-Location $repoRoot
try {
    Write-Host "Building debug APK..."
    & $gradle --no-daemon :app:assembleDebug

    $installScript = Join-Path $PSScriptRoot "install-debug-apk.ps1"
    & $installScript -Device $Device
} finally {
    Pop-Location
}

