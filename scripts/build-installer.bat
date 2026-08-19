@echo off
setlocal
cd /d "%~dp0.."
set "INSTALLER_OUTPUT=%CD%\dist\installer\KovaxFlowOps-Setup.exe"
if exist "%INSTALLER_OUTPUT%" del /f /q "%INSTALLER_OUTPUT%"
if exist "%INSTALLER_OUTPUT%" (
  echo [ERROR] Could not remove previous installer: %INSTALLER_OUTPUT%
  exit /b 1
)
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\build-installer.ps1
if errorlevel 1 exit /b 1
endlocal
