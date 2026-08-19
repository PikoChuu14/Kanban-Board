@echo off
setlocal EnableExtensions
cd /d "%~dp0.."
echo ========================================
echo            FlowOps Setup
echo ========================================
where java >nul 2>&1 || (echo [FAIL] Java 21 is not installed. & exit /b 1)
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do set JAVA_VERSION=%%~v
echo [OK] Java detected: %JAVA_VERSION%
if not exist app\flowops.jar (echo [FAIL] app\flowops.jar not found. & exit /b 1)
if not exist config mkdir config
if not exist logs mkdir logs
if not exist backups mkdir backups
if not exist config\application.properties copy config\application.properties.example config\application.properties >nul
findstr /c:"CHANGE_ME" config\application.properties >nul && (echo [FAIL] Edit config\application.properties and replace CHANGE_ME. & exit /b 1)
powershell -NoProfile -ExecutionPolicy Bypass -File installer\scripts\ensure-jwt-secret.ps1 -SecretsPath config\secrets.properties
if errorlevel 1 (echo [FAIL] JWT security secret validation failed. & exit /b 1)
findstr /c:"app.bootstrap.admin.email=" config\secrets.properties >nul || (
  set /p ADMIN_NAME=First administrator name: 
  set /p ADMIN_EMAIL=First administrator email: 
  set /p ADMIN_PASSWORD=First administrator password: 
  >>config\secrets.properties echo app.bootstrap.admin.name=%ADMIN_NAME%
  >>config\secrets.properties echo app.bootstrap.admin.email=%ADMIN_EMAIL%
  >>config\secrets.properties echo app.bootstrap.admin.password=%ADMIN_PASSWORD%
  echo [OK] First administrator queued for creation on first start
)
if not exist config\pgpass.conf (
  for /f "tokens=2 delims==" %%p in ('findstr /b "spring.datasource.password=" config\application.properties') do set DBPASS=%%p
  if defined DBPASS echo localhost:5432:flowops:flowops_user:%DBPASS%>config\pgpass.conf
)
echo [OK] Application configuration found
echo [OK] Security secret is persistent
echo [INFO] PostgreSQL is checked when the application starts.
echo.
echo Setup complete. Run scripts\start.bat next.
endlocal
