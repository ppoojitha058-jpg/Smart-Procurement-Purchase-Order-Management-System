@echo off
setlocal
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"
rem Find system mvn executable. Prefer a second result if present (avoids the wrapper itself)
set "FIRST="
for /f "delims=" %%i in ('where mvn 2^>nul') do (
  if not defined FIRST (
    set "FIRST=%%i"
    set "MVN_CMD=%%i"
  ) else (
    set "MVN_CMD=%%i"
    goto :found
  )
)
if defined MVN_CMD goto :found
echo Maven executable not found in PATH.
exit /b 1

:found
"%MVN_CMD%" %*
endlocal
