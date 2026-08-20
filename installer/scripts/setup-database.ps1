param([Parameter(Mandatory=$true)][string]$InputFile,[Parameter(Mandatory=$true)][string]$DataRoot)
$ErrorActionPreference = 'Stop'
$input = Get-Content -Raw $InputFile | ConvertFrom-Json
$config = Join-Path $DataRoot 'config'; $logs = Join-Path $DataRoot 'logs'
New-Item -ItemType Directory -Force -Path $config,$logs,(Join-Path $DataRoot 'backups'),(Join-Path $DataRoot 'runtime') | Out-Null
. (Join-Path $PSScriptRoot 'jwt-secret.ps1')
function Find-Psql { if ($input.postgresBin -and (Test-Path (Join-Path $input.postgresBin 'psql.exe'))) { return (Join-Path $input.postgresBin 'psql.exe') }; $cmd = Get-Command psql.exe -ErrorAction SilentlyContinue; if ($cmd) { return $cmd.Source }; $roots = @('C:\Program Files\PostgreSQL','C:\Program Files (x86)\PostgreSQL'); foreach ($root in $roots) { if (Test-Path $root) { $found = Get-ChildItem $root -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | ForEach-Object { $candidate = Join-Path $_.FullName 'bin\psql.exe'; if (Test-Path $candidate) { Get-Item $candidate; break } } | Select-Object -First 1; if ($found) { return $found.FullName } } }; throw 'PostgreSQL client psql.exe was not found. Install PostgreSQL or add psql.exe to PATH.' }
function Find-PgTool([string]$Name) { $candidate = Join-Path (Split-Path $psql -Parent) $Name; if (Test-Path $candidate) { return $candidate }; throw "$Name was not found beside psql.exe." }
$psql = Find-Psql
function New-Secret([int]$Bytes=48) { $b=New-Object byte[] $Bytes; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b) }
function SqlQuote([string]$Value) { return $Value.Replace("'", "''") }
$secretsPath = Join-Path $config 'secrets.properties'
$existingApplication = Join-Path $config 'application.properties'
$existingPgpass = Join-Path $config 'pgpass.conf'
$useExisting = ($input.databaseMode -eq 'existing')
$dbPassword = ''
$jwt = ''; $jwtEncoding = ''
$existingHost = ''; $existingPort = ''; $existingDb = ''; $existingUser = ''
$baseUrl = ''
if ($useExisting -and (Test-Path -LiteralPath $existingApplication) -and (Test-Path -LiteralPath $existingPgpass)) {
  $existingUrl = (Get-Content -LiteralPath $existingApplication | Where-Object { $_ -like 'spring.datasource.url=*' } | Select-Object -First 1) -replace '^spring.datasource.url=', ''
  if ($existingUrl -match '^jdbc:postgresql://([^:/]+)(?::(\d+))?/([^?]+)') { $existingHost=$Matches[1]; $existingPort=$Matches[2]; $existingDb=$Matches[3] }
  $existingUser = (Get-Content -LiteralPath $existingApplication | Where-Object { $_ -like 'spring.datasource.username=*' } | Select-Object -First 1) -replace '^spring.datasource.username=', ''
  $baseUrl = (Get-Content -LiteralPath $existingApplication | Where-Object { $_ -like 'app.base-url=*' } | Select-Object -First 1) -replace '^app.base-url=', ''
  if ($existingHost) { $input.host=$existingHost }; if ($existingPort) { $input.port=$existingPort }; if ($existingDb) { $input.database=$existingDb }; if ($existingUser) { $input.appUser=$existingUser }
  $dbPassword = (Get-Content -LiteralPath $existingPgpass | Select-Object -First 1) -split ':' | Select-Object -Last 1
  $jwtConfig = Resolve-JwtSecret $secretsPath; $jwt=$jwtConfig.Secret; $jwtEncoding=$jwtConfig.Encoding
  Add-Content -Encoding utf8 (Join-Path $logs 'installer-database.log') 'Database mode: existing; retained application credentials, JWT secret, and database contents.'
} else {
  $dbPassword = New-Secret
  $jwtConfig = Resolve-JwtSecret $secretsPath; $jwt = $jwtConfig.Secret; $jwtEncoding = $jwtConfig.Encoding
}
$configuredBaseUrl = [Environment]::GetEnvironmentVariable('APP_BASE_URL')
if ($configuredBaseUrl) { $baseUrl = $configuredBaseUrl.TrimEnd('/') }
# Never persist an automatically detected DHCP address. APP_BASE_URL or an
# existing explicit app.base-url is the only source for the company URL.
if (-not $baseUrl -or $baseUrl -match '^http://localhost(?::\d+)?$') { $baseUrl = '' }
$adminPasswordFile = Join-Path $DataRoot 'runtime\postgres-admin.pgpass.tmp'
$sqlFile = Join-Path $DataRoot 'runtime\database-setup.sql'
try {
  Set-Content -Encoding ascii $adminPasswordFile "$($input.host):$($input.port):*:$($input.adminUser):$($input.postgresAdminPassword)"
  if (-not $useExisting) {
    Set-Content -Encoding utf8 $sqlFile @"
DO `$`$ BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '$($input.appUser)') THEN
    CREATE ROLE "$($input.appUser)" LOGIN PASSWORD '$(SqlQuote $dbPassword)';
  ELSE
    ALTER ROLE "$($input.appUser)" WITH LOGIN PASSWORD '$(SqlQuote $dbPassword)';
  END IF;
END `$`$;
SELECT 'CREATE DATABASE $($input.database) OWNER $($input.appUser)' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$($input.database)')\gexec
ALTER DATABASE "$($input.database)" OWNER TO "$($input.appUser)";
GRANT ALL PRIVILEGES ON DATABASE "$($input.database)" TO "$($input.appUser)";
"@
  } else { Set-Content -Encoding utf8 $sqlFile "SELECT 1;" }
  $env:PGPASSFILE = $adminPasswordFile
  Add-Content -Encoding utf8 (Join-Path $logs 'installer-database.log') "PostgreSQL detection: $($input.postgresDetection); service=$($input.postgresService); status=$($input.postgresStatus); bin=$($input.postgresBin)"
  if ($useExisting) {
    $env:PGPASSFILE = $existingPgpass
    & $psql -h $input.host -p $input.port -U $input.appUser -d $input.database -v ON_ERROR_STOP=1 -f $sqlFile *>> (Join-Path $logs 'installer-database.log')
  } else {
    # Recheck the live PostgreSQL catalog with the supplied administrator
    # credentials. The earlier wizard probe can be inconclusive when retained
    # application credentials are unavailable or protected.
    $databaseExistsText = & $psql -h $input.host -p $input.port -U $input.adminUser -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = '$(SqlQuote ([string]$input.database))';" 2>> (Join-Path $logs 'installer-database.log')
    if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL could not verify whether the existing FlowOps database is present.' }
    $databaseExists = (($databaseExistsText | Select-Object -First 1) | Out-String).Trim() -eq '1'
    Add-Content -Encoding utf8 (Join-Path $logs 'installer-database.log') "Database mode: new; live existing database check=$databaseExists"
    if ($databaseExists) {
      $pgDump = Find-PgTool 'pg_dump.exe'
      $stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
      $backup = Join-Path $DataRoot ("backups\flowops_$stamp.backup")
      & $pgDump -h $input.host -p $input.port -U $input.adminUser -d $input.database -Fc -f $backup *>> (Join-Path $logs 'installer-database.log')
      if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $backup)) { throw 'The existing FlowOps database could not be backed up; no new database was created.' }
      $archive = "flowops_backup_$stamp"
      $renameSql = "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$(SqlQuote ([string]$input.database))' AND pid <> pg_backend_pid(); ALTER DATABASE `"$($input.database)`" RENAME TO `"$archive`";"
      & $psql -h $input.host -p $input.port -U $input.adminUser -d postgres -v ON_ERROR_STOP=1 -c $renameSql *>> (Join-Path $logs 'installer-database.log')
      if ($LASTEXITCODE -ne 0) { throw "The existing database backup was created at $backup, but the database could not be archived. No new database was created." }
      Add-Content -Encoding utf8 (Join-Path $logs 'installer-database.log') "Existing database archived as $archive; backup=$backup"
      $metadataPath = Join-Path $DataRoot 'backups\backup-metadata.json'
      $metadata = @{}
      if (Test-Path -LiteralPath $metadataPath) {
        $existingMetadata = Get-Content -Raw -LiteralPath $metadataPath | ConvertFrom-Json
        $existingMetadata.PSObject.Properties | ForEach-Object { $metadata[$_.Name] = $_.Value }
      }
      $metadata[[IO.Path]::GetFileName($backup)] = @{
        filename=[IO.Path]::GetFileName($backup); createdAt=(Get-Date).ToString('s'); backupType='PRE_NEW_DATABASE';
        reason='Installer created a new active database'; sourceDatabase=[string]$input.database; archivedDatabaseName=$archive; status='COMPLETED'
      }
      $metadata | ConvertTo-Json -Depth 5 | Set-Content -Encoding utf8 -LiteralPath $metadataPath
    }
    & $psql -h $input.host -p $input.port -U $input.adminUser -d postgres -v ON_ERROR_STOP=1 -f $sqlFile *>> (Join-Path $logs 'installer-database.log')
  }
  if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL database setup failed. Check the installer log.' }
  Set-Content -Encoding utf8 (Join-Path $config 'application.properties') @"
