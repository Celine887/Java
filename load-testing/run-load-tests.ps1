# PowerShell script to run load tests with and without cache
# Usage: .\run-load-tests.ps1

param(
    [string]$JMeterPath = "jmeter",
    [int]$Threads = 16,
    [int]$Duration = 300
)

Write-Host "=== Link Tracker Load Testing Script ===" -ForegroundColor Cyan
Write-Host ""

# Check if JMeter is installed
try {
    & $JMeterPath -v | Out-Null
    Write-Host "[OK] JMeter found" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] JMeter not found. Please install JMeter or specify path with -JMeterPath" -ForegroundColor Red
    exit 1
}

# Create results directory
$resultsDir = "results"
if (!(Test-Path $resultsDir)) {
    New-Item -ItemType Directory -Path $resultsDir | Out-Null
    Write-Host "[OK] Created results directory" -ForegroundColor Green
}

# Function to wait for Scrapper API
function Wait-ForScrapperAPI {
    Write-Host "Waiting for Scrapper API to be ready..." -ForegroundColor Yellow
    $maxAttempts = 30
    $attempt = 0
    
    while ($attempt -lt $maxAttempts) {
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:8081/actuator/health" -TimeoutSec 2 -ErrorAction Stop
            if ($response.StatusCode -eq 200) {
                Write-Host "[OK] Scrapper API is ready" -ForegroundColor Green
                return $true
            }
        } catch {
            $attempt++
            Start-Sleep -Seconds 2
        }
    }
    
    Write-Host "[ERROR] Scrapper API is not responding" -ForegroundColor Red
    return $false
}

# Test 1: WITHOUT cache
Write-Host ""
Write-Host "=== Test 1: WITHOUT Cache ===" -ForegroundColor Cyan
Write-Host "Please ensure:"
Write-Host "  1. Valkey caching is DISABLED in application.yaml"
Write-Host "  2. Scrapper API is running on port 8081"
Write-Host ""
$continue = Read-Host "Continue with Test 1? (y/n)"

if ($continue -eq "y") {
    if (Wait-ForScrapperAPI) {
        Write-Host "Starting load test WITHOUT cache..." -ForegroundColor Yellow
        
        & $JMeterPath -n `
            -t "link-tracker-load-test.jmx" `
            -JTHREADS=$Threads `
            -JDURATION=$Duration `
            -l "$resultsDir/no-cache-results.jtl" `
            -e -o "$resultsDir/no-cache-report"
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "[OK] Test 1 completed successfully" -ForegroundColor Green
            Write-Host "Report: $resultsDir/no-cache-report/index.html" -ForegroundColor Cyan
        } else {
            Write-Host "[ERROR] Test 1 failed" -ForegroundColor Red
        }
    }
}

# Test 2: WITH cache
Write-Host ""
Write-Host "=== Test 2: WITH Cache ===" -ForegroundColor Cyan
Write-Host "Please ensure:"
Write-Host "  1. Valkey caching is ENABLED in application.yaml"
Write-Host "  2. Valkey is running on port 6379"
Write-Host "  3. Scrapper API is restarted with cache enabled"
Write-Host ""
$continue = Read-Host "Continue with Test 2? (y/n)"

if ($continue -eq "y") {
    # Check if Valkey is running
    try {
        $valkeyRunning = docker ps --filter "name=valkey-standalone" --filter "status=running" --format "{{.Names}}"
        if ($valkeyRunning -eq "valkey-standalone") {
            Write-Host "[OK] Valkey is running" -ForegroundColor Green
        } else {
            Write-Host "[WARNING] Valkey container not found. Starting..." -ForegroundColor Yellow
            docker start valkey-standalone
            Start-Sleep -Seconds 3
        }
    } catch {
        Write-Host "[WARNING] Could not check Valkey status" -ForegroundColor Yellow
    }
    
    if (Wait-ForScrapperAPI) {
        Write-Host "Starting load test WITH cache..." -ForegroundColor Yellow
        
        & $JMeterPath -n `
            -t "link-tracker-load-test.jmx" `
            -JTHREADS=$Threads `
            -JDURATION=$Duration `
            -l "$resultsDir/with-cache-results.jtl" `
            -e -o "$resultsDir/with-cache-report"
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "[OK] Test 2 completed successfully" -ForegroundColor Green
            Write-Host "Report: $resultsDir/with-cache-report/index.html" -ForegroundColor Cyan
        } else {
            Write-Host "[ERROR] Test 2 failed" -ForegroundColor Red
        }
    }
}

# Summary
Write-Host ""
Write-Host "=== Load Testing Complete ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Results:"
Write-Host "  - Without cache: $resultsDir/no-cache-report/index.html" -ForegroundColor Yellow
Write-Host "  - With cache:    $resultsDir/with-cache-report/index.html" -ForegroundColor Yellow
Write-Host ""
Write-Host "Next steps:"
Write-Host "  1. Open HTML reports in browser"
Write-Host "  2. Compare metrics (RPS, response time, percentiles)"
Write-Host "  3. Create comparison table for README.md"
Write-Host ""
