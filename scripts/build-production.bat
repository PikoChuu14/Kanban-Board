@echo off
setlocal
cd /d "%~dp0..\frontend"
echo Building frontend...
call npm ci
if errorlevel 1 (echo FRONTEND INSTALL FAILED & exit /b 1)
call npm run build
if errorlevel 1 (echo FRONTEND BUILD FAILED & exit /b 1)
cd /d "%~dp0..\backend"
echo Building backend and bundling frontend...
call mvnw.cmd -DskipTests clean package
if errorlevel 1 (echo BUILD FAILED & exit /b 1)
echo.
echo Built artifact: %cd%\target\kovax-flowops.jar
echo Final handover package: %cd%\..\dist\Kovax-FlowOps\
endlocal
