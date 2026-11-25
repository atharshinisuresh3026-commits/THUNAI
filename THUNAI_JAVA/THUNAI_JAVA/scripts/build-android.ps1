. "$PSScriptRoot\_common.ps1"
$app = "$Root\android-app"
if (-not $env:ANDROID_HOME -and -not $env:ANDROID_SDK_ROOT) { Write-Error 'Set ANDROID_HOME to your Android SDK folder (installed by Android Studio).'; exit 1 }
if (Test-Path "$app\gradlew.bat") { Push-Location $app; try { .\gradlew.bat assembleDebug } finally { Pop-Location } }
elseif (Get-Command gradle -ErrorAction SilentlyContinue) { Push-Location $app; try { gradle assembleDebug } finally { Pop-Location } }
else { Write-Error "No Gradle wrapper yet. Easiest: open '$app' in Android Studio, let it sync (it fetches Gradle 8.7), then run Build > Build APK. After that this script can use gradlew.bat."; exit 1 }
if ($LASTEXITCODE -ne 0) { Write-Error 'Android build failed - read the Gradle errors above.'; exit 1 }
Write-Host "APK: $app\app\build\outputs\apk\debug\app-debug.apk"
