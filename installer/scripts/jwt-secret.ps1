function New-JwtSecretBase64([int]$Bytes = 64) {
  if ($Bytes -lt 32) { throw 'JWT secrets must contain at least 32 random bytes.' }
  $randomBytes = New-Object byte[] $Bytes
  $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
  try {
    $generator.GetBytes($randomBytes)
  } finally {
    $generator.Dispose()
  }
  return [Convert]::ToBase64String($randomBytes)
}

function Get-PropertiesValue([string]$Path, [string]$Name) {
  if (-not (Test-Path -LiteralPath $Path)) { return $null }
  $prefix = $Name + '='
  $line = Get-Content -LiteralPath $Path -ErrorAction Stop |
    Where-Object { $_.StartsWith($prefix, [StringComparison]::Ordinal) } |
    Select-Object -First 1
  if ($null -eq $line) { return $null }
  return $line.Substring($prefix.Length).Trim()
}

function Resolve-JwtSecret([string]$SecretsPath) {
  $existingSecret = Get-PropertiesValue $SecretsPath 'app.jwt.secret'
  $configuredEncoding = Get-PropertiesValue $SecretsPath 'app.jwt.secret.encoding'

  if (-not [string]::IsNullOrWhiteSpace($existingSecret)) {
    # Markerless secrets were consumed as raw UTF-8 by older releases. Preserve that
    # interpretation so an upgrade does not invalidate tokens from a healthy system.
    $encoding = if ([string]::IsNullOrWhiteSpace($configuredEncoding)) { 'raw' } else { $configuredEncoding.ToLowerInvariant() }
    $valid = $false
    if ($encoding -eq 'raw') {
      $valid = [Text.Encoding]::UTF8.GetByteCount($existingSecret) -ge 32
    } elseif ($encoding -eq 'base64') {
      try { $valid = [Convert]::FromBase64String($existingSecret).Length -ge 32 } catch { $valid = $false }
    }
    if ($valid) {
      return [pscustomobject]@{ Secret = $existingSecret; Encoding = $encoding; Reused = $true }
    }
  }

  return [pscustomobject]@{
    Secret = New-JwtSecretBase64 64
    Encoding = 'base64'
    Reused = $false
  }
}
