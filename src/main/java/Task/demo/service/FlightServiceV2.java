package Task.demo.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import Task.demo.Repository.FlightRepository;
import Task.demo.config.AirLabsConfig;
import Task.demo.entity.Flight;
import Task.demo.service.FlightCacheServiceV2.CacheResult;

/**
 * Service xử lý logic nghiệp vụ cho Flight
 * Triển khai Multi-Layer Smart Caching theo RedisCacheWorkFlow.md:
 * 
 * 1. Negative Cache Check - Chặn request đến API không hợp lệ
 * 2. Data Cache Check - Kiểm tra cache với Logical Expiration
 * 3. Async Update - Cập nhật ngầm khi cache hết hạn logic
 * 4. Frequency-Based TTL - TTL động dựa trên mức độ quan tâm
 * 5. Hash-based Dedup - So sánh hash để tránh ghi trùng DB
 */
@Service
public class FlightServiceV2 {
    
    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private FlightCacheServiceV2 cacheService;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private AirLabsConfig airLabsConfig;

    // ===========================================
    // MAIN ENTRY POINTS
    // ===========================================
    
    /**
     * Xử lý request lấy arrivals theo IATA
     * Flow theo RedisCacheWorkFlow.md:
     * 1. Check Negative Cache
     * 2. Check Data Cache (với Logical Expiration)
     * 3. Fetch từ API nếu cần
     * 4. Sync vào DB với Dedup
     */
    public List<Flight> getArrivals(String iata) {
        return getFlights(iata, "arrivals");
    }
    
    /**
     * Xử lý request lấy departures theo IATA
     */
    public List<Flight> getDepartures(String iata) {
        return getFlights(iata, "departures");
    }
    
    /**
     * Logic chung cho cả arrivals và departures
     * OPTIMIZED: Skip DB check if cache hit
     */
    private List<Flight> getFlights(String iata, String type) {
        System.out.println("\n========== Processing " + type + " for IATA: " + iata + " ==========");
        
        // STEP 1: Check Negative Cache
        if (cacheService.isNegativeCached(iata, type)) {
            System.out.println("STEP 1: Negative cache HIT - returning empty");
            return new ArrayList<>();
        }
        System.out.println("STEP 1: Negative cache MISS - continue");
        
        // STEP 2: Check Data Cache
        CacheResult cacheResult = cacheService.getFlightsFromCache(iata, type);
        
        if (cacheResult.isCacheHit()) {
            System.out.println("STEP 2: Cache HIT - found " + cacheResult.getFlights().size() + " flights");
            
            // STEP 3: Check Logical Expiration
            if (!cacheResult.isLogicallyExpired()) {
                // Còn hạn logic -> Trả về ngay (FAST PATH)
                System.out.println("STEP 3: Cache is FRESH - returning immediately");
                return cacheResult.getFlights();
            } else {
                // Hết hạn logic -> Trả về data cũ + trigger async update
                System.out.println("STEP 3: Cache is STALE - returning old data + async update");
                triggerAsyncUpdate(iata, type);
                return cacheResult.getFlights();
            }
        }
        
        System.out.println("STEP 2: Cache MISS - fetching from API directly");
        
        // STEP 4: Cache Miss - Fetch from API (skip DB to reduce latency on Render)
        // Trên cloud environment, network latency đến external API thường tốt hơn
        // và dữ liệu flight cần realtime nên ưu tiên API
        return fetchAndSync(iata, type);
    }
    
