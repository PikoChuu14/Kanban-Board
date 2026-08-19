@echo off
setlocal
cd /d "%~dp0.."
if not exist backups mkdir backups
set PGPASSFILE=%cd%\config\pgpass.conf
for /f %%t in ('powershell -NoProfile -Command "Get-Date -Format yyyy-MM-dd-HHmm"') do set STAMP=%%t
where pg_dump >nul 2>&1 || (echo [FAIL] pg_dump is not installed or not on PATH. & exit /b 1)
pg_dump -h localhost -p 5432 -U flowops_user -Fc -f "backups\flowops-%STAMP%.dump" flowops
if errorlevel 1 (echo [FAIL] Backup failed. & exit /b 1)
echo [OK] Backup created: backups\flowops-%STAMP%.dump
endlocal
