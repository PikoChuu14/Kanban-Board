param([Parameter(Mandatory = $true)][string]$RequestFile)

$ErrorActionPreference = 'Stop'

$requestPath = [IO.Path]::GetFullPath($RequestFile)
$backupRoot = [IO.Path]::GetFullPath((Split-Path -Parent $requestPath))
$runtimeRoot = Join-Path (Split-Path -Parent $backupRoot) 'runtime'
$statusFile = Join-Path $runtimeRoot 'restore-status.json'

function Set-RestoreStatus([string]$Status, [string]$Message) {
    New-Item -ItemType Directory -Path $runtimeRoot -Force | Out-Null
    @{
        status = $Status
        message = $Message
        updatedAt = (Get-Date).ToString('o')
    } | ConvertTo-Json | Set-Content -Encoding utf8 -LiteralPath $statusFile
}

try {
    Start-Sleep -Seconds 2
    $request = Get-Content -Raw -LiteralPath $requestPath | ConvertFrom-Json
    $backup = [IO.Path]::GetFullPath([string]$request.backup)
    $extension = [IO.Path]::GetExtension($backup).ToLowerInvariant()

    if ((Split-Path -Parent $backup) -ne $backupRoot -or $extension -notin @('.backup', '.dump') -or -not (Test-Path -LiteralPath $backup -PathType Leaf)) {
        throw 'Invalid restore source.'
    }

    if ([string]::IsNullOrWhiteSpace([string]$request.postgresBin)) {
        $restore = (Get-Command 'pg_restore.exe' -ErrorAction Stop).Source
    } else {
        $restore = Join-Path ([string]$request.postgresBin) 'pg_restore.exe'
        if (-not (Test-Path -LiteralPath $restore -PathType Leaf)) {
            throw 'pg_restore.exe was not found in the configured PostgreSQL directory.'
        }
    }

    Set-RestoreStatus 'RESTORING' "Restoring $([string]$request.database) from $([IO.Path]::GetFileName($backup))"
    & $restore `
        -h ([string]$request.host) `
        -p ([string]$request.port) `
        -U ([string]$request.username) `
        --clean `
        --if-exists `
        --no-owner `
        --no-privileges `
        --single-transaction `
        --exit-on-error `
        -d ([string]$request.database) `
        $backup

    if ($LASTEXITCODE -ne 0) {
        throw 'pg_restore failed. The automatic pre-restore safety backup is available for recovery.'
    }

    Set-RestoreStatus 'COMPLETED' 'Restore completed successfully. Refresh the browser to load the restored data.'
    Remove-Item -LiteralPath $requestPath -Force -ErrorAction SilentlyContinue
} catch {
    Set-RestoreStatus 'FAILED' $_.Exception.Message
    exit 1
}
