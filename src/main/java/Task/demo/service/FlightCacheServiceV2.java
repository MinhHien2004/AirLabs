package Task.demo.service;

import Task.demo.dto.FlightCacheEntry;
import Task.demo.dto.response.FlightDisplayDTO;
import Task.demo.entity.Flight;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class FlightCacheServiceV2 {

    private static final Logger logger = LoggerFactory.getLogger(FlightCacheServiceV2.class);
    
    // Cache duration: 30 phút
    private static final Duration CACHE_DURATION = Duration.ofMinutes(30);
    
    // Số lần gọi tối thiểu để lưu cache
    private static final int MIN_CALL_COUNT_FOR_CACHE = 3;
    
    // Prefix cho cache key
    private static final String CACHE_PREFIX = "flights:v2:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;
    
    // In-memory counter để đếm số lần gọi (reset khi restart)
    // Dùng ConcurrentHashMap để thread-safe
    private final ConcurrentHashMap<String, AtomicInteger> callCountMap = new ConcurrentHashMap<>();
    
    // Thời điểm reset counter (mỗi 30 phút reset 1 lần)
    private volatile long lastResetTime = System.currentTimeMillis();
    private static final long COUNTER_RESET_INTERVAL = Duration.ofMinutes(30).toMillis();

    /**
     * Tăng số lần gọi và kiểm tra có đủ điều kiện cache không
     */
    public int incrementCallCount(String iataCode) {
        resetCountersIfNeeded();
        
        String key = iataCode.toUpperCase();
        AtomicInteger counter = callCountMap.computeIfAbsent(key, k -> new AtomicInteger(0));
        int count = counter.incrementAndGet();
        
        logger.debug("IATA {} call count: {}", iataCode, count);
        return count;
    }
    
    /**
     * Kiểm tra xem IATA có đủ điều kiện để cache không
     */
    public boolean shouldCache(String iataCode) {
        String key = iataCode.toUpperCase();
        AtomicInteger counter = callCountMap.get(key);
        if (counter == null) {
            return false;
        }
        return counter.get() >= MIN_CALL_COUNT_FOR_CACHE;
    }
    
    /**
     * Reset counters sau mỗi 30 phút
     */
    private void resetCountersIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastResetTime > COUNTER_RESET_INTERVAL) {
            synchronized (this) {
                if (now - lastResetTime > COUNTER_RESET_INTERVAL) {
                    callCountMap.clear();
                    lastResetTime = now;
                    logger.info("Call count counters reset");
                }
            }
        }
    }

    /**
     * Lấy cache key cho IATA code
     */
    private String getCacheKey(String iataCode) {
        return CACHE_PREFIX + iataCode.toUpperCase();
    }

    /**
     * Lấy flights từ cache
     */
    public FlightCacheEntry getCachedFlights(String iataCode) {
        try {
            String key = getCacheKey(iataCode);
            String json = redisTemplate.opsForValue().get(key);
            
            if (json == null) {
                logger.debug("Cache MISS for IATA: {}", iataCode);
                return null;
            }
            
            FlightCacheEntry entry = objectMapper.readValue(json, FlightCacheEntry.class);
            
            // Kiểm tra cache có hết hạn chưa
            if (entry.isExpired()) {
                logger.debug("Cache EXPIRED for IATA: {}", iataCode);
                redisTemplate.delete(key);
                return null;
            }
            
            logger.info("Cache HIT for IATA: {} ({} flights, age: {}s)", 
                iataCode, 
                entry.getFlights() != null ? entry.getFlights().size() : 0,
                Duration.between(entry.getCachedAtInstant(), Instant.now()).getSeconds());
            
            return entry;
            
        } catch (JsonProcessingException e) {
            logger.error("Error deserializing cache for IATA {}: {}", iataCode, e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Redis error getting cache for IATA {}: {}", iataCode, e.getMessage());
            return null;
        }
    }

    /**
     * Lưu flights vào cache (chỉ khi đủ điều kiện)
     */
    public void cacheFlights(String iataCode, List<FlightDisplayDTO> flights) {
        // Kiểm tra điều kiện cache
        if (!shouldCache(iataCode)) {
            int currentCount = callCountMap.getOrDefault(iataCode.toUpperCase(), new AtomicInteger(0)).get();
            logger.debug("IATA {} not cached yet (call count: {}/{})", 
                iataCode, currentCount, MIN_CALL_COUNT_FOR_CACHE);
            return;
        }
        
        try {
            String key = getCacheKey(iataCode);
            
            FlightCacheEntry entry = new FlightCacheEntry(
                flights,
                Instant.now(),
                CACHE_DURATION
            );
            
            String json = objectMapper.writeValueAsString(entry);
            redisTemplate.opsForValue().set(key, json, CACHE_DURATION);
            
            logger.info("Cached {} flights for IATA: {} (TTL: {} minutes)", 
                flights.size(), iataCode, CACHE_DURATION.toMinutes());
            
        } catch (JsonProcessingException e) {
            logger.error("Error serializing flights for IATA {}: {}", iataCode, e.getMessage());
        } catch (Exception e) {
            logger.error("Redis error caching flights for IATA {}: {}", iataCode, e.getMessage());
        }
    }
    
    /**
     * Lưu flights vào cache ngay lập tức (bỏ qua điều kiện call count)
     * Dùng cho trường hợp force cache
     */
    public void cacheFlightsImmediately(String iataCode, List<FlightDisplayDTO> flights) {
        try {
            String key = getCacheKey(iataCode);
            
            FlightCacheEntry entry = new FlightCacheEntry(
                flights,
                Instant.now(),
                CACHE_DURATION
            );
            
            String json = objectMapper.writeValueAsString(entry);
            redisTemplate.opsForValue().set(key, json, CACHE_DURATION);
            
            logger.info("Force cached {} flights for IATA: {} (TTL: {} minutes)", 
                flights.size(), iataCode, CACHE_DURATION.toMinutes());
            
        } catch (JsonProcessingException e) {
            logger.error("Error serializing flights for IATA {}: {}", iataCode, e.getMessage());
        } catch (Exception e) {
            logger.error("Redis error caching flights for IATA {}: {}", iataCode, e.getMessage());
        }
    }

    /**
     * Xóa cache cho một IATA code
     */
    public void evictCache(String iataCode) {
        try {
            String key = getCacheKey(iataCode);
            Boolean deleted = redisTemplate.delete(key);
            
            // Reset call count khi xóa cache
            callCountMap.remove(iataCode.toUpperCase());
            
            logger.info("Evicted cache for IATA: {} (existed: {})", iataCode, deleted);
        } catch (Exception e) {
            logger.error("Error evicting cache for IATA {}: {}", iataCode, e.getMessage());
        }
    }

    /**
     * Xóa toàn bộ flight cache
     */
    public void evictAllCache() {
        try {
            var keys = redisTemplate.keys(CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                logger.info("Evicted all flight cache ({} keys)", keys.size());
            }
            
            // Reset tất cả call counts
            callCountMap.clear();
            lastResetTime = System.currentTimeMillis();
            
        } catch (Exception e) {
            logger.error("Error evicting all cache: {}", e.getMessage());
        }
    }

    /**
     * Kiểm tra cache có tồn tại và còn hạn không
     */
    public boolean isCacheValid(String iataCode) {
        FlightCacheEntry entry = getCachedFlights(iataCode);
        return entry != null && !entry.isExpired();
    }
    
    /**
     * Lấy thông tin thống kê call count
     */
    public String getCallCountStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("Call Count Statistics:\n");
        callCountMap.forEach((iata, count) -> {
            sb.append(String.format("  %s: %d calls (cache: %s)\n", 
                iata, count.get(), count.get() >= MIN_CALL_COUNT_FOR_CACHE ? "YES" : "NO"));
        });
        return sb.toString();
    }
    
    // =====================================================
    // BACKWARD COMPATIBILITY METHODS FOR FlightService.java
    // =====================================================
    
    private static final String NEGATIVE_PREFIX = "flights:empty:";
    private static final long NEGATIVE_TTL_MINUTES = 5;
    private static final long PHYSICAL_TTL_MINUTES = 60;
    private static final long LOGICAL_TTL_HOT_MINUTES = 30;
    private static final long LOGICAL_TTL_COLD_MINUTES = 5;
    private static final int HOT_DATA_THRESHOLD = 2;
    
    /**
     * Kiểm tra negative cache (backward compatible)
     */
    public boolean isNegativeCached(String iata, String type) {
        try {
            String key = NEGATIVE_PREFIX + type + ":" + iata;
            Boolean hasKey = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(hasKey);
        } catch (Exception e) {
            logger.error("Redis negative cache check error: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Set negative cache (backward compatible)
     */
    public void setNegativeCache(String iata, String type) {
        try {
            String key = NEGATIVE_PREFIX + type + ":" + iata;
            redisTemplate.opsForValue().set(key, "EMPTY", Duration.ofMinutes(NEGATIVE_TTL_MINUTES));
            logger.info("Set negative cache for {}:{}", type, iata);
        } catch (Exception e) {
            logger.error("Redis negative cache set error: {}", e.getMessage());
        }
    }
    
    /**
     * Increment counter (backward compatible with type)
     */
    public long incrementCounter(String iata, String type) {
        return incrementCallCount(iata + ":" + type);
    }
    
    /**
     * Determine logical TTL based on frequency (backward compatible)
     */
    public long determineLogicalTtl(String iata, String type) {
        String key = (iata + ":" + type).toUpperCase();
        AtomicInteger counter = callCountMap.get(key);
        long count = counter != null ? counter.get() : 0;
        
        if (count >= HOT_DATA_THRESHOLD) {
            return LOGICAL_TTL_HOT_MINUTES;
        }
        return LOGICAL_TTL_COLD_MINUTES;
    }
    
    /**
     * Check if flight has changed (backward compatible)
     */
    public boolean hasChanged(String iata, String type, Flight flight) {
        // Simplified: always return true to update
        // In production, compare hash of flight data
        return true;
    }
    
    /**
     * Cache flights with type and TTL (backward compatible for FlightService)
     */
    public void cacheFlights(String iata, String type, List<Flight> flights, long logicalTtlMinutes) {
        try {
            String key = CACHE_PREFIX + type + ":" + iata;
            
            // Convert Flight entities to simple JSON
            String json = objectMapper.writeValueAsString(flights);
            redisTemplate.opsForValue().set(key, json, Duration.ofMinutes(PHYSICAL_TTL_MINUTES));
            
            logger.info("Cached {} {} flights for IATA: {} (TTL: {} minutes)", 
                flights.size(), type, iata, PHYSICAL_TTL_MINUTES);
            
        } catch (Exception e) {
            logger.error("Error caching {} flights for IATA {}: {}", type, iata, e.getMessage());
        }
    }
    
    /**
     * Get flights from cache with type (backward compatible)
     */
    public CacheResult getFlightsFromCache(String iata, String type) {
        try {
            String key = CACHE_PREFIX + type + ":" + iata;
            String json = redisTemplate.opsForValue().get(key);
            
            if (json == null) {
                return new CacheResult(false, null, false);
            }
            
            List<Flight> flights = objectMapper.readValue(json, 
                new com.fasterxml.jackson.core.type.TypeReference<List<Flight>>() {});
            
            // For simplicity, assume not expired if exists
            return new CacheResult(true, flights, false);
            
        } catch (Exception e) {
            logger.error("Error getting cache for {}:{}: {}", type, iata, e.getMessage());
            return new CacheResult(false, null, false);
        }
    }
    
    // =====================================================
    // METHODS FOR ScheduleService COMPATIBILITY
    // =====================================================
    
    private static final String ARRIVALS_PREFIX = "flights:arrivals:";
    private static final String DEPARTURES_PREFIX = "flights:departures:";
    
    /**
     * Get arrivals from cache (for ScheduleService)
     */
    public List<Flight> getArrivalsFromCache(String iata) {
        try {
            String key = ARRIVALS_PREFIX + iata.toUpperCase();
            String json = redisTemplate.opsForValue().get(key);
            
            if (json == null) {
                logger.debug("Arrivals cache MISS for IATA: {}", iata);
                return null;
            }
            
            List<Flight> flights = objectMapper.readValue(json, 
                new com.fasterxml.jackson.core.type.TypeReference<List<Flight>>() {});
            logger.info("Arrivals cache HIT for IATA: {} ({} flights)", iata, flights.size());
            return flights;
            
        } catch (Exception e) {
            logger.error("Error getting arrivals cache for IATA {}: {}", iata, e.getMessage());
            return null;
        }
    }
    
    /**
     * Cache arrivals (for ScheduleService)
     */
    public void cacheArrivals(String iata, List<Flight> flights) {
        try {
            String key = ARRIVALS_PREFIX + iata.toUpperCase();
            String json = objectMapper.writeValueAsString(flights);
            redisTemplate.opsForValue().set(key, json, CACHE_DURATION);
            logger.info("Cached {} arrivals for IATA: {}", flights.size(), iata);
        } catch (Exception e) {
            logger.error("Error caching arrivals for IATA {}: {}", iata, e.getMessage());
        }
    }
    
    /**
     * Get departures from cache (for ScheduleService)
     */
    public List<Flight> getDeparturesFromCache(String iata) {
        try {
            String key = DEPARTURES_PREFIX + iata.toUpperCase();
            String json = redisTemplate.opsForValue().get(key);
            
            if (json == null) {
                logger.debug("Departures cache MISS for IATA: {}", iata);
                return null;
            }
            
            List<Flight> flights = objectMapper.readValue(json, 
                new com.fasterxml.jackson.core.type.TypeReference<List<Flight>>() {});
            logger.info("Departures cache HIT for IATA: {} ({} flights)", iata, flights.size());
            return flights;
            
        } catch (Exception e) {
            logger.error("Error getting departures cache for IATA {}: {}", iata, e.getMessage());
            return null;
        }
    }
    
    /**
     * Cache departures (for ScheduleService)
     */
    public void cacheDepartures(String iata, List<Flight> flights) {
        try {
            String key = DEPARTURES_PREFIX + iata.toUpperCase();
            String json = objectMapper.writeValueAsString(flights);
            redisTemplate.opsForValue().set(key, json, CACHE_DURATION);
            logger.info("Cached {} departures for IATA: {}", flights.size(), iata);
        } catch (Exception e) {
            logger.error("Error caching departures for IATA {}: {}", iata, e.getMessage());
        }
    }
    
    /**
     * CacheResult class for backward compatibility
     */
    public static class CacheResult {
        private final boolean cacheHit;
        private final List<Flight> flights;
        private final boolean logicallyExpired;
        
        public CacheResult(boolean cacheHit, List<Flight> flights, boolean logicallyExpired) {
            this.cacheHit = cacheHit;
            this.flights = flights;
            this.logicallyExpired = logicallyExpired;
        }
        
        public boolean isCacheHit() { return cacheHit; }
        public List<Flight> getFlights() { return flights; }
        public boolean isLogicallyExpired() { return logicallyExpired; }
    }
}
