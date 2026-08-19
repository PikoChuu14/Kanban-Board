@echo off
setlocal
cd /d "%~dp0.."
if not exist run\kovax-flowops.pid (echo Kovax FlowOps is not running. & exit /b 0)
set /p PID=<run\kovax-flowops.pid
powershell -NoProfile -Command "$p=Get-Process -Id %PID% -ErrorAction SilentlyContinue; if($p){$p.CloseMainWindow(); Start-Sleep -Seconds 2; if(!$p.HasExited){$p.Kill()}}"
del run\kovax-flowops.pid >nul 2>&1
echo Kovax FlowOps stopped.
endlocal
