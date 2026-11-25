. "$PSScriptRoot\_common.ps1"
Require-Tool mvn 'Install Apache Maven 3.9+.'
Push-Location "$Root\desktop-app"; try { mvn -q package; if ($LASTEXITCODE -ne 0) { Write-Error 'Desktop build failed.'; exit 1 } } finally { Pop-Location }
Write-Host "Built: $Root\desktop-app\target\thunai-desktop.jar"
