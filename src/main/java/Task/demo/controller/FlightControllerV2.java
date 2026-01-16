package Task.demo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import Task.demo.entity.Flight;
import Task.demo.service.FlightCacheServiceV2;
import Task.demo.service.FlightServiceV2;

/**
 * REST Controller cho Flight API v2
 * Sử dụng Multi-Layer Smart Caching theo RedisCacheWorkFlow.md
 * 
 * API Endpoints:
 * - GET /api/v2/flights?iata={code}&type={arrivals|departures}
 * - GET /api/v2/flights/arrivals/{iata}
 * - GET /api/v2/flights/departures/{iata}
 * - DELETE /api/v2/flights/cache/{iata}
 * - GET /api/v2/flights/stats/{iata}
 */
@RestController
@RequestMapping("/api/v2/flights")
public class FlightControllerV2 {
    
    @Autowired
    private FlightServiceV2 flightService;
    
    @Autowired
    private FlightCacheServiceV2 cacheService;

    /**
     * API chính để lấy thông tin chuyến bay
     * @param iata Mã sân bay (VD: SGN, HAN)
     * @param type Loại: arrivals hoặc departures (default: arrivals)
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getFlights(
            @RequestParam String iata,
            @RequestParam(defaultValue = "arrivals") String type) {
        
        long startTime = System.currentTimeMillis();
        Map<String, Object> response = new HashMap<>();
        
        try {
            iata = iata.toUpperCase();
            List<Flight> flights;
            
            if ("departures".equalsIgnoreCase(type)) {
                flights = flightService.getDepartures(iata);
            } else {
                flights = flightService.getArrivals(iata);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            
            response.put("success", true);
            response.put("iata", iata);
            response.put("type", type);
            response.put("count", flights.size());
            response.put("data", flights);
            response.put("responseTime", duration + "ms");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * API lấy arrivals cho một sân bay
     */
    @GetMapping("/arrivals/{iata}")
    public ResponseEntity<Map<String, Object>> getArrivals(@PathVariable String iata) {
        return getFlights(iata, "arrivals");
    }
    
    /**
     * API lấy departures cho một sân bay
     */
    @GetMapping("/departures/{iata}")
    public ResponseEntity<Map<String, Object>> getDepartures(@PathVariable String iata) {
        return getFlights(iata, "departures");
    }
    
    /**
     * API để xử lý arrivals từ frontend (backward compatible)
     */
    @PostMapping("/arrivals/{iata}")
    public ResponseEntity<Map<String, Object>> processArrivals(
            @PathVariable String iata,
            @RequestBody(required = false) List<Flight> flights) {
        
        long startTime = System.currentTimeMillis();
        Map<String, Object> response = new HashMap<>();
        
        try {
            iata = iata.toUpperCase();
            List<Flight> result = flightService.processArrivals(iata, flights);
            long duration = System.currentTimeMillis() - startTime;
            
            response.put("success", true);
            response.put("iata", iata);
            response.put("type", "arrivals");
            response.put("count", result.size());
            response.put("data", result);
            response.put("responseTime", duration + "ms");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * API để xử lý departures từ frontend (backward compatible)
     */
    @PostMapping("/departures/{iata}")
    public ResponseEntity<Map<String, Object>> processDepartures(
            @PathVariable String iata,
            @RequestBody(required = false) List<Flight> flights) {
        
        long startTime = System.currentTimeMillis();
        Map<String, Object> response = new HashMap<>();
        
        try {
            iata = iata.toUpperCase();
            List<Flight> result = flightService.processDepartures(iata, flights);
            long duration = System.currentTimeMillis() - startTime;
            
            response.put("success", true);
            response.put("iata", iata);
            response.put("type", "departures");
            response.put("count", result.size());
            response.put("data", result);
            response.put("responseTime", duration + "ms");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * API xóa cache cho một IATA
     */
    @DeleteMapping("/cache/{iata}")
    public ResponseEntity<Map<String, Object>> clearCache(@PathVariable String iata) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            iata = iata.toUpperCase();
            cacheService.clearCache(iata);
            
            response.put("success", true);
            response.put("message", "Cache cleared for IATA: " + iata);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * API lấy thống kê cache cho một IATA
     * Hữu ích để debug và monitor
     */
    @GetMapping("/stats/{iata}")
    public ResponseEntity<Map<String, Object>> getCacheStats(@PathVariable String iata) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            iata = iata.toUpperCase();
            
            // Arrivals stats
            Map<String, Object> arrivalsStats = new HashMap<>();
            arrivalsStats.put("counter", cacheService.getCounter(iata, "arrivals"));
            arrivalsStats.put("isNegativeCached", cacheService.isNegativeCached(iata, "arrivals"));
            arrivalsStats.put("logicalTtlMinutes", cacheService.determineLogicalTtl(iata, "arrivals"));
            
            FlightCacheServiceV2.CacheResult arrivalsCache = cacheService.getFlightsFromCache(iata, "arrivals");
            arrivalsStats.put("cacheHit", arrivalsCache.isCacheHit());
            arrivalsStats.put("cachedFlightsCount", arrivalsCache.isEmpty() ? 0 : arrivalsCache.getFlights().size());
            arrivalsStats.put("logicallyExpired", arrivalsCache.isLogicallyExpired());
            
            // Departures stats
            Map<String, Object> departuresStats = new HashMap<>();
            departuresStats.put("counter", cacheService.getCounter(iata, "departures"));
            departuresStats.put("isNegativeCached", cacheService.isNegativeCached(iata, "departures"));
            departuresStats.put("logicalTtlMinutes", cacheService.determineLogicalTtl(iata, "departures"));
            
            FlightCacheServiceV2.CacheResult departuresCache = cacheService.getFlightsFromCache(iata, "departures");
            departuresStats.put("cacheHit", departuresCache.isCacheHit());
            departuresStats.put("cachedFlightsCount", departuresCache.isEmpty() ? 0 : departuresCache.getFlights().size());
            departuresStats.put("logicallyExpired", departuresCache.isLogicallyExpired());
            
            response.put("success", true);
            response.put("iata", iata);
            response.put("arrivals", arrivalsStats);
            response.put("departures", departuresStats);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
