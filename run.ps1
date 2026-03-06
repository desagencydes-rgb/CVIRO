# CVPro - Autonomous Run Script (PowerShell)
# This script builds the project and launches it using an embedded Tomcat 10 container.

$MAVEN_EXEC = "C:\Users\HP\.maven\maven-3.9.12\bin\mvn.cmd"

Write-Host "===============================================" -ForegroundColor Cyan
Write-Host "   CVPro - Autonomous Application Runner       " -ForegroundColor Cyan
Write-Host "===============================================" -ForegroundColor Cyan

if (!(Test-Path $MAVEN_EXEC)) {
    Write-Host "[ERROR] Maven executable not found at: $MAVEN_EXEC" -ForegroundColor Red
    Write-Host "Please ensure Maven is installed or update the MAVEN_EXEC path in this script." -ForegroundColor Yellow
    exit 1
}

Write-Host "[Step 1/2] Building application (skipping tests)..." -ForegroundColor Yellow
& $MAVEN_EXEC clean package -DskipTests

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Build failed. Review Maven output above." -ForegroundColor Red
    exit 1
}

Write-Host "[Step 2/2] Starting embedded Tomcat 10 (Port 8080)..." -ForegroundColor Green
Write-Host "-----------------------------------------------" -ForegroundColor Green
Write-Host "Once started, visit: http://localhost:8080/" -ForegroundColor Green
Write-Host "Press Ctrl+C to stop the server." -ForegroundColor Green
Write-Host "-----------------------------------------------" -ForegroundColor Green

& $MAVEN_EXEC cargo:run
