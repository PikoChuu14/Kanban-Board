$ErrorActionPreference = 'Stop'
$data = 'C:\ProgramData\Kovax FlowOps'
$pgpass = Join-Path $data 'config\pgpass.conf'
$binFile = Join-Path $data 'config\postgres-bin.txt'
$pgBin = if (Test-Path $binFile) { (Get-Content -Raw $binFile).Trim() } else { '' }
if (-not $pgBin -or -not (Test-Path (Join-Path $pgBin 'pg_dump.exe'))) {
  $cmd = Get-Command pg_dump.exe -ErrorAction SilentlyContinue
  if ($cmd) { $pgBin = Split-Path $cmd.Source -Parent }
}
if (-not $pgBin -or -not (Test-Path (Join-Path $pgBin 'pg_dump.exe'))) { throw 'PostgreSQL pg_dump.exe was not found.' }
$out = Join-Path $data 'backups\kovax-flowops-{0}.dump' (Get-Date -Format 'yyyy-MM-dd-HHmm')
New-Item -ItemType Directory -Force (Split-Path $out) | Out-Null
$env:PGPASSFILE = $pgpass
& (Join-Path $pgBin 'pg_dump.exe') -h localhost -p 5432 -U kovax_user -Fc -f $out kovax_flowops
if ($LASTEXITCODE -ne 0) { throw 'Backup failed' }
Write-Host "Backup created: $out"
Read-Host 'Press Enter to close'
