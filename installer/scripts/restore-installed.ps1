$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Windows.Forms
$data = 'C:\ProgramData\Kovax FlowOps'
$dialog = New-Object Windows.Forms.OpenFileDialog
$dialog.InitialDirectory = Join-Path $data 'backups'
$dialog.Filter = 'FlowOps backups (*.dump)|*.dump'
if ($dialog.ShowDialog() -ne 'OK') { exit }
if ([Windows.Forms.MessageBox]::Show('This replaces current FlowOps data. Continue?', 'Kovax FlowOps Restore', 'YesNo', 'Warning') -ne 'Yes') { exit }
$binFile = Join-Path $data 'config\postgres-bin.txt'
$pgBin = if (Test-Path $binFile) { (Get-Content -Raw $binFile).Trim() } else { '' }
if (-not $pgBin -or -not (Test-Path (Join-Path $pgBin 'pg_restore.exe'))) {
  $cmd = Get-Command pg_restore.exe -ErrorAction SilentlyContinue
  if ($cmd) { $pgBin = Split-Path $cmd.Source -Parent }
}
if (-not $pgBin -or -not (Test-Path (Join-Path $pgBin 'pg_restore.exe'))) { throw 'PostgreSQL pg_restore.exe was not found.' }
& sc.exe stop KovaxFlowOps | Out-Null
Start-Sleep 3
$env:PGPASSFILE = Join-Path $data 'config\pgpass.conf'
& (Join-Path $pgBin 'pg_restore.exe') -h localhost -p 5432 -U kovax_user --clean --if-exists --no-owner -d kovax_flowops $dialog.FileName
& sc.exe start KovaxFlowOps | Out-Null
[Windows.Forms.MessageBox]::Show('Restore completed.', 'Kovax FlowOps')
