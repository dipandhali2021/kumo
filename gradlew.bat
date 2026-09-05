@echo off
where py >nul 2>nul
if %ERRORLEVEL% EQU 0 (
 py -3 "%~dp0tools\gradle.py" %*
) else (
 python "%~dp0tools\gradle.py" %*
)
exit /b %ERRORLEVEL%
