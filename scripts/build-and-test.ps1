<#
Local equivalent of .github/workflows/android.yml + desktop.yml, for Windows 11 + Android Studio's
SDK (no CI needed). Builds debug APKs and runs unit tests for each android/<App>; optionally also
Recetario's desktop target.

Usage:
  .\scripts\build-and-test.ps1                    # build + test every Android app
  .\scripts\build-and-test.ps1 -App FuelLog        # just one app
  .\scripts\build-and-test.ps1 -Desktop            # also run Recetario's :shared:desktopTest
  .\scripts\build-and-test.ps1 -Package            # also build the Windows .msi installer
#>
param(
    [string]$App = "",
    [switch]$Desktop,
    [switch]$Package
)

$ErrorActionPreference = "Stop"
if (-not $env:ANDROID_HOME) { $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk" }

$apps = Get-ChildItem (Join-Path $PSScriptRoot "..\android") -Directory |
    Where-Object { Test-Path (Join-Path $_.FullName "gradlew.bat") } |
    Select-Object -ExpandProperty Name
if ($App) { $apps = $apps | Where-Object { $_ -eq $App } }
if (-not $apps) { throw "No matching app under android/ with a gradlew.bat (got -App '$App')" }

foreach ($a in $apps) {
    Write-Host "`n=== $a : assembleDebug + testDebugUnitTest ===" -ForegroundColor Cyan
    Push-Location (Join-Path $PSScriptRoot "..\android\$a")
    try { ./gradlew.bat assembleDebug testDebugUnitTest --no-daemon }
    finally { Pop-Location }
}

if ($Desktop -or $Package) {
    Push-Location (Join-Path $PSScriptRoot "..\android\Recetario")
    try {
        if ($Desktop) {
            Write-Host "`n=== Recetario : desktopTest ===" -ForegroundColor Cyan
            ./gradlew.bat :shared:desktopTest --no-daemon
        }
        if ($Package) {
            Write-Host "`n=== Recetario : packageMsi (Windows installer) ===" -ForegroundColor Cyan
            ./gradlew.bat :shared:packageMsi --no-daemon
        }
    } finally { Pop-Location }
}
