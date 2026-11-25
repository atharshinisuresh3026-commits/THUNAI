. "$PSScriptRoot\_common.ps1"
Require-Tool java 'Install JDK 17+.'
$jar = "$Root\desktop-app\target\thunai-desktop.jar"
if (-not (Test-Path $jar)) { Write-Error 'Jar not found. Run scripts\build-desktop.ps1 first.'; exit 1 }
java -jar $jar
