@echo off
cd /d "%~dp0"
set PROFILE=%1
if "%PROFILE%"=="" set PROFILE=merged
if not exist renderer\materials mkdir renderer\materials
lazurite build proj -p %PROFILE% -o renderer\materials
