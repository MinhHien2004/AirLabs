package Task.demo.service;

import Task.demo.entity.Flight;
import Task.demo.dto.response.FlightDisplayDTO;
import Task.demo.Repository.FlightRepository;
import Task.demo.config.AirLabsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * FlightServiceV2 - Service với Smart Caching
 * Cache chỉ được tạo khi IATA được gọi >= 3 lần trong 30 phút
 */
@Service
public class FlightServiceV2 {

    private static final Logger logger = LoggerFactory.getLogger(FlightServiceV2.class);

    @Autowired
    private FlightCacheServiceV2 cacheService;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AirLabsConfig airLabsConfig;

    /**
     * Lấy arrivals với smart caching
     */
    public List<Flight> getArrivals(String iata) {
        return getFlights(iata, "arrivals");
    }

    /**
     * Lấy departures với smart caching
     */
    public List<Flight> getDepartures(String iata) {
        return getFlights(iata, "departures");
    }

    /**
     * Logic chính với smart caching
     * Cache chỉ lưu khi IATA được gọi >= 3 lần
     */
    private List<Flight> getFlights(String iata, String type) {
        long startTime = System.currentTimeMillis();
        String iataUpper = iata.toUpperCase();
        
        // Tăng call count
        int callCount = cacheService.incrementCallCount(iataUpper);
        logger.info("Processing {} for IATA: {} (call #{} in window)", type, iataUpper, callCount);

        // 1. Check negative cache
        if (cacheService.isNegativeCached(iataUpper, type)) {
            logger.info("Negative cache HIT for {}:{}", type, iataUpper);
            return new ArrayList<>();
        }

        // 2. Check data cache
        FlightCacheServiceV2.CacheResult cacheResult = cacheService.getFlightsFromCache(iataUpper, type);
        if (cacheResult.isCacheHit()) {
            long elapsed = System.currentTimeMillis() - startTime;
            logger.info("Cache HIT for {}:{} - {} flights in {}ms", type, iataUpper, 
                cacheResult.getFlights().size(), elapsed);
            return cacheResult.getFlights();
        }

        // 3. Cache miss - fetch from API
        logger.info("Cache MISS for {}:{} - fetching from API", type, iataUpper);
        List<Flight> flights = fetchFromAPI(iataUpper, type);
        
        if (flights == null || flights.isEmpty()) {
            cacheService.setNegativeCache(iataUpper, type);
            return new ArrayList<>();
        }

        // 4. Sync to database
        List<Flight> savedFlights = syncToDatabase(flights);

        // 5. Cache if call count >= 3
        if (cacheService.shouldCache(iataUpper)) {
            long ttl = cacheService.determineLogicalTtl(iataUpper, type);
            cacheService.cacheFlights(iataUpper, type, savedFlights, ttl);
            logger.info("Cached {} {} flights for IATA: {} (call count >= 3)", savedFlights.size(), type, iataUpper);
        } else {
            logger.info("Not caching {} flights for {}:{} (call count < 3)", savedFlights.size(), type, iataUpper);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        logger.info("Completed {}:{} with {} flights in {}ms", type, iataUpper, savedFlights.size(), elapsed);
        
        return savedFlights;
    }

    /**
     * Process arrivals (backward compatible)
     */
    public List<Flight> processArrivals(String iata, List<Flight> flights) {
        if (flights == null || flights.isEmpty()) {
            return getArrivals(iata);
        }
        return syncToDatabase(flights);
    }

    /**
     * Process departures (backward compatible)
     */
    public List<Flight> processDepartures(String iata, List<Flight> flights) {
        if (flights == null || flights.isEmpty()) {
            return getDepartures(iata);
        }
        return syncToDatabase(flights);
    }

    /**
     * Fetch from AirLabs API
     */
    @SuppressWarnings("unchecked")
    private List<Flight> fetchFromAPI(String iata, String type) {
        try {
            String paramName = "arrivals".equals(type) ? "arr_iata" : "dep_iata";
            String url = airLabsConfig.getBaseUrl() + "/schedules?" + paramName + "=" + iata 
                    + "&api_key=" + airLabsConfig.getApiKey();
            
            logger.debug("Calling API: {}", url.replace(airLabsConfig.getApiKey(), "***"));
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && response.containsKey("response")) {
                List<Map<String, Object>> apiFlights = (List<Map<String, Object>>) response.get("response");
                List<Flight> flightList = new ArrayList<>();
                
                for (Map<String, Object> apiData : apiFlights) {
                    Flight flight = mapToFlight(apiData);
                    flightList.add(flight);
                }
                
                logger.info("API returned {} flights for {}:{}", flightList.size(), type, iata);
                return flightList;
            }
        } catch (Exception e) {
            logger.error("Error fetching from API: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     * Sync flights to database with dedup
     */
    private List<Flight> syncToDatabase(List<Flight> flights) {
        List<Flight> savedFlights = new ArrayList<>();
        int inserted = 0, updated = 0, skipped = 0;
        
        for (Flight flight : flights) {
            try {
                Flight existing = flightRepository.findByFlightIataAndDepTime(
                    flight.getFlightIata(), flight.getDepTime());
                
                if (existing == null) {
                    savedFlights.add(flightRepository.save(flight));
                    inserted++;
                } else {
                    flight.setId(existing.getId());
                    savedFlights.add(flightRepository.save(flight));
                    updated++;
                }
            } catch (Exception e) {
                savedFlights.add(flight);
                skipped++;
            }
        }
        
        logger.info("DB sync: {} inserted, {} updated, {} skipped", inserted, updated, skipped);
        return savedFlights;
    }

    /**
     * Convert API response to Flight entity
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
     * Clear cache for IATA
     */
    public void clearCache(String iata) {
        cacheService.evictCache(iata);
    }
    
    /**
     * Get cache stats
     */
    public String getCacheStats() {
        return cacheService.getCallCountStats();
    }
    
    // =====================================================
    // PUBLIC API METHODS FOR FlightControllerV2
    // =====================================================
    
    /**
     * Get flight schedules as DTO (for controller)
     * With smart caching logic
     */
    public List<FlightDisplayDTO> getFlightSchedules(String depIata) {
        List<Flight> flights = getDepartures(depIata);
        return convertToDTO(flights);
    }
    
    /**
     * Get flight schedules with force cache (bypass call count check)
     */
    public List<FlightDisplayDTO> getFlightSchedulesWithForceCache(String depIata) {
        String iataUpper = depIata.toUpperCase();
        
        // Check cache first
        FlightCacheServiceV2.CacheResult cacheResult = cacheService.getFlightsFromCache(iataUpper, "departures");
        if (cacheResult.isCacheHit()) {
            logger.info("Force cache HIT for departures:{}", iataUpper);
            return convertToDTO(cacheResult.getFlights());
        }
        
        // Fetch from API
        List<Flight> flights = fetchFromAPI(iataUpper, "departures");
        if (flights == null || flights.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Sync to DB
        List<Flight> savedFlights = syncToDatabase(flights);
        
        // Force cache immediately
        cacheService.cacheFlights(iataUpper, "departures", savedFlights, 30);
        logger.info("Force cached {} departures for IATA: {}", savedFlights.size(), iataUpper);
        
        return convertToDTO(savedFlights);
    }
    
    /**
     * Evict cache for specific IATA
     */
    public void evictCache(String iataCode) {
        cacheService.evictCache(iataCode);
    }
    
    /**
     * Evict all cache
     */
    public void evictAllCache() {
        cacheService.evictAllCache();
    }
    
    /**
     * Get call count statistics
     */
    public String getCallCountStats() {
        return cacheService.getCallCountStats();
    }
    
    /**
     * Convert Flight entities to FlightDisplayDTO
     */
    private List<FlightDisplayDTO> convertToDTO(List<Flight> flights) {
        return flights.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Map Flight entity to FlightDisplayDTO
     */
    private FlightDisplayDTO mapToDTO(Flight flight) {
        FlightDisplayDTO dto = new FlightDisplayDTO();
        dto.setId(flight.getId());
        dto.setAirlineIata(flight.getAirlineIata());
        dto.setFlightIata(flight.getFlightIata());
        dto.setFlightNumber(flight.getFlightNumber());
        dto.setDepIata(flight.getDepIata());
        dto.setArrIata(flight.getArrIata());
        dto.setDepTime(flight.getDepTime());
        dto.setArrTime(flight.getArrTime());
        dto.setStatus(flight.getStatus());
        dto.setDepDelayed(flight.getDepDelayed());
        dto.setArrDelayed(flight.getArrDelayed());
        
        // Set calculated times (same as dep/arr time if not delayed)
        dto.setScheduledDepTime(flight.getDepTime());
        dto.setScheduledArrTime(flight.getArrTime());
        dto.setActualDepTime(flight.getDepTime());
        dto.setActualArrTime(flight.getArrTime());
        
        return dto;
    }
}
