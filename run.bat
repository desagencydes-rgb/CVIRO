@echo off
set MAVEN_EXEC=C:\Users\HP\.maven\maven-3.9.12\bin\mvn.cmd

echo ===============================================
echo    CVPro - Autonomous Application Runner       
echo ===============================================

if not exist "%MAVEN_EXEC%" (
    echo [ERROR] Maven executable not found at: %MAVEN_EXEC%
    echo Please ensure Maven is installed or update the MAVEN_EXEC path in this script.
    pause
    exit /b 1
)

echo [Step 1/2] Building application (skipping tests)...
call "%MAVEN_EXEC%" clean package -DskipTests

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Build failed. Review Maven output above.
    pause
    exit /b 1
)

echo [Step 2/2] Starting embedded Tomcat 10 (Port 8080)...
echo -----------------------------------------------
echo Once started, visit: http://localhost:8080/
echo Press Ctrl+C to stop the server.
echo -----------------------------------------------

call "%MAVEN_EXEC%" cargo:run
pause
