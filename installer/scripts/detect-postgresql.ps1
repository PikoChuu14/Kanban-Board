param(
  [Parameter(Mandatory = $true)]
  [string]$OutputFile
)

$ErrorActionPreference = 'SilentlyContinue'

function Get-MajorVersion([string]$Version, [string]$Path, [string]$ServiceName) {
  if ($Version -match '^(\d+)') { return $Matches[1] }
  if ($Path -match '\\PostgreSQL\\(\d+)(?:\\|$)') { return $Matches[1] }
  if ($ServiceName -match '(\d+)$') { return $Matches[1] }
  return ''
}

function Get-VersionNumber([string]$Text) {
  $value = 0
  if ([int]::TryParse(($Text -replace '\D.*$', ''), [ref]$value)) { return $value }
  return -1
}

function Find-RegistryInstallation {
  foreach ($view in @([Microsoft.Win32.RegistryView]::Registry64, [Microsoft.Win32.RegistryView]::Registry32)) {
    $hklm = $null
    $installations = $null
    try {
      $hklm = [Microsoft.Win32.RegistryKey]::OpenBaseKey([Microsoft.Win32.RegistryHive]::LocalMachine, $view)
      $installations = $hklm.OpenSubKey('SOFTWARE\PostgreSQL\Installations')
      if (-not $installations) { continue }
      $matches = foreach ($keyName in $installations.GetSubKeyNames()) {
        $key = $null
        try {
          $key = $installations.OpenSubKey($keyName)
          $baseDir = [string]$key.GetValue('Base Directory', '')
          $version = [string]$key.GetValue('Version', '')
          $serviceName = [string]$key.GetValue('Service ID', '')
          $binDir = if ($baseDir) { Join-Path $baseDir 'bin' } else { '' }
          $psqlPath = if ($binDir) { Join-Path $binDir 'psql.exe' } else { '' }
          [pscustomobject]@{
            View = [string]$view; Key = $keyName; BaseDir = $baseDir; Version = $version
            Major = Get-MajorVersion $version $baseDir $serviceName
            ServiceName = $serviceName; BinDir = $binDir; PsqlPath = $psqlPath
            PsqlExists = [bool]($psqlPath -and (Test-Path -LiteralPath $psqlPath))
          }
        } finally {
          if ($key) { $key.Dispose() }
        }
      }
      $match = $matches | Sort-Object @{ Expression = { Get-VersionNumber $_.Major }; Descending = $true } | Select-Object -First 1
      if ($match) { return $match }
    } finally {
      if ($installations) { $installations.Dispose() }
      if ($hklm) { $hklm.Dispose() }
    }
  }
  return $null
}

function Find-PostgresService([string]$PreferredName) {
  $services = @(Get-Service -Name 'postgresql*' -ErrorAction SilentlyContinue)
  if ($PreferredName) {
    $preferred = $services | Where-Object Name -eq $PreferredName | Select-Object -First 1
    if ($preferred) { return $preferred }
  }
  return $services | Sort-Object @{ Expression = { Get-VersionNumber $_.Name }; Descending = $true }, Name | Select-Object -First 1
}

function Find-FilesystemInstallation {
  foreach ($root in @('C:\Program Files\PostgreSQL', 'C:\Program Files (x86)\PostgreSQL')) {
    if (-not (Test-Path -LiteralPath $root)) { continue }
    $directories = Get-ChildItem -LiteralPath $root -Directory -ErrorAction SilentlyContinue |
      Where-Object Name -Match '^\d+(?:\.\d+)*$' |
      Sort-Object @{ Expression = { Get-VersionNumber $_.Name }; Descending = $true }
    foreach ($directory in $directories) {
      $binDir = Join-Path $directory.FullName 'bin'
      $psqlPath = Join-Path $binDir 'psql.exe'
      $pgDumpPath = Join-Path $binDir 'pg_dump.exe'
      $pgRestorePath = Join-Path $binDir 'pg_restore.exe'
      if ((Test-Path -LiteralPath $psqlPath) -and (Test-Path -LiteralPath $pgDumpPath) -and (Test-Path -LiteralPath $pgRestorePath)) {
        return [pscustomobject]@{ BaseDir = $directory.FullName; Major = $directory.Name; BinDir = $binDir; PsqlPath = $psqlPath }
      }
    }
  }
  return $null
}

