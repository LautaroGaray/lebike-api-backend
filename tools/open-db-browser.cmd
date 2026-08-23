@echo off
setlocal

set "BASE=%~dp0..\springboot-sqlite-scaffold"
set "DB_BROWSER_EXE=%BASE%\tools\sqlite-gui\portable\DB Browser for SQLite.exe"
set "DB_FILE=%USERPROFILE%\springboot-sqlite-scaffold-local.db"

if not exist "%DB_BROWSER_EXE%" (
  echo [ERROR] No se encontro DB Browser en:
  echo         %DB_BROWSER_EXE%
  exit /b 1
)

if not exist "%DB_FILE%" (
  echo [WARN] No existe aun la DB:
  echo        %DB_FILE%
  echo        Se abrira DB Browser igual para que la selecciones manualmente.
  start "" "%DB_BROWSER_EXE%"
  exit /b 0
)

start "" "%DB_BROWSER_EXE%" "%DB_FILE%"
exit /b 0

