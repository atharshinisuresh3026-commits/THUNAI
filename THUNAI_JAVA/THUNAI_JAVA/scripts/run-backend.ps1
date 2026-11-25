. "$PSScriptRoot\_common.ps1"
Require-Tool mvn 'Install Apache Maven 3.9+.'
foreach ($v in 'DB_URL','DB_USER','DB_PASSWORD','JWT_SECRET') {
  if (-not (Get-Item "env:$v" -ErrorAction SilentlyContinue)) { Write-Error "Environment variable $v is not set. See backend\.env.template."; exit 1 }
}
if (-not $env:TWILIO_ACCOUNT_SID) { Write-Warning 'Twilio not configured: SMS will be reported as NOT_CONFIGURED (not sent).' }
Push-Location "$Root\backend"; try { mvn spring-boot:run } finally { Pop-Location }