function Find-PsqlOnPath {
  $fromPath = Get-Command psql.exe -ErrorAction SilentlyContinue
  if ($fromPath -and (Test-Path -LiteralPath $fromPath.Source)) { return $fromPath.Source }
  $wherePath = & where.exe psql.exe 2>$null | Select-Object -First 1
  if ($wherePath -and (Test-Path -LiteralPath $wherePath)) { return (Resolve-Path -LiteralPath $wherePath).Path }
  return ''
}

$registry = Find-RegistryInstallation
$service = Find-PostgresService $(if ($registry) { $registry.ServiceName } else { '' })
$filesystem = Find-FilesystemInstallation
$pathPsql = Find-PsqlOnPath
$registryDetected = [bool]$registry
$serviceDetected = [bool]$service
$filesystemDetected = [bool]$filesystem
$pathDetected = [bool]$pathPsql
$serviceName = if ($service) { $service.Name } elseif ($registry) { $registry.ServiceName } else { '' }
$serviceStatus = if ($service) { [string]$service.Status } else { '' }
$baseDir = if ($registry -and $registry.BaseDir) { $registry.BaseDir } elseif ($filesystem) { $filesystem.BaseDir } else { '' }
$binDir = if ($registry -and $registry.PsqlExists) { $registry.BinDir } elseif ($filesystem) { $filesystem.BinDir } elseif ($pathPsql) { Split-Path $pathPsql -Parent } else { '' }
$psqlPath = if ($registry -and $registry.PsqlExists) { $registry.PsqlPath } elseif ($filesystem) { $filesystem.PsqlPath } else { $pathPsql }
$major = if ($registry -and $registry.Major) { $registry.Major } elseif ($filesystem) { $filesystem.Major } else { Get-MajorVersion '' $psqlPath $serviceName }
$version = if ($registry) { $registry.Version } else { $major }
$detected = $registryDetected -or $serviceDetected -or $filesystemDetected -or $pathDetected

if ($detected) {
  $message = if ($major) { "PostgreSQL $major detected" } else { 'PostgreSQL detected' }
  if ($serviceName) { $message += "|Service: $serviceName" }
  if ($serviceStatus) { $message += "|Status: $serviceStatus" }
  elseif ($serviceName) { $message += '|Status: Installed (state unavailable)' }
} else {
  $message = 'PostgreSQL was not detected. The bundled PostgreSQL package will be installed.'
}

$lines = @(
  "DETECTED=$([int]$detected)",
  "REGISTRY_DETECTED=$([int]$registryDetected)",
  "REGISTRY_VIEW=$(if ($registry) { $registry.View } else { '' })",
  "REGISTRY_KEY=$(if ($registry) { $registry.Key } else { '' })",
  "REGISTRY_BASE_DIR=$(if ($registry) { $registry.BaseDir } else { '' })",
  "SERVICE_DETECTED=$([int]$serviceDetected)",
  "FILESYSTEM_DETECTED=$([int]$filesystemDetected)",
  "PATH_DETECTED=$([int]$pathDetected)",
  "PATH_PSQL=$pathPsql",
  "PSQL_PATH=$psqlPath",
  "BASE_DIR=$baseDir",
  "BIN_DIR=$binDir",
  "SERVICE_NAME=$serviceName",
  "SERVICE_STATUS=$serviceStatus",
  "VERSION=$version",
  "MAJOR_VERSION=$major",
  "MESSAGE=$message"
)

$outputDirectory = Split-Path -Parent $OutputFile
if ($outputDirectory -and -not (Test-Path -LiteralPath $outputDirectory)) { [void](New-Item -ItemType Directory -Path $outputDirectory -Force) }
[System.IO.File]::WriteAllLines($OutputFile, $lines, [System.Text.Encoding]::ASCII)
$lines
