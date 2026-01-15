package Task.demo.service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import Task.demo.entity.Flight;

@Service
public class FlightCacheService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String ARRIVALS_PREFIX = "arrivals:";
    private static final String DEPARTURES_PREFIX = "departures:";
    private static final int CACHE_EXPIRATION = 30; // 30 minutes

    // Lưu arrivals vào cache
    public void cacheArrivals(String iata, List<Flight> flights) {
        try {
            String key = ARRIVALS_PREFIX + iata;
            redisTemplate.opsForValue().set(key, flights, CACHE_EXPIRATION, TimeUnit.MINUTES);
        } catch (Exception e) {
            System.err.println("Redis cache error (arrivals): " + e.getMessage());
            // Continue without caching
        }
    }

    // Lưu departures vào cache
    public void cacheDepartures(String iata, List<Flight> flights) {
        try {
            String key = DEPARTURES_PREFIX + iata;
            redisTemplate.opsForValue().set(key, flights, CACHE_EXPIRATION, TimeUnit.MINUTES);
        } catch (Exception e) {
            System.err.println("Redis cache error (departures): " + e.getMessage());
            // Continue without caching
        }
    }

    // Lấy arrivals từ cache
    @SuppressWarnings("unchecked")
    public List<Flight> getArrivalsFromCache(String iata) {
        try {
            String key = ARRIVALS_PREFIX + iata;
            return (List<Flight>) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            System.err.println("Redis get error (arrivals): " + e.getMessage());
            return null;
        }
    }

    // Lấy departures từ cache
    @SuppressWarnings("unchecked")
    public List<Flight> getDeparturesFromCache(String iata) {
        try {
            String key = DEPARTURES_PREFIX + iata;
            return (List<Flight>) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            System.err.println("Redis get error (departures): " + e.getMessage());
            return null;
        }
    }

    // Xóa cache khi thay đổi IATA
    public void clearCache(String iata) {
        try {
            redisTemplate.delete(ARRIVALS_PREFIX + iata);
            redisTemplate.delete(DEPARTURES_PREFIX + iata);
        } catch (Exception e) {
            System.err.println("Redis delete error: " + e.getMessage());
        }
    }
}
