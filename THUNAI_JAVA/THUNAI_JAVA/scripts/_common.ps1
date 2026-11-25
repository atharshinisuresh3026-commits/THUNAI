$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $PSScriptRoot
function Require-Tool($name, $hint) {
  if (-not (Get-Command $name -ErrorAction SilentlyContinue)) { Write-Error "Missing prerequisite '$name'. $hint"; exit 1 }
}
