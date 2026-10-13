<#
Local equivalent of .github/workflows/android.yml + desktop.yml, for Windows 11 + Android Studio's
SDK (no CI needed). Builds debug APKs and runs unit tests for each android/<App>; optionally also
Recetario's desktop target. Uses the Gradle daemon (unlike CI's --no-daemon): repeat local runs are
much faster once it's warm; pass -NoDaemon for a CI-faithful cold run.

Usage:
  .\scripts\build-and-test.ps1                    # build + test every Android app
  .\scripts\build-and-test.ps1 -App FuelLog        # just one app
  .\scripts\build-and-test.ps1 -Desktop            # also run Recetario's :shared:desktopTest
  .\scripts\build-and-test.ps1 -Package            # also build the Windows .msi installer
  .\scripts\build-and-test.ps1 -Install -App FuelLog  # also adb-install + launch on a running device/emulator
  .\scripts\build-and-test.ps1 -NoDaemon           # cold build, same flags CI uses
#>
param(
    [string]$App = "",
    [switch]$Desktop,
    [switch]$Package,
    [switch]$Install,
    [switch]$NoDaemon
)

$ErrorActionPreference = "Stop"
if (-not $env:ANDROID_HOME) { $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk" }
$daemonFlag = if ($NoDaemon) { "--no-daemon" } else { "" }

$apps = Get-ChildItem (Join-Path $PSScriptRoot "..\android") -Directory |
    Where-Object { Test-Path (Join-Path $_.FullName "gradlew.bat") } |
    Select-Object -ExpandProperty Name
if ($App) { $apps = $apps | Where-Object { $_ -eq $App } }
if (-not $apps) { throw "No matching app under android/ with a gradlew.bat (got -App '$App')" }

foreach ($a in $apps) {
    Write-Host "`n=== $a : assembleDebug + testDebugUnitTest ===" -ForegroundColor Cyan
    Push-Location (Join-Path $PSScriptRoot "..\android\$a")
    try {
        if ($daemonFlag) { ./gradlew.bat assembleDebug testDebugUnitTest $daemonFlag }
        else { ./gradlew.bat assembleDebug testDebugUnitTest }
        if ($Install) {
            $apk = Get-ChildItem "app\build\outputs\apk\debug\*.apk" | Select-Object -First 1
            $pkg = (Select-String -Path "app\build.gradle.kts" -Pattern 'applicationId\s*=\s*"([^"]+)"').Matches[0].Groups[1].Value
            Write-Host "--- installing $($apk.Name) on the running device/emulator ---" -ForegroundColor Cyan
            & "$env:ANDROID_HOME\platform-tools\adb.exe" install -r $apk.FullName
            & "$env:ANDROID_HOME\platform-tools\adb.exe" shell monkey -p $pkg -c android.intent.category.LAUNCHER 1
        }
    } finally { Pop-Location }
}

if ($Desktop -or $Package) {
    Push-Location (Join-Path $PSScriptRoot "..\android\Recetario")
    try {
        if ($Desktop) {
            Write-Host "`n=== Recetario : desktopTest ===" -ForegroundColor Cyan
            if ($daemonFlag) { ./gradlew.bat :shared:desktopTest $daemonFlag } else { ./gradlew.bat :shared:desktopTest }
        }
        if ($Package) {
            Write-Host "`n=== Recetario : packageMsi (Windows installer) ===" -ForegroundColor Cyan
            if ($daemonFlag) { ./gradlew.bat :shared:packageMsi $daemonFlag } else { ./gradlew.bat :shared:packageMsi }
        }
    } finally { Pop-Location }
}