    /**
     * Trigger async update khi cache hết hạn logic
     * User không phải chờ - nhận data cũ ngay lập tức
     */
    @Async
    public CompletableFuture<Void> triggerAsyncUpdate(String iata, String type) {
        System.out.println("[ASYNC] Starting background update for " + type + ":" + iata);
        try {
            fetchAndSync(iata, type);
            System.out.println("[ASYNC] Completed background update for " + type + ":" + iata);
        } catch (Exception e) {
            System.err.println("[ASYNC] Error in background update: " + e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

    // ===========================================
    // DATA FETCHING
    // ===========================================
    
    /**
     * Fetch data từ API và sync vào DB + Cache
     */
    private List<Flight> fetchAndSync(String iata, String type) {
        // Fetch từ API
        List<Flight> apiFlights = fetchFromAPI(iata, type);
        
        // Nếu API trả về rỗng -> set negative cache
        if (apiFlights == null || apiFlights.isEmpty()) {
            System.out.println("API returned empty - setting negative cache");
            cacheService.setNegativeCache(iata, type);
            return new ArrayList<>();
        }
        
        System.out.println("API returned " + apiFlights.size() + " flights");
        
        // Increment counter
        cacheService.incrementCounter(iata, type);
        
        // Determine TTL based on frequency
        long logicalTtl = cacheService.determineLogicalTtl(iata, type);
        
        // Sync to DB with dedup logic
        List<Flight> syncedFlights = syncToDatabase(iata, type, apiFlights);
        
        // Update cache
        cacheService.cacheFlights(iata, type, syncedFlights, logicalTtl);
        
        return syncedFlights;
    }
    
    /**
     * Lấy dữ liệu từ Database
     * Giữ method này cho fallback nếu cần
     */
    @SuppressWarnings("unused")
    private List<Flight> getFromDatabase(String iata, String type) {
        try {
            if ("arrivals".equals(type)) {
                return flightRepository.findByArrIata(iata);
            } else {
                return flightRepository.findByDepIata(iata);
            }
        } catch (Exception e) {
            System.err.println("Error fetching from DB: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Gọi AirLabs API để lấy dữ liệu
     */
    @SuppressWarnings("unchecked")
    private List<Flight> fetchFromAPI(String iata, String type) {
        try {
            String paramName = "arrivals".equals(type) ? "arr_iata" : "dep_iata";
            String url = airLabsConfig.getBaseUrl() + "/schedules?" + paramName + "=" + iata 
                    + "&api_key=" + airLabsConfig.getApiKey();
            
            System.out.println("Calling API: " + url.replace(airLabsConfig.getApiKey(), "***"));
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && response.containsKey("response")) {
                List<Map<String, Object>> apiFlights = (List<Map<String, Object>>) response.get("response");
                List<Flight> flights = new ArrayList<>();
                
                for (Map<String, Object> apiData : apiFlights) {
                    Flight flight = mapToFlight(apiData);
                    flights.add(flight);
                }
                
                return flights;
            }
        } catch (Exception e) {
            System.err.println("Error fetching from API: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    // ===========================================
    // DATABASE SYNC WITH DEDUP
    // ===========================================
    
    /**
     * Đồng bộ dữ liệu vào Database với logic dedup - BATCH OPTIMIZED
     * Giảm N+1 queries bằng cách batch check và batch save
     */
    private List<Flight> syncToDatabase(String iata, String type, List<Flight> newFlights) {
        System.out.println("Starting optimized DB sync for " + newFlights.size() + " flights");
        
        if (newFlights.isEmpty()) {
            return new ArrayList<>();
        }
        
        // STEP 1: Build composite keys cho tất cả flights
        Map<String, Flight> newFlightsMap = new HashMap<>();
        for (Flight f : newFlights) {
            String key = f.getFlightIata() + "|" + f.getDepTime();
            newFlightsMap.put(key, f);
        }
        
        // STEP 2: Batch query - tìm tất cả existing flights trong 1 query
        java.util.Set<String> compositeKeys = newFlightsMap.keySet();
        List<Flight> existingFlights = flightRepository.findByCompositeKeys(compositeKeys);
        
        // Build map của existing flights
        Map<String, Flight> existingMap = new HashMap<>();
        for (Flight f : existingFlights) {
            String key = f.getFlightIata() + "|" + f.getDepTime();
            existingMap.put(key, f);
        }
        
        // STEP 3: Classify flights: INSERT vs UPDATE vs SKIP
        List<Flight> toInsert = new ArrayList<>();
        List<Flight> toUpdate = new ArrayList<>();
        List<Flight> unchanged = new ArrayList<>();
        
        for (Map.Entry<String, Flight> entry : newFlightsMap.entrySet()) {
            String key = entry.getKey();
            Flight newFlight = entry.getValue();
            Flight existing = existingMap.get(key);
            
            if (existing == null) {
                // New flight - INSERT
                toInsert.add(newFlight);
            } else {
                // Check if changed
                if (cacheService.hasChanged(iata, type, newFlight)) {
                    newFlight.setId(existing.getId());
                    toUpdate.add(newFlight);
                } else {
                    unchanged.add(existing);
                }
            }
        }
        
        // STEP 4: Batch save
        List<Flight> syncedFlights = new ArrayList<>();
        
        if (!toInsert.isEmpty()) {
            List<Flight> inserted = flightRepository.saveAll(toInsert);
            syncedFlights.addAll(inserted);
        }
        
        if (!toUpdate.isEmpty()) {
            List<Flight> updated = flightRepository.saveAll(toUpdate);
            syncedFlights.addAll(updated);
        }
        
        syncedFlights.addAll(unchanged);
        
        System.out.println("DB Sync complete: " + toInsert.size() + " inserted, " + 
                toUpdate.size() + " updated, " + unchanged.size() + " skipped");
        
        return syncedFlights;
    }

    // ===========================================
    // UTILITY METHODS
    // ===========================================
    
    /**
     * Convert Map từ API sang Flight entity
     */
    private Flight mapToFlight(Map<String, Object> data) {
        Flight flight = new Flight();
        flight.setAirlineIata((String) data.get("airline_iata"));
        flight.setAirlineIcao((String) data.get("airline_icao"));
        flight.setFlightIata((String) data.get("flight_iata"));
        flight.setFlightIcao((String) data.get("flight_icao"));
        flight.setFlightNumber((String) data.get("flight_number"));
        flight.setDepIata((String) data.get("dep_iata"));
        flight.setDepIcao((String) data.get("dep_icao"));
        flight.setDepTerminal((String) data.get("dep_terminal"));
        flight.setDepGate((String) data.get("dep_gate"));
        flight.setDepTime((String) data.get("dep_time"));
        flight.setDepTimeUtc((String) data.get("dep_time_utc"));
        flight.setDepTimeTs(data.get("dep_time_ts") != null ? ((Number) data.get("dep_time_ts")).longValue() : null);
        flight.setArrIata((String) data.get("arr_iata"));
        flight.setArrIcao((String) data.get("arr_icao"));
        flight.setArrTerminal((String) data.get("arr_terminal"));
        flight.setArrGate((String) data.get("arr_gate"));
        flight.setArrBaggage((String) data.get("arr_baggage"));
        flight.setArrTime((String) data.get("arr_time"));
        flight.setArrTimeUtc((String) data.get("arr_time_utc"));
        flight.setArrTimeTs(data.get("arr_time_ts") != null ? ((Number) data.get("arr_time_ts")).longValue() : null);
        flight.setCsAirlineIata((String) data.get("cs_airline_iata"));
        flight.setCsFlightNumber((String) data.get("cs_flight_number"));
        flight.setCsFlightIata((String) data.get("cs_flight_iata"));
        flight.setStatus((String) data.get("status"));
        flight.setDuration(data.get("duration") != null ? ((Number) data.get("duration")).intValue() : null);
        flight.setDelayed(data.get("delayed") != null ? ((Number) data.get("delayed")).intValue() : null);
        flight.setDepDelayed(data.get("dep_delayed") != null ? ((Number) data.get("dep_delayed")).intValue() : null);
        flight.setArrDelayed(data.get("arr_delayed") != null ? ((Number) data.get("arr_delayed")).intValue() : null);
        flight.setAircraftIcao((String) data.get("aircraft_icao"));
        return flight;
    }
    
    /**
     * Xóa cache cho một IATA
     */
    public void clearCache(String iata) {
        cacheService.clearCache(iata);
    }

    // ===========================================
    // LEGACY SUPPORT - Backward compatibility với API cũ
    // ===========================================
    
    /**
     * [LEGACY] Xử lý arrivals từ frontend hoặc tự động fetch
     */
    public List<Flight> processArrivals(String iata, List<Flight> flights) {
        if (flights == null || flights.isEmpty()) {
            return getArrivals(iata);
        }
        
        // Frontend gửi dữ liệu - sync vào DB và cache
        cacheService.incrementCounter(iata, "arrivals");
        long logicalTtl = cacheService.determineLogicalTtl(iata, "arrivals");
        List<Flight> synced = syncToDatabase(iata, "arrivals", flights);
        cacheService.cacheFlights(iata, "arrivals", synced, logicalTtl);
        return synced;
    }
    
    /**
     * [LEGACY] Xử lý departures từ frontend hoặc tự động fetch
     */
    public List<Flight> processDepartures(String iata, List<Flight> flights) {
        if (flights == null || flights.isEmpty()) {
            return getDepartures(iata);
        }
        
        // Frontend gửi dữ liệu - sync vào DB và cache
        cacheService.incrementCounter(iata, "departures");
        long logicalTtl = cacheService.determineLogicalTtl(iata, "departures");
        List<Flight> synced = syncToDatabase(iata, "departures", flights);
        cacheService.cacheFlights(iata, "departures", synced, logicalTtl);
        return synced;
    }
}
