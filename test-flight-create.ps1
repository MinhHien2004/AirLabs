# Test API Flight
Write-Host "Waiting for server..." -ForegroundColor Yellow
Start-Sleep -Seconds 2

# Test 1: Tạo flight mới
Write-Host "`n=== Test 1: Tạo flight mới ===" -ForegroundColor Green
$body1 = @'
{
    "airline_iata": "VN",
    "flight_iata": "VN999",
    "flight_number": "999",
    "dep_iata": "HAN",
    "dep_time": "2026-01-08T10:00:00",
    "arr_iata": "SGN",
    "arr_time": "2026-01-08T12:00:00",
    "status": "scheduled"
}
'@

try {
    $result = Invoke-WebRequest -Uri "http://localhost:8080/api/flights" -Method POST -Body $body1 -ContentType "application/json" -UseBasicParsing
    Write-Host "SUCCESS: Status $($result.StatusCode)" -ForegroundColor Green
    Write-Host $result.Content
} catch {
    Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host $reader.ReadToEnd()
    }
}

# Test 2: Tạo duplicate (phải lỗi)
Write-Host "`n=== Test 2: Tạo duplicate (phải lỗi) ===" -ForegroundColor Green
try {
    $result = Invoke-WebRequest -Uri "http://localhost:8080/api/flights" -Method POST -Body $body1 -ContentType "application/json" -UseBasicParsing
    Write-Host "WRONG: Không nên tạo được!" -ForegroundColor Red
} catch {
    Write-Host "CORRECT: Đã chặn duplicate!" -ForegroundColor Green
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host $reader.ReadToEnd()
    }
}

# Test 3: Tạo flight với dep_time khác
Write-Host "`n=== Test 3: Tạo flight với dep_time khác ===" -ForegroundColor Green
$body2 = @'
{
    "airline_iata": "VN",
    "flight_iata": "VN999",
    "flight_number": "999",
    "dep_iata": "HAN",
    "dep_time": "2026-01-08T14:00:00",
    "arr_iata": "SGN",
    "arr_time": "2026-01-08T16:00:00",
    "status": "scheduled"
}
'@

try {
    $result = Invoke-WebRequest -Uri "http://localhost:8080/api/flights" -Method POST -Body $body2 -ContentType "application/json" -UseBasicParsing
    Write-Host "SUCCESS: Status $($result.StatusCode)" -ForegroundColor Green
    Write-Host $result.Content
} catch {
    Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`nDone!" -ForegroundColor Cyan