server.port=$($input.appPort)
server.address=0.0.0.0
spring.datasource.url=jdbc:postgresql://$($input.host):$($input.port)/$($input.database)
spring.datasource.username=$($input.appUser)
spring.datasource.password=`${DB_PASSWORD}
app.base-url=$baseUrl
app.mail.enabled=false
app.backup.directory=$([IO.Path]::Combine($DataRoot, 'backups').Replace('\','/'))
app.postgres.bin=$(([string]$input.postgresBin).Replace('\','/'))
app.restore.enabled=true
app.restore.helper-path=$([IO.Path]::Combine($PSScriptRoot, 'restore-request.ps1').Replace('\','/'))
"@
  $secretsContent = @"
DB_PASSWORD=$dbPassword
app.jwt.secret=$jwt
app.jwt.secret.encoding=$jwtEncoding
$(if ($useExisting) { Get-Content $secretsPath | Where-Object { $_ -like 'app.bootstrap.admin.*' } })
$(if (-not $useExisting) { "app.bootstrap.admin.name=$($input.adminName)`napp.bootstrap.admin.email=$($input.adminEmail)`napp.bootstrap.admin.password=$($input.adminPassword)" })
app.bootstrap.secrets-file=$([IO.Path]::Combine($config, 'secrets.properties'))
"@
  [IO.File]::WriteAllText($secretsPath, $secretsContent, [Text.UTF8Encoding]::new($false))
  Set-Content -Encoding ascii (Join-Path $config 'postgres-bin.txt') $input.postgresBin
  Set-Content -Encoding ascii (Join-Path $config 'pgpass.conf') "$($input.host):$($input.port):$($input.database):$($input.appUser):$dbPassword"
  icacls $config /inheritance:r /grant:r 'SYSTEM:(OI)(CI)(F)' 'Administrators:(OI)(CI)(F)' | Out-Null
  Write-Output 'Database and protected application configuration created.'
} finally {
  Remove-Item $adminPasswordFile,$sqlFile -Force -ErrorAction SilentlyContinue
  Remove-Item Env:PGPASSFILE -ErrorAction SilentlyContinue
}
