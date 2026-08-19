param([Parameter(Mandatory=$true)][string]$DataRoot,[Parameter(Mandatory=$true)][string]$OutputFile,[string]$PsqlPath = '')
$ErrorActionPreference = 'SilentlyContinue'

function Get-Property([string]$Path, [string]$Name) {
  if (-not (Test-Path -LiteralPath $Path)) { return '' }
  $line = Get-Content -LiteralPath $Path | Where-Object { $_ -match ('^' + [regex]::Escape($Name) + '=') } | Select-Object -First 1
  if ($line) { return ($line -replace ('^' + [regex]::Escape($Name) + '='), '') }
  return ''
}

$config = Join-Path $DataRoot 'config'
$application = Join-Path $config 'application.properties'
$pgpass = Join-Path $config 'pgpass.conf'
$db = Get-Property $application 'spring.datasource.url' -replace '^jdbc:postgresql://[^/]+/', ''
$user = Get-Property $application 'spring.datasource.username'
$url = Get-Property $application 'spring.datasource.url'
$host = 'localhost'; $port = '5432'
if ($url -match '^jdbc:postgresql://([^:/]+)(?::(\d+))?/([^?]+)') { $host = $Matches[1]; if ($Matches[2]) { $port = $Matches[2] }; $db = $Matches[3] }
if (-not $db) { $db = 'kovax_flowops' }
if (-not $PsqlPath) { $cmd = Get-Command psql.exe -ErrorAction SilentlyContinue; if ($cmd) { $PsqlPath = $cmd.Source } }

$dbDetected = $false; $dataDetected = [bool](Test-Path -LiteralPath $application); $adminDetected = $false; $querySucceeded = $false; $adminCount = 0
if ($PsqlPath -and (Test-Path -LiteralPath $PsqlPath) -and (Test-Path -LiteralPath $pgpass)) {
  $env:PGPASSFILE = $pgpass
  $result = & $PsqlPath -h $host -p $port -U $user -d $db -tAc "SELECT COUNT(*) FROM users WHERE UPPER(role::text) = 'ADMIN';" 2>$null
  if ($LASTEXITCODE -eq 0) {
    $querySucceeded = $true; $dbDetected = $true
    $adminText = (($result | Select-Object -First 1) | Out-String).Trim()
    [void][int]::TryParse($adminText, [ref]$adminCount)
    $adminDetected = $adminCount -gt 0
  }
  Remove-Item Env:PGPASSFILE -ErrorAction SilentlyContinue
}
@(
  "DB_DETECTED=$([int]$dbDetected)",
  "DATA_DETECTED=$([int]$dataDetected)",
  "ADMIN_DETECTED=$([int]$adminDetected)",
  "ADMIN_COUNT=$adminCount",
  "ADMIN_QUERY_SUCCEEDED=$([int]$querySucceeded)",
  "DATABASE=$db",
  "DB_USER=$user",
  "HOST=$host",
  "PORT=$port"
) | Set-Content -LiteralPath $OutputFile -Encoding ascii
Write-Output "DB_DETECTED=$([int]$dbDetected)"
Write-Output "DATA_DETECTED=$([int]$dataDetected)"
Write-Output "ADMIN_DETECTED=$([int]$adminDetected)"
Write-Output "ADMIN_COUNT=$adminCount"
Write-Output "ADMIN_QUERY_SUCCEEDED=$([int]$querySucceeded)"
