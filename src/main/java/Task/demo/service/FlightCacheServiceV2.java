package Task.demo.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import Task.demo.dto.FlightCacheEntry;
import Task.demo.entity.Flight;

/**
 * Service quản lý cache cho dữ liệu chuyến bay
 * Triển khai các chiến lược theo RedisCacheWorkFlow.md:
 * - Logical Expiration (Hết hạn ảo)
 * - Frequency-Based Caching (Dựa trên tần suất)
 * - Negative Caching (Cache kết quả rỗng)
 * - Hash-based Duplicate Detection (So sánh hash)
 */
@Service
public class FlightCacheServiceV2 {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;

    // Key prefixes theo thiết kế
    private static final String DATA_PREFIX = "flights:data:";        // Hash lưu dữ liệu
    private static final String COUNTER_PREFIX = "flights:counter:";   // Counter đếm tần suất (theo IATA chung)
    private static final String NEGATIVE_PREFIX = "flights:empty:";    // Negative cache marker
    
    // TTL configurations (theo thiết kế)
    private static final long PHYSICAL_TTL_MINUTES = 60;    // TTL vật lý trên Redis
    private static final long LOGICAL_TTL_HOT_MINUTES = 30; // TTL logic cho hot data (count >= 3)
    private static final long LOGICAL_TTL_COLD_MINUTES = 5; // TTL logic cho cold data (count < 3)
    private static final long COUNTER_TTL_MINUTES = 30;     // TTL cho counter
    private static final long NEGATIVE_TTL_MINUTES = 5;     // TTL cho negative cache
    
    // Threshold cho hot data - chỉ cache khi IATA được gọi >= 3 lần
    private static final int HOT_DATA_THRESHOLD = 3;

    // ===========================================
    // 1. NEGATIVE CACHE OPERATIONS
    // ===========================================
    
