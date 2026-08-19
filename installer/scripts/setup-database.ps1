param([Parameter(Mandatory=$true)][string]$InputFile,[Parameter(Mandatory=$true)][string]$DataRoot)
$ErrorActionPreference = 'Stop'
$input = Get-Content -Raw $InputFile | ConvertFrom-Json
$config = Join-Path $DataRoot 'config'; $logs = Join-Path $DataRoot 'logs'
New-Item -ItemType Directory -Force -Path $config,$logs,(Join-Path $DataRoot 'backups'),(Join-Path $DataRoot 'runtime') | Out-Null
. (Join-Path $PSScriptRoot 'jwt-secret.ps1')
function Find-Psql { if ($input.postgresBin -and (Test-Path (Join-Path $input.postgresBin 'psql.exe'))) { return (Join-Path $input.postgresBin 'psql.exe') }; $cmd = Get-Command psql.exe -ErrorAction SilentlyContinue; if ($cmd) { return $cmd.Source }; $roots = @('C:\Program Files\PostgreSQL','C:\Program Files (x86)\PostgreSQL'); foreach ($root in $roots) { if (Test-Path $root) { $found = Get-ChildItem $root -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | ForEach-Object { $candidate = Join-Path $_.FullName 'bin\psql.exe'; if (Test-Path $candidate) { Get-Item $candidate; break } } | Select-Object -First 1; if ($found) { return $found.FullName } } }; throw 'PostgreSQL client psql.exe was not found. Install PostgreSQL or add psql.exe to PATH.' }
$psql = Find-Psql
function New-Secret([int]$Bytes=48) { $b=New-Object byte[] $Bytes; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b) }
function SqlQuote([string]$Value) { return $Value.Replace("'", "''") }
$dbPassword = New-Secret
$secretsPath = Join-Path $config 'secrets.properties'
$jwtConfig = Resolve-JwtSecret $secretsPath
$jwt = $jwtConfig.Secret
$jwtEncoding = $jwtConfig.Encoding
$adminPasswordFile = Join-Path $DataRoot 'runtime\postgres-admin.pgpass.tmp'
$sqlFile = Join-Path $DataRoot 'runtime\database-setup.sql'
try {
  Set-Content -Encoding ascii $adminPasswordFile "$($input.host):$($input.port):*:$($input.adminUser):$($input.postgresAdminPassword)"
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
  $env:PGPASSFILE = $adminPasswordFile
  Add-Content -Encoding utf8 (Join-Path $logs 'installer-database.log') "PostgreSQL detection: $($input.postgresDetection); service=$($input.postgresService); status=$($input.postgresStatus); bin=$($input.postgresBin)"
  & $psql -h $input.host -p $input.port -U $input.adminUser -d postgres -v ON_ERROR_STOP=1 -f $sqlFile *>> (Join-Path $logs 'installer-database.log')
  if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL could not create the Kovax FlowOps database. Check the installer log and administrator credentials.' }
  Set-Content -Encoding utf8 (Join-Path $config 'application.properties') @"
server.port=$($input.appPort)
server.address=0.0.0.0
spring.datasource.url=jdbc:postgresql://$($input.host):$($input.port)/$($input.database)
spring.datasource.username=$($input.appUser)
spring.datasource.password=`${DB_PASSWORD}
app.base-url=http://localhost:$($input.appPort)
app.mail.enabled=false
"@
  $secretsContent = @"
DB_PASSWORD=$dbPassword
app.jwt.secret=$jwt
app.jwt.secret.encoding=$jwtEncoding
app.bootstrap.admin.name=$($input.adminName)
app.bootstrap.admin.email=$($input.adminEmail)
app.bootstrap.admin.password=$($input.adminPassword)
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
