param(
  [switch]$Configure,
  [switch]$Test,
  [string]$Url,
  [string]$ConfigPath = (Join-Path $env:LOCALAPPDATA 'FlowOps Client\server-url.txt')
)

$ErrorActionPreference = 'Stop'

function Get-ValidFlowOpsUrl([string]$Value) {
  if ([string]::IsNullOrWhiteSpace($Value)) { throw 'The FlowOps server address is not configured.' }
  $candidate = $Value.Trim().TrimEnd('/')
  $uri = $null
  if (-not [Uri]::TryCreate($candidate, [UriKind]::Absolute, [ref]$uri) -or
      $uri.Scheme -notin @('http', 'https') -or
      [string]::IsNullOrWhiteSpace($uri.Host) -or
      -not [string]::IsNullOrWhiteSpace($uri.UserInfo)) {
    throw 'Enter a complete HTTP or HTTPS address without credentials, for example http://flowops-server:8080.'
  }
  return $candidate
}

function Save-FlowOpsUrl([string]$Value) {
  $validUrl = Get-ValidFlowOpsUrl $Value
  $configDirectory = Split-Path $ConfigPath -Parent
  New-Item -ItemType Directory -Force -Path $configDirectory | Out-Null
  [IO.File]::WriteAllText($ConfigPath, $validUrl, [Text.UTF8Encoding]::new($false))
  return $validUrl
}

function Show-ConfigurationDialog {
  Add-Type -AssemblyName Microsoft.VisualBasic
  Add-Type -AssemblyName System.Windows.Forms
  $current = if (Test-Path -LiteralPath $ConfigPath) { (Get-Content -Raw -LiteralPath $ConfigPath).Trim() } else { 'http://flowops-server:8080' }
  $entered = [Microsoft.VisualBasic.Interaction]::InputBox(
    'Enter the central FlowOps server address. Only this URL is stored on this PC.',
    'Configure FlowOps Client',
    $current)
  if ([string]::IsNullOrWhiteSpace($entered)) { return $null }
  try {
    $saved = Save-FlowOpsUrl $entered
    [System.Windows.Forms.MessageBox]::Show("FlowOps Client now uses:`r`n$saved", 'FlowOps Client', 'OK', 'Information') | Out-Null
    return $saved
  } catch {
    [System.Windows.Forms.MessageBox]::Show($_.Exception.Message, 'FlowOps Client', 'OK', 'Error') | Out-Null
    return $null
  }
}

function Find-AppBrowser {
  $programFiles = [Environment]::GetFolderPath('ProgramFiles')
  $programFilesX86 = [Environment]::GetFolderPath('ProgramFilesX86')
  $candidates = @(
    @{ Name = 'Edge'; Path = (Join-Path $programFilesX86 'Microsoft\Edge\Application\msedge.exe') },
    @{ Name = 'Edge'; Path = (Join-Path $programFiles 'Microsoft\Edge\Application\msedge.exe') },
    @{ Name = 'Edge'; Path = (Join-Path $env:LOCALAPPDATA 'Microsoft\Edge\Application\msedge.exe') },
    @{ Name = 'Chrome'; Path = (Join-Path $programFiles 'Google\Chrome\Application\chrome.exe') },
    @{ Name = 'Chrome'; Path = (Join-Path $programFilesX86 'Google\Chrome\Application\chrome.exe') },
    @{ Name = 'Chrome'; Path = (Join-Path $env:LOCALAPPDATA 'Google\Chrome\Application\chrome.exe') }
  )
  return $candidates | Where-Object { $_.Path -and (Test-Path -LiteralPath $_.Path -PathType Leaf) } | Select-Object -First 1
}

if ($Configure) {
  if (-not (Show-ConfigurationDialog)) { exit 1 }
  exit 0
}

try {
  $flowOpsUrl = if ($Url) { Get-ValidFlowOpsUrl $Url } elseif (Test-Path -LiteralPath $ConfigPath) { Get-ValidFlowOpsUrl (Get-Content -Raw -LiteralPath $ConfigPath) } else { throw 'The FlowOps server address is not configured.' }

  if ($Test) {
    Invoke-WebRequest -Uri $flowOpsUrl -UseBasicParsing -Method Get -TimeoutSec 8 | Out-Null
    Write-Output "Connected to FlowOps at $flowOpsUrl"
    exit 0
  }

  $browser = Find-AppBrowser
  if ($browser) {
    Start-Process -FilePath $browser.Path -ArgumentList ('--app="{0}"' -f $flowOpsUrl)
  } else {
    Start-Process $flowOpsUrl
  }
} catch {
  if ($Test) {
    Write-Error $_.Exception.Message
    exit 1
  }
  Add-Type -AssemblyName System.Windows.Forms
  $choice = [System.Windows.Forms.MessageBox]::Show(
    "$($_.Exception.Message)`r`n`r`nConfigure the FlowOps server address now?",
    'FlowOps Client',
    'YesNo',
    'Warning')
  if ($choice -eq 'Yes') { Show-ConfigurationDialog | Out-Null }
  exit 1
}
