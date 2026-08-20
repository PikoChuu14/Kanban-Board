$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$installer = Join-Path $root 'installer'

function Find-Tool([string]$Name, [string[]]$Fallbacks) {
  $command = Get-Command $Name -ErrorAction SilentlyContinue
  if ($command) { return $command.Source }
  foreach ($path in $Fallbacks) { if (Test-Path -LiteralPath $path) { return $path } }
  return $null
}

$iscc = Find-Tool 'ISCC.exe' @(
  'C:\Program Files (x86)\Inno Setup 6\ISCC.exe',
  'C:\Program Files\Inno Setup 6\ISCC.exe'
)
if (-not $iscc) { throw 'Inno Setup compiler ISCC.exe was not found. Install Inno Setup 6, then rerun this script.' }

& $iscc (Join-Path $installer 'FlowOps-Client.iss')
if ($LASTEXITCODE -ne 0) { throw 'FlowOps Client installer compilation failed.' }

$output = Join-Path $root 'dist\installer\FlowOps-Client-Setup.exe'
if (-not (Test-Path -LiteralPath $output)) { throw "Installer output was not created: $output" }
Write-Host "FlowOps Client installer created: $output"
