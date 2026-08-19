$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot '..\scripts\jwt-secret.ps1')

function Assert-True([bool]$Condition, [string]$Message) {
  if (-not $Condition) { throw $Message }
}

$testRoot = Join-Path ([IO.Path]::GetTempPath()) ('KovaxFlowOps-JwtSecret-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $testRoot | Out-Null
try {
  $secretsPath = Join-Path $testRoot 'secrets.properties'

  $created = Resolve-JwtSecret $secretsPath
  Assert-True (-not $created.Reused) 'A missing JWT secret must be generated.'
  Assert-True ($created.Encoding -eq 'base64') 'New JWT secrets must use Base64 encoding.'
  Assert-True ([Convert]::FromBase64String($created.Secret).Length -eq 64) 'New JWT secrets must decode to 64 random bytes.'

  [IO.File]::WriteAllText($secretsPath, "app.jwt.secret=$($created.Secret)`napp.jwt.secret.encoding=base64`n", [Text.UTF8Encoding]::new($false))
  $reused = Resolve-JwtSecret $secretsPath
  Assert-True ($reused.Reused -and $reused.Secret -ceq $created.Secret) 'A valid generated JWT secret must be reused unchanged.'

  $legacyRaw = 'legacy-production-secret-32-bytes!'
  [IO.File]::WriteAllText($secretsPath, "app.jwt.secret=$legacyRaw`n", [Text.UTF8Encoding]::new($false))
  $legacy = Resolve-JwtSecret $secretsPath
  Assert-True ($legacy.Reused -and $legacy.Encoding -eq 'raw' -and $legacy.Secret -ceq $legacyRaw) 'A valid markerless legacy secret must retain raw interpretation.'

  [IO.File]::WriteAllText($secretsPath, "app.jwt.secret=development-only-change-me`n", [Text.UTF8Encoding]::new($false))
  $replaced = Resolve-JwtSecret $secretsPath
  Assert-True (-not $replaced.Reused) 'A 26-byte legacy JWT secret must be replaced.'
  Assert-True ([Convert]::FromBase64String($replaced.Secret).Length -eq 64) 'A replacement JWT secret must decode to 64 bytes.'

  Write-Output 'JWT installer secret tests passed.'
} finally {
  Remove-Item -LiteralPath $testRoot -Recurse -Force -ErrorAction SilentlyContinue
}
