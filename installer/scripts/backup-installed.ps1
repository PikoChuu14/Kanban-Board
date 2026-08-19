$ErrorActionPreference = 'Stop'
$data = 'C:\ProgramData\FlowOps'
$pgpass = Join-Path $data 'config\pgpass.conf'
$binFile = Join-Path $data 'config\postgres-bin.txt'
$pgBin = if (Test-Path $binFile) { (Get-Content -Raw $binFile).Trim() } else { '' }
if (-not $pgBin -or -not (Test-Path (Join-Path $pgBin 'pg_dump.exe'))) {
  $cmd = Get-Command pg_dump.exe -ErrorAction SilentlyContinue
  if ($cmd) { $pgBin = Split-Path $cmd.Source -Parent }
}
if (-not $pgBin -or -not (Test-Path (Join-Path $pgBin 'pg_dump.exe'))) { throw 'PostgreSQL pg_dump.exe was not found.' }
$application = Join-Path $data 'config\application.properties'
$url = ((Get-Content -LiteralPath $application | Where-Object { $_ -like 'spring.datasource.url=*' } | Select-Object -First 1) -replace '^spring.datasource.url=', '')
$user = ((Get-Content -LiteralPath $application | Where-Object { $_ -like 'spring.datasource.username=*' } | Select-Object -First 1) -replace '^spring.datasource.username=', '')
if ($url -notmatch '^jdbc:postgresql://([^:/]+)(?::(\d+))?/([^?]+)') { throw 'The FlowOps database configuration is invalid.' }
$hostName = $Matches[1]; $port = if ($Matches[2]) { $Matches[2] } else { '5432' }; $database = $Matches[3]
$out = Join-Path $data 'backups\flowops-{0}.dump' (Get-Date -Format 'yyyy-MM-dd-HHmm')
New-Item -ItemType Directory -Force (Split-Path $out) | Out-Null
$env:PGPASSFILE = $pgpass
& (Join-Path $pgBin 'pg_dump.exe') -h $hostName -p $port -U $user -Fc -f $out $database
if ($LASTEXITCODE -ne 0) { throw 'Backup failed' }
Write-Host "Backup created: $out"
Read-Host 'Press Enter to close'