    /**
     * Kiểm tra xem IATA code có trong negative cache không
     * Nếu có nghĩa là trước đó đã gọi API và không có dữ liệu
     */
    public boolean isNegativeCached(String iata, String type) {
        try {
            String key = NEGATIVE_PREFIX + type + ":" + iata;
            Boolean hasKey = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(hasKey);
        } catch (Exception e) {
            System.err.println("❌ Redis negative cache check error: " + e.getMessage());
            System.err.println("   Error class: " + e.getClass().getName());
            if (e.getCause() != null) {
                System.err.println("   Root cause: " + e.getCause().getMessage());
                System.err.println("   Cause class: " + e.getCause().getClass().getName());
            }
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Đánh dấu IATA code vào negative cache
     * (API trả về rỗng hoặc lỗi)
     */
    public void setNegativeCache(String iata, String type) {
        try {
            String key = NEGATIVE_PREFIX + type + ":" + iata;
            redisTemplate.opsForValue().set(key, "EMPTY", NEGATIVE_TTL_MINUTES, TimeUnit.MINUTES);
            System.out.println("Set negative cache for " + type + ": " + iata);
        } catch (Exception e) {
            System.err.println("Redis negative cache set error: " + e.getMessage());
        }
    }
    
    /**
     * Xóa negative cache khi có dữ liệu mới
     */
    public void clearNegativeCache(String iata, String type) {
        try {
            String key = NEGATIVE_PREFIX + type + ":" + iata;
            redisTemplate.delete(key);
        } catch (Exception e) {
            System.err.println("Redis negative cache clear error: " + e.getMessage());
        }
    }

    // ===========================================
    // 2. COUNTER OPERATIONS (Frequency-Based)
    // Counter đếm theo IATA CHUNG, không phân biệt arrivals/departures
    // Vì 1 request user thường gọi cả arr_iata + dep_iata
    // ===========================================
    
    /**
     * Tăng counter cho IATA code và trả về giá trị mới
     * Counter đếm chung cho cả arrivals và departures
     */
    public long incrementCounter(String iata, String type) {
        try {
            // Dùng key chung cho IATA, không phân biệt type
            String key = COUNTER_PREFIX + iata;
            Long count = redisTemplate.opsForValue().increment(key);
            
            // Set TTL nếu là counter mới
            if (count != null && count == 1) {
                redisTemplate.expire(key, Duration.ofMinutes(COUNTER_TTL_MINUTES));
            }
            
            System.out.println("📊 Counter for IATA " + iata + ": " + count + 
                    " (threshold: " + HOT_DATA_THRESHOLD + ", from: " + type + ")");
            
            return count != null ? count : 0;
        } catch (Exception e) {
            System.err.println("Redis counter increment error: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Lấy giá trị counter hiện tại (chung cho cả arrivals/departures)
     */
    public long getCounter(String iata, String type) {
        try {
            // Dùng key chung cho IATA
            String key = COUNTER_PREFIX + iata;
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                return Long.parseLong(value.toString());
            }
            return 0;
        } catch (Exception e) {
            System.err.println("❌ Redis counter get error: " + e.getMessage());
            System.err.println("   Error class: " + e.getClass().getName());
            if (e.getCause() != null) {
                System.err.println("   Root cause: " + e.getCause().getMessage());
                System.err.println("   Cause class: " + e.getCause().getClass().getName());
            }
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * Kiểm tra xem IATA có đủ điều kiện cache với TTL dài (30 phút) không
     * Điều kiện: counter >= 3
     */
    public boolean isHotData(String iata) {
        long count = getCounter(iata, null);
        return count >= HOT_DATA_THRESHOLD;
    }
    
    /**
     * Xác định TTL logic dựa trên counter (Frequency-Based)
     * - Counter >= 3: Hot data -> 30 phút
     * - Counter < 3: Cold data -> 5 phút
     */
    public long determineLogicalTtl(String iata, String type) {
        long count = getCounter(iata, type);
        if (count >= HOT_DATA_THRESHOLD) {
            System.out.println("🔥 IATA " + iata + " is HOT DATA (count=" + count + " >= " + HOT_DATA_THRESHOLD + "), using 30 min TTL");
            return LOGICAL_TTL_HOT_MINUTES;
        } else {
            System.out.println("❄️ IATA " + iata + " is COLD DATA (count=" + count + " < " + HOT_DATA_THRESHOLD + "), using 5 min TTL");
            return LOGICAL_TTL_COLD_MINUTES;
        }
    }

    // ===========================================
    // 3. DATA CACHE OPERATIONS (Hash-based)
    // ===========================================
    
    /**
     * Lưu danh sách flights vào Redis Hash
     * Key format: flights:data:{type}:{iata}
     * Field: compositeKey (dep_iata_flight_iata_dep_time)
     * Value: FlightCacheEntry (JSON)
     */
    public void cacheFlights(String iata, String type, List<Flight> flights, long logicalTtlMinutes) {
        try {
            String key = DATA_PREFIX + type + ":" + iata;
            HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();
            
            Map<String, String> entries = new HashMap<>();
            for (Flight flight : flights) {
                String contentHash = calculateHash(flight);
                FlightCacheEntry entry = new FlightCacheEntry(flight, contentHash, logicalTtlMinutes);
                String compositeKey = entry.getCompositeKey();
                String json = objectMapper.writeValueAsString(entry);
                entries.put(compositeKey, json);
            }
            
            // Xóa key cũ trước khi ghi mới (để đảm bảo tính nhất quán)
            redisTemplate.delete(key);
            
            // Sử dụng pipeline để ghi nhiều entry cùng lúc (tối ưu hiệu năng)
            if (!entries.isEmpty()) {
                hashOps.putAll(key, entries);
                redisTemplate.expire(key, Duration.ofMinutes(PHYSICAL_TTL_MINUTES));
            }
            
            // Xóa negative cache nếu có dữ liệu
            clearNegativeCache(iata, type);
            
            System.out.println("✅ Cached " + flights.size() + " flights for " + type + ":" + iata + 
                    " with logical TTL " + logicalTtlMinutes + " min");
                    
        } catch (Exception e) {
            System.err.println("❌ Redis cache flights error: " + e.getMessage());
            System.err.println("   Error class: " + e.getClass().getName());
            if (e.getCause() != null) {
                System.err.println("   Root cause: " + e.getCause().getMessage());
                System.err.println("   Cause class: " + e.getCause().getClass().getName());
            }
            e.printStackTrace();
        }
    }
    
    /**
     * Lấy tất cả flights từ cache
     * @return CacheResult chứa danh sách flights và trạng thái expired
     */
    public CacheResult getFlightsFromCache(String iata, String type) {
        try {
            String key = DATA_PREFIX + type + ":" + iata;
            HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();
            
            Map<String, String> entries = hashOps.entries(key);
            if (entries == null || entries.isEmpty()) {
                return new CacheResult(null, false, false);
            }
            
            List<Flight> flights = new ArrayList<>();
            boolean anyLogicallyExpired = false;
            
            for (String json : entries.values()) {
                try {
                    FlightCacheEntry entry = objectMapper.readValue(json, FlightCacheEntry.class);
                    flights.add(entry.getFlight());
                    
                    if (entry.isLogicallyExpired()) {
                        anyLogicallyExpired = true;
                    }
                } catch (JsonProcessingException e) {
                    System.err.println("Error parsing cache entry: " + e.getMessage());
                }
            }
            
            if (flights.isEmpty()) {
                return new CacheResult(null, false, false);
            }
            
            return new CacheResult(flights, true, anyLogicallyExpired);
            
        } catch (Exception e) {
            System.err.println("❌ Redis get flights error: " + e.getMessage());
            System.err.println("   Error class: " + e.getClass().getName());
            if (e.getCause() != null) {
                System.err.println("   Root cause: " + e.getCause().getMessage());
                System.err.println("   Cause class: " + e.getCause().getClass().getName());
            }
            e.printStackTrace();
            return new CacheResult(null, false, false);
        }
    }
    
    /**
     * Lấy cache entry theo composite key
     */
    public FlightCacheEntry getCacheEntry(String iata, String type, String compositeKey) {
        try {
            String key = DATA_PREFIX + type + ":" + iata;
            HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();
            
            String json = hashOps.get(key, compositeKey);
            if (json != null) {
                return objectMapper.readValue(json, FlightCacheEntry.class);
            }
            return null;
        } catch (Exception e) {
            System.err.println("Redis get cache entry error: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Cập nhật một flight entry trong cache
     */
    public void updateCacheEntry(String iata, String type, Flight flight, long logicalTtlMinutes) {
        try {
            String key = DATA_PREFIX + type + ":" + iata;
            HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();
            
            String contentHash = calculateHash(flight);
            FlightCacheEntry entry = new FlightCacheEntry(flight, contentHash, logicalTtlMinutes);
            String compositeKey = entry.getCompositeKey();
            String json = objectMapper.writeValueAsString(entry);
            
            hashOps.put(key, compositeKey, json);
            
        } catch (Exception e) {
            System.err.println("Redis update cache entry error: " + e.getMessage());
        }
    }
    
    /**
     * Xóa cache cho một IATA code
     */
    public void clearCache(String iata) {
        try {
            redisTemplate.delete(DATA_PREFIX + "arrivals:" + iata);
            redisTemplate.delete(DATA_PREFIX + "departures:" + iata);
            // Counter dùng key chung cho IATA
            redisTemplate.delete(COUNTER_PREFIX + iata);
            clearNegativeCache(iata, "arrivals");
            clearNegativeCache(iata, "departures");
            System.out.println("Cleared all cache for IATA: " + iata);
        } catch (Exception e) {
            System.err.println("Redis delete error: " + e.getMessage());
        }
    }

    // ===========================================
    // 4. HASH COMPARISON (Duplicate Detection)
    // ===========================================
    
    /**
     * Tính SHA-256 hash cho Flight object
     * Dùng để so sánh xem dữ liệu có thay đổi không
     */
    public String calculateHash(Flight flight) {
        try {
            // Tạo string từ tất cả các trường quan trọng của flight
            StringBuilder sb = new StringBuilder();
            sb.append(flight.getAirlineIata()).append("|");
            sb.append(flight.getAirlineIcao()).append("|");
            sb.append(flight.getFlightIata()).append("|");
            sb.append(flight.getFlightIcao()).append("|");
            sb.append(flight.getFlightNumber()).append("|");
            sb.append(flight.getDepIata()).append("|");
            sb.append(flight.getDepIcao()).append("|");
            sb.append(flight.getDepTerminal()).append("|");
            sb.append(flight.getDepGate()).append("|");
            sb.append(flight.getDepTime()).append("|");
            sb.append(flight.getDepTimeUtc()).append("|");
            sb.append(flight.getDepTimeTs()).append("|");
            sb.append(flight.getArrIata()).append("|");
            sb.append(flight.getArrIcao()).append("|");
            sb.append(flight.getArrTerminal()).append("|");
            sb.append(flight.getArrGate()).append("|");
            sb.append(flight.getArrBaggage()).append("|");
            sb.append(flight.getArrTime()).append("|");
            sb.append(flight.getArrTimeUtc()).append("|");
            sb.append(flight.getArrTimeTs()).append("|");
            sb.append(flight.getCsAirlineIata()).append("|");
            sb.append(flight.getCsFlightNumber()).append("|");
            sb.append(flight.getCsFlightIata()).append("|");
            sb.append(flight.getStatus()).append("|");
            sb.append(flight.getDuration()).append("|");
            sb.append(flight.getDelayed()).append("|");
            sb.append(flight.getDepDelayed()).append("|");
            sb.append(flight.getArrDelayed()).append("|");
            sb.append(flight.getAircraftIcao());
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            System.err.println("SHA-256 not available: " + e.getMessage());
            return String.valueOf(flight.hashCode());
        }
    }
    
    /**
     * So sánh hash của flight mới với flight trong cache
     * @return true nếu có thay đổi (hash khác nhau), false nếu giống nhau
     */
    public boolean hasChanged(String iata, String type, Flight newFlight) {
        String compositeKey = FlightCacheEntry.generateCompositeKey(newFlight);
        FlightCacheEntry oldEntry = getCacheEntry(iata, type, compositeKey);
        
        if (oldEntry == null) {
            // Không có trong cache = flight mới
            return true;
        }
        
        String newHash = calculateHash(newFlight);
        String oldHash = oldEntry.getContentHash();
        
        boolean changed = !newHash.equals(oldHash);
        if (changed) {
            System.out.println("Flight " + newFlight.getFlightIata() + " has changed");
        }
        return changed;
    }

    // ===========================================
    // 5. LEGACY SUPPORT (Backward compatibility)
    // ===========================================
    
    /**
     * [LEGACY] Lưu arrivals vào cache - giữ lại cho backward compatibility
     */
    public void cacheArrivals(String iata, List<Flight> flights) {
        long logicalTtl = determineLogicalTtl(iata, "arrivals");
        cacheFlights(iata, "arrivals", flights, logicalTtl);
    }

    /**
     * [LEGACY] Lưu departures vào cache - giữ lại cho backward compatibility
     */
    public void cacheDepartures(String iata, List<Flight> flights) {
        long logicalTtl = determineLogicalTtl(iata, "departures");
        cacheFlights(iata, "departures", flights, logicalTtl);
    }

    /**
     * [LEGACY] Lấy arrivals từ cache
     */
    public List<Flight> getArrivalsFromCache(String iata) {
        CacheResult result = getFlightsFromCache(iata, "arrivals");
        return result.getFlights();
    }

    /**
     * [LEGACY] Lấy departures từ cache
     */
    public List<Flight> getDeparturesFromCache(String iata) {
        CacheResult result = getFlightsFromCache(iata, "departures");
        return result.getFlights();
    }

    // ===========================================
    // 6. HELPER CLASSES
    // ===========================================
    
    /**
     * Class chứa kết quả từ cache
     */
    public static class CacheResult {
        private final List<Flight> flights;
        private final boolean cacheHit;
        private final boolean logicallyExpired;
        
        public CacheResult(List<Flight> flights, boolean cacheHit, boolean logicallyExpired) {
            this.flights = flights;
            this.cacheHit = cacheHit;
            this.logicallyExpired = logicallyExpired;
        }
        
        public List<Flight> getFlights() {
            return flights;
        }
        
        public boolean isCacheHit() {
            return cacheHit;
        }
        
        public boolean isLogicallyExpired() {
            return logicallyExpired;
        }
        
        public boolean isEmpty() {
            return flights == null || flights.isEmpty();
        }
    }
}
