param([Parameter(Mandatory=$true)][string]$RequestFile)
$ErrorActionPreference = 'Stop'
Start-Sleep -Seconds 3
$data = Split-Path (Split-Path ([IO.Path]::GetFullPath($RequestFile)) -Parent) -Parent
$statusFile = Join-Path $data 'runtime\restore-status.json'
function Set-Status([string]$status, [string]$message) {
  @{ status=$status; message=$message; updatedAt=(Get-Date).ToString('o') } | ConvertTo-Json | Set-Content -Encoding utf8 -LiteralPath $statusFile
}
try {
  $request = Get-Content -Raw -LiteralPath $RequestFile | ConvertFrom-Json
  $backupRoot = [IO.Path]::GetFullPath((Join-Path $data 'backups'))
  $backup = [IO.Path]::GetFullPath([string]$request.backup)
  if (-not $backup.StartsWith($backupRoot + [IO.Path]::DirectorySeparatorChar) -or -not (Test-Path -LiteralPath $backup)) { throw 'Invalid restore source.' }
  $pgBin = (Get-Content -Raw -LiteralPath (Join-Path $data 'config\postgres-bin.txt')).Trim()
  $restore = Join-Path $pgBin 'pg_restore.exe'
  if (-not (Test-Path -LiteralPath $restore)) { throw 'pg_restore.exe was not found.' }
  Set-Status 'STOPPING_SERVICE' 'Stopping FlowOps before restore'
  & sc.exe stop FlowOps | Out-Null
  Start-Sleep -Seconds 5
  $env:PGPASSFILE = Join-Path $data 'config\pgpass.conf'
  Set-Status 'RESTORING' 'Restoring the selected backup'
  & $restore -h $request.host -p $request.port -U $request.username --clean --if-exists --no-owner -d $request.database $backup
  if ($LASTEXITCODE -ne 0) { throw 'pg_restore failed. The pre-restore safety backup is available for recovery.' }
  Set-Status 'RESTARTING' 'Restarting FlowOps'
  & sc.exe start FlowOps | Out-Null
  Set-Status 'COMPLETED' 'Restore completed successfully'
  Remove-Item -LiteralPath $RequestFile -Force -ErrorAction SilentlyContinue
} catch {
  Set-Status 'FAILED' $_.Exception.Message
  & sc.exe start FlowOps | Out-Null
  exit 1
}
