@echo off
setlocal
cd /d "%~dp0.."
set "BACKUP=%~1"
if "%BACKUP%"=="" (set /p BACKUP=Enter backup path: )
if not exist "%BACKUP%" (echo [FAIL] Backup not found. & exit /b 1)
echo WARNING: This replaces the current FlowOps database data.
choice /m "Continue"
if errorlevel 2 exit /b 0
set PGPASSFILE=%cd%\config\pgpass.conf
where pg_restore >nul 2>&1 || (echo [FAIL] pg_restore is not installed or not on PATH. & exit /b 1)
pg_restore -h localhost -p 5432 -U flowops_user --clean --if-exists --no-owner -d flowops "%BACKUP%"
if errorlevel 1 (echo [FAIL] Restore failed. & exit /b 1)
echo [OK] Restore completed. Restart the application.
endlocal
