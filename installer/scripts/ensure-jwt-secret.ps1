param([Parameter(Mandatory = $true)][string]$SecretsPath)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'jwt-secret.ps1')

$resolved = Resolve-JwtSecret $SecretsPath
$retainedLines = @()
if (Test-Path -LiteralPath $SecretsPath) {
  $retainedLines = @(Get-Content -LiteralPath $SecretsPath | Where-Object {
    -not $_.StartsWith('app.jwt.secret=', [StringComparison]::Ordinal) -and
    -not $_.StartsWith('app.jwt.secret.encoding=', [StringComparison]::Ordinal)
  })
}

$allLines = @($retainedLines)
$allLines += ('app.jwt.secret=' + $resolved.Secret)
$allLines += ('app.jwt.secret.encoding=' + $resolved.Encoding)
$parent = Split-Path -Parent $SecretsPath
if ($parent -and -not (Test-Path -LiteralPath $parent)) {
  [void](New-Item -ItemType Directory -Path $parent -Force)
}
[IO.File]::WriteAllText(
  $SecretsPath,
  (($allLines -join [Environment]::NewLine) + [Environment]::NewLine),
  [Text.UTF8Encoding]::new($false)
)

if ($resolved.Reused) {
  Write-Output 'Existing valid JWT secret preserved.'
} else {
  Write-Output 'A new 64-byte JWT secret was generated.'
}
