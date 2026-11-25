. "$PSScriptRoot\_common.ps1"
$out = "$Root\release\THUNAI_Java_Project.zip"
$items = 'backend','desktop-app','database','scripts','docs','android-app' | ForEach-Object { Join-Path $Root $_ } | Where-Object { Test-Path $_ }
$tmp = Join-Path $env:TEMP "thunai_pkg_$(Get-Random)"; New-Item -ItemType Directory $tmp | Out-Null
foreach ($i in $items) { robocopy $i (Join-Path $tmp (Split-Path $i -Leaf)) /E /XD target build .gradle /NFL /NDL /NJH /NJS | Out-Null }
if (Test-Path $out) { Write-Error "$out already exists; move or rename it first (this script never deletes files)."; exit 1 }
Compress-Archive -Path "$tmp\*" -DestinationPath $out
Remove-Item $tmp -Recurse -Force
Write-Host "Created $out"
