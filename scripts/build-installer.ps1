$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$backend = Join-Path $root 'backend'; $frontend = Join-Path $root 'frontend'; $installer = Join-Path $root 'installer'; $payload = Join-Path $installer 'payload'
function Find-Tool([string]$Name,[string[]]$Fallbacks) { $cmd=Get-Command $Name -ErrorAction SilentlyContinue; if($cmd){return $cmd.Source}; foreach($p in $Fallbacks){if(Test-Path $p){return $p}}; return $null }
$iscc = Find-Tool 'ISCC.exe' @('C:\Program Files (x86)\Inno Setup 6\ISCC.exe','C:\Program Files\Inno Setup 6\ISCC.exe')
$jlink = Find-Tool 'jlink.exe' @()
if(-not $iscc){ throw 'Inno Setup compiler ISCC.exe was not found. Install Inno Setup 6, then rerun scripts\build-installer.bat.' }
if(-not $jlink){ throw 'jlink.exe was not found. Run this build with a Java 21 JDK.' }
$winsw = Join-Path $installer 'prerequisites\WinSW-x64.exe'; if(-not (Test-Path $winsw)){ throw 'installer\prerequisites\WinSW-x64.exe is required. Place the approved WinSW binary there; it is not bundled by this repository.' }
$postgresInstaller = Join-Path $installer 'prerequisites\postgresql-installer.exe'; if(-not (Test-Path $postgresInstaller)){ throw 'installer\prerequisites\postgresql-installer.exe is required for the automatic PostgreSQL option. Place the official installer there; it is not bundled by this repository.' }
Write-Host '[1/6] Building frontend...'
Push-Location $frontend
try {
  npm ci
  if($LASTEXITCODE -ne 0){throw 'Frontend dependency installation failed'}
  npm run build
  if($LASTEXITCODE -ne 0){throw 'Frontend build failed'}
} finally {
  Pop-Location
}
Write-Host '[2/6] Building Spring Boot JAR...'; Push-Location $backend; & .\mvnw.cmd -DskipTests clean package; if($LASTEXITCODE -ne 0){throw 'Backend package failed'}; Pop-Location
Write-Host '[3/6] Preparing installer payload...'
New-Item -ItemType Directory -Force -Path "$payload\app","$payload\tools","$payload\prerequisites" | Out-Null
Copy-Item "$backend\target\kovax-flowops.jar" "$payload\app\kovax-flowops.jar" -Force
Copy-Item "$installer\service.xml" "$payload\KovaxFlowOps.xml" -Force
Copy-Item $winsw "$payload\KovaxFlowOps.exe" -Force
Copy-Item $postgresInstaller "$payload\prerequisites\postgresql-installer.exe" -Force
Copy-Item "$installer\scripts\setup-database.ps1","$installer\scripts\jwt-secret.ps1","$installer\scripts\backup-installed.ps1","$installer\scripts\restore-installed.ps1","$installer\scripts\detect-postgresql.ps1","$installer\scripts\inspect-flowops.ps1","$installer\scripts\wait-for-ready.ps1" "$payload\tools" -Force
Write-Host '[OK]'
Write-Host '[4/6] Creating private Java 21 runtime...'
$runtimeDir = Join-Path $payload 'runtime'
if(Test-Path $runtimeDir){Remove-Item -Recurse -Force $runtimeDir}
& $jlink --add-modules java.base,java.compiler,java.datatransfer,java.desktop,java.instrument,java.logging,java.management,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,java.security.sasl,java.sql,java.transaction.xa,jdk.crypto.ec,jdk.unsupported --output $runtimeDir --strip-debug --no-header-files --no-man-pages --compress=2
if($LASTEXITCODE -ne 0){throw 'jlink runtime creation failed'}
if(-not (Test-Path (Join-Path $runtimeDir 'bin\java.exe'))){throw "jlink completed but runtime\bin\java.exe is missing: $runtimeDir"}
Write-Host '[OK] runtime\bin\java.exe'
Write-Host '[5/6] Compiling Inno Setup installer...'; & $iscc "$installer\KovaxFlowOps.iss"; if($LASTEXITCODE -ne 0){throw 'Inno Setup compilation failed'}
$installerOutput = Join-Path $root 'dist\installer\KovaxFlowOps-Setup.exe'
if(-not (Test-Path -LiteralPath $installerOutput)){throw "Inno Setup reported success but the installer was not created: $installerOutput"}
$installerTimestamp = (Get-Item -LiteralPath $installerOutput).LastWriteTime.ToString('yyyy-MM-dd HH:mm:ss zzz')
Write-Host '[6/6] Installer ready'
Write-Host "Installer build timestamp: $installerTimestamp"
Write-Host "Installer output path: $installerOutput"
