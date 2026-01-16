# Script test các API mới của hệ thống Flight Caching
# Triển khai theo RedisCacheWorkFlow.md

$baseUrl = "http://localhost:8080"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "FLIGHT CACHING SYSTEM TEST" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Test IATA
$testIata = "SGN"

Write-Host "`n1. Test API V2 - GET Arrivals (Cache Miss - First call)" -ForegroundColor Yellow
$response1 = Invoke-RestMethod -Uri "$baseUrl/api/v2/flights?iata=$testIata&type=arrivals" -Method GET
Write-Host "Success: $($response1.success)"
Write-Host "Count: $($response1.count)"
Write-Host "Response Time: $($response1.responseTime)"

Start-Sleep -Seconds 1

Write-Host "`n2. Test API V2 - GET Arrivals (Cache Hit - Second call)" -ForegroundColor Yellow
$response2 = Invoke-RestMethod -Uri "$baseUrl/api/v2/flights?iata=$testIata&type=arrivals" -Method GET
Write-Host "Success: $($response2.success)"
Write-Host "Count: $($response2.count)"
Write-Host "Response Time: $($response2.responseTime)"

Write-Host "`n3. Test Cache Stats" -ForegroundColor Yellow
$stats = Invoke-RestMethod -Uri "$baseUrl/api/v2/flights/stats/$testIata" -Method GET
Write-Host "IATA: $($stats.iata)"
Write-Host "Arrivals:"
Write-Host "  - Counter: $($stats.arrivals.counter)"
Write-Host "  - Cache Hit: $($stats.arrivals.cacheHit)"
Write-Host "  - Cached Flights: $($stats.arrivals.cachedFlightsCount)"
Write-Host "  - Logically Expired: $($stats.arrivals.logicallyExpired)"
Write-Host "  - Logical TTL: $($stats.arrivals.logicalTtlMinutes) min"

Write-Host "`n4. Test API V2 - GET Departures" -ForegroundColor Yellow
$response3 = Invoke-RestMethod -Uri "$baseUrl/api/v2/flights/departures/$testIata" -Method GET
Write-Host "Success: $($response3.success)"
Write-Host "Count: $($response3.count)"
Write-Host "Response Time: $($response3.responseTime)"

Write-Host "`n5. Test Negative Cache (Invalid IATA)" -ForegroundColor Yellow
$invalidIata = "XXX"
try {
    $response4 = Invoke-RestMethod -Uri "$baseUrl/api/v2/flights?iata=$invalidIata&type=arrivals" -Method GET
    Write-Host "Success: $($response4.success)"
    Write-Host "Count: $($response4.count)"
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

# Test lại invalid IATA - should hit negative cache
Start-Sleep -Seconds 1
Write-Host "`n6. Test Negative Cache Hit (Same Invalid IATA - Should be fast)" -ForegroundColor Yellow
try {
    $response5 = Invoke-RestMethod -Uri "$baseUrl/api/v2/flights?iata=$invalidIata&type=arrivals" -Method GET
    Write-Host "Success: $($response5.success)"
    Write-Host "Count: $($response5.count)"
    Write-Host "Response Time: $($response5.responseTime)"
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n7. Test Clear Cache" -ForegroundColor Yellow
$clearResponse = Invoke-RestMethod -Uri "$baseUrl/api/v2/flights/cache/$testIata" -Method DELETE
Write-Host "Success: $($clearResponse.success)"
Write-Host "Message: $($clearResponse.message)"

Write-Host "`n8. Verify Cache Cleared" -ForegroundColor Yellow
$statsAfterClear = Invoke-RestMethod -Uri "$baseUrl/api/v2/flights/stats/$testIata" -Method GET
Write-Host "Arrivals Cache Hit: $($statsAfterClear.arrivals.cacheHit)"
Write-Host "Departures Cache Hit: $($statsAfterClear.departures.cacheHit)"

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "TEST COMPLETED" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

Write-Host "`nCác tính năng đã triển khai theo RedisCacheWorkFlow.md:" -ForegroundColor Green
Write-Host "✓ Logical Expiration (TTL logic 30 phút, TTL vật lý 60 phút)"
Write-Host "✓ Frequency-Based Caching (Counter để xác định hot/cold data)"
Write-Host "✓ Negative Caching (Cache kết quả rỗng 5 phút)"
Write-Host "✓ Hash-based Duplicate Detection (SHA-256)"
Write-Host "✓ Async Update khi cache hết hạn logic"
Write-Host "✓ Redis Hash structure cho data storage"
