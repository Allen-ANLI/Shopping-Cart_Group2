@echo off
setlocal
if exist "%~dp0.local\tools\node-v24.19.0-win-x64\node.exe" set "PATH=%~dp0.local\tools\node-v24.19.0-win-x64;%PATH%"
where node >nul 2>nul
if errorlevel 1 (
  echo Node.js is missing. Install the Node.js version described in README.md.
  pause
  exit /b 1
)
cd /d "%~dp0frontend"
if not exist node_modules (
  call npm ci
  if errorlevel 1 exit /b 1
)
call npm run dev
pause
