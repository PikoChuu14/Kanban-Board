$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Windows.Forms
$data = 'C:\ProgramData\FlowOps'
$dialog = New-Object Windows.Forms.OpenFileDialog
$dialog.InitialDirectory = Join-Path $data 'backups'
$dialog.Filter = 'FlowOps backups (*.backup;*.dump)|*.backup;*.dump'
if ($dialog.ShowDialog() -ne 'OK') { exit }
if ([Windows.Forms.MessageBox]::Show('This replaces current FlowOps data. Continue?', 'FlowOps Restore', 'YesNo', 'Warning') -ne 'Yes') { exit }
$binFile = Join-Path $data 'config\postgres-bin.txt'
$pgBin = if (Test-Path $binFile) { (Get-Content -Raw $binFile).Trim() } else { '' }
if (-not $pgBin -or -not (Test-Path (Join-Path $pgBin 'pg_restore.exe'))) {
  $cmd = Get-Command pg_restore.exe -ErrorAction SilentlyContinue
  if ($cmd) { $pgBin = Split-Path $cmd.Source -Parent }
}
if (-not $pgBin -or -not (Test-Path (Join-Path $pgBin 'pg_restore.exe'))) { throw 'PostgreSQL pg_restore.exe was not found.' }
$application = Join-Path $data 'config\application.properties'
$url = ((Get-Content -LiteralPath $application | Where-Object { $_ -like 'spring.datasource.url=*' } | Select-Object -First 1) -replace '^spring.datasource.url=', '')
$user = ((Get-Content -LiteralPath $application | Where-Object { $_ -like 'spring.datasource.username=*' } | Select-Object -First 1) -replace '^spring.datasource.username=', '')
if ($url -notmatch '^jdbc:postgresql://([^:/]+)(?::(\d+))?/([^?]+)') { throw 'The FlowOps database configuration is invalid.' }
$hostName = $Matches[1]; $port = if ($Matches[2]) { $Matches[2] } else { '5432' }; $database = $Matches[3]
& sc.exe stop FlowOps | Out-Null
Start-Sleep 3
$env:PGPASSFILE = Join-Path $data 'config\pgpass.conf'
& (Join-Path $pgBin 'pg_restore.exe') -h $hostName -p $port -U $user --clean --if-exists --no-owner -d $database $dialog.FileName
& sc.exe start FlowOps | Out-Null
[Windows.Forms.MessageBox]::Show('Restore completed.', 'FlowOps')
