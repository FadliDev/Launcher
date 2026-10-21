@echo off
setlocal EnableExtensions
set "VERSION=8.11.1"
set "EXPECTED=f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6"
if "%GRADLE_USER_HOME%"=="" set "GRADLE_USER_HOME=%USERPROFILE%\.gradle"
set "CACHE=%GRADLE_USER_HOME%\sally-wrapper"
set "DIST=%CACHE%\gradle-%VERSION%"
set "GRADLE_BIN=%DIST%\bin\gradle.bat"
if exist "%GRADLE_BIN%" goto run
if not exist "%CACHE%" mkdir "%CACHE%"
set "ZIP=%CACHE%\gradle-%VERSION%-bin.zip"
echo Gradle %VERSION% belum tersedia; mengunduh distribusi resmi...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%VERSION%-bin.zip' -OutFile '%ZIP%'; $actual=(Get-FileHash '%ZIP%' -Algorithm SHA256).Hash.ToLower(); if ($actual -ne '%EXPECTED%') { Remove-Item '%ZIP%' -Force; throw 'Checksum Gradle tidak cocok' }; Expand-Archive -Path '%ZIP%' -DestinationPath '%CACHE%' -Force"
if errorlevel 1 exit /b 1
:run
call "%GRADLE_BIN%" -p "%~dp0" %*
exit /b %ERRORLEVEL%
