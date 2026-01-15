# Test Redis Cache Flow
Write-Host "=== Testing Redis Cache Flow ===" -ForegroundColor Cyan

# Test 1: First request (should fetch from API)
Write-Host "`n1. First request for HAN (should fetch from API)..." -ForegroundColor Yellow
$response1 = Invoke-RestMethod -Uri "http://localhost:8080/api/schedules/arrivals/HAN" -Method Get
Write-Host "Response count: $($response1.Count)" -ForegroundColor Green

Start-Sleep -Seconds 2

# Test 2: Second request (should use cache)
Write-Host "`n2. Second request for HAN (should use Redis cache)..." -ForegroundColor Yellow
$response2 = Invoke-RestMethod -Uri "http://localhost:8080/api/schedules/arrivals/HAN" -Method Get
Write-Host "Response count: $($response2.Count)" -ForegroundColor Green

# Test 3: Different IATA (should fetch from API again)
Write-Host "`n3. Request for DAD (new IATA, should fetch from API)..." -ForegroundColor Yellow
$response3 = Invoke-RestMethod -Uri "http://localhost:8080/api/schedules/arrivals/DAD" -Method Get
Write-Host "Response count: $($response3.Count)" -ForegroundColor Green

Write-Host "`n=== Test completed ===" -ForegroundColor Cyan
Write-Host "Check the backend logs for 'Redis' messages"
