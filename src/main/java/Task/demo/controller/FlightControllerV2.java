package Task.demo.controller;

import Task.demo.dto.response.FlightDisplayDTO;
import Task.demo.service.FlightServiceV2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/flights")
@CrossOrigin(origins = "*")
public class FlightControllerV2 {

    @Autowired
    private FlightServiceV2 flightService;

    /**
     * Lấy danh sách chuyến bay theo IATA code
     * Cache được tạo sau khi IATA được gọi >= 3 lần
     */
    @GetMapping("/schedules")
    public ResponseEntity<List<FlightDisplayDTO>> getFlightSchedules(
            @RequestParam String dep_iata) {
        
        List<FlightDisplayDTO> flights = flightService.getFlightSchedules(dep_iata);
        return ResponseEntity.ok(flights);
    }
    
    /**
     * Lấy danh sách chuyến bay và force cache ngay lập tức
     * Bỏ qua điều kiện call count
     */
    @GetMapping("/schedules/force-cache")
    public ResponseEntity<List<FlightDisplayDTO>> getFlightSchedulesForceCache(
            @RequestParam String dep_iata) {
        
        List<FlightDisplayDTO> flights = flightService.getFlightSchedulesWithForceCache(dep_iata);
        return ResponseEntity.ok(flights);
    }

    /**
     * Xóa cache cho một IATA code cụ thể
     */
    @DeleteMapping("/cache/{iataCode}")
    public ResponseEntity<Map<String, String>> evictCache(@PathVariable String iataCode) {
        flightService.evictCache(iataCode);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Cache evicted for IATA: " + iataCode);
        response.put("status", "success");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Xóa toàn bộ cache
     */
    @DeleteMapping("/cache")
    public ResponseEntity<Map<String, String>> evictAllCache() {
        flightService.evictAllCache();
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "All flight cache evicted");
        response.put("status", "success");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Lấy thống kê call count
     */
    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Object> response = new HashMap<>();
        response.put("callCountStats", flightService.getCallCountStats());
        response.put("message", "Cache is created after 3 calls to the same IATA code");
        response.put("cacheDuration", "30 minutes");
        
        return ResponseEntity.ok(response);
    }
}
