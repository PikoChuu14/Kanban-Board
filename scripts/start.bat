@echo off
setlocal
cd /d "%~dp0.."
if not exist app\flowops.jar (echo [FAIL] Run setup first. & exit /b 1)
if not exist logs mkdir logs
if not exist run mkdir run
for /f %%p in ('powershell -NoProfile -Command "Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue ^| Select-Object -First 1 -ExpandProperty OwningProcess"') do (echo [FAIL] Port 8080 is already in use by PID %%p. & exit /b 1)
echo ========================================
echo             FlowOps
echo ========================================
echo Starting on http://SERVER-IP:8080
start "FlowOps" /b java -jar app\flowops.jar --spring.profiles.active=prod --spring.config.additional-location=optional:file:config\ --spring.config.import=optional:file:config\secrets.properties --logging.file.name=logs\flowops.log
for /f %%p in ('powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"name = 'java.exe'\" ^| Where-Object { $_.CommandLine -like '*flowops.jar*' } ^| Select-Object -First 1 -ExpandProperty ProcessId"') do echo %%p>run\flowops.pid
echo Started. Log: logs\flowops.log
echo Press Ctrl+C only stops this window; use stop.bat to stop safely.
endlocal
