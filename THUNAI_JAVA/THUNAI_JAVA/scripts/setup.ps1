. "$PSScriptRoot\_common.ps1"
Require-Tool java  'Install JDK 17 or newer (e.g. Temurin) and reopen PowerShell.'
Require-Tool javac 'You have a JRE only. Install a full JDK 17+.'
Require-Tool mvn   'Install Apache Maven 3.9+ and add it to PATH.'
Require-Tool mysql 'Install MySQL 8 and add its bin folder to PATH.'
java -version
if (-not $env:ANDROID_HOME) { Write-Warning 'ANDROID_HOME is not set (only needed for the Android app, which is not included in this scaffold).' }
Write-Host "Next: mysql -u root -p < `"$Root\database\schema.sql`"  then set env vars from backend\.env.template and run scripts\run-backend.ps1"
