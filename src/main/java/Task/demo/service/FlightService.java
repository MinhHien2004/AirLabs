package Task.demo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import Task.demo.Repository.FlightRepository;
import Task.demo.config.AirLabsConfig;
import Task.demo.dto.request.FlightCreateRequest;
import Task.demo.dto.request.FlightUpdateRequest;
import Task.demo.entity.Flight;

/**
 * Service xử lý logic nghiệp vụ cho Flight
 * Đã được nâng cấp để delegate sang FlightServiceV2 cho các tính năng caching nâng cao
 */
@Service
public class FlightService {
    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private FlightCacheServiceV2 cacheService;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private AirLabsConfig airLabsConfig;

    public List<Flight> getAllFlights(){
        return flightRepository.findAll();
    }
    
    public Flight getFlightById(Long id){
        return flightRepository.findById(id).orElseThrow(() -> new RuntimeException("Flight not found!"));
    }
    
    public List<Flight> getFlightByDepIata(String depIata){
        return flightRepository.findByDepIata(depIata);
    }

    public List<Flight> getFlightByArrIata(String arrIata){
        return flightRepository.findByArrIata(arrIata);
    }

    public Flight createFlight(FlightCreateRequest request){
        if(flightRepository.existsByFlightIataAndDepTime(request.getFlightIata(), request.getDepTime())){
            throw new RuntimeException("Flight with the same IATA and Departure Time already exists!");
        }
        Flight flight = new Flight();

        flight.setAirlineIata(request.getAirlineIata());
        flight.setAirlineIcao(request.getAirlineIcao());
        flight.setFlightIata(request.getFlightIata());
        flight.setFlightIcao(request.getFlightIcao());
        flight.setFlightNumber(request.getFlightNumber());
        flight.setDepIata(request.getDepIata());
        flight.setDepIcao(request.getDepIcao());
        flight.setDepTerminal(request.getDepTerminal());
        flight.setDepGate(request.getDepGate());
        flight.setDepTime(request.getDepTime());
        flight.setDepTimeUtc(request.getDepTimeUtc());
        flight.setDepTimeTs(request.getDepTimeTs());
        flight.setArrIata(request.getArrIata());
        flight.setArrIcao(request.getArrIcao());
        flight.setArrTerminal(request.getArrTerminal());
        flight.setArrGate(request.getArrGate());
        flight.setArrBaggage(request.getArrBaggage());
        flight.setArrTime(request.getArrTime());
        flight.setArrTimeUtc(request.getArrTimeUtc());
        flight.setArrTimeTs(request.getArrTimeTs());
        flight.setCsAirlineIata(request.getCsAirlineIata());
        flight.setCsFlightNumber(request.getCsFlightNumber());
        flight.setCsFlightIata(request.getCsFlightIata());
        flight.setStatus(request.getStatus());
        flight.setDuration(request.getDuration());
        flight.setDelayed(request.getDelayed());
        flight.setDepDelayed(request.getDepDelayed());
        flight.setArrDelayed(request.getArrDelayed());
        flight.setAircraftIcao(request.getAircraftIcao());

        return flightRepository.save(flight);
    }

    public Flight updateFlight(Long id, FlightUpdateRequest request){
        Flight flight = getFlightById(id);

        flight.setAirlineIata(request.getAirlineIata());
        flight.setAirlineIcao(request.getAirlineIcao());
        flight.setFlightIata(request.getFlightIata());
        flight.setFlightIcao(request.getFlightIcao());
        flight.setFlightNumber(request.getFlightNumber());
        flight.setDepIata(request.getDepIata());
        flight.setDepIcao(request.getDepIcao());
        flight.setDepTerminal(request.getDepTerminal());
        flight.setDepGate(request.getDepGate());
        flight.setDepTime(request.getDepTime());
        flight.setDepTimeUtc(request.getDepTimeUtc());
        flight.setDepTimeTs(request.getDepTimeTs());
        flight.setArrIata(request.getArrIata());
        flight.setArrIcao(request.getArrIcao());
        flight.setArrTerminal(request.getArrTerminal());
        flight.setArrGate(request.getArrGate());
        flight.setArrBaggage(request.getArrBaggage());
        flight.setArrTime(request.getArrTime());
        flight.setArrTimeUtc(request.getArrTimeUtc());
        flight.setArrTimeTs(request.getArrTimeTs());
        flight.setCsAirlineIata(request.getCsAirlineIata());
        flight.setCsFlightNumber(request.getCsFlightNumber());
        flight.setCsFlightIata(request.getCsFlightIata());
        flight.setStatus(request.getStatus());
        flight.setDuration(request.getDuration());
        flight.setDelayed(request.getDelayed());
        flight.setDepDelayed(request.getDepDelayed());
        flight.setArrDelayed(request.getArrDelayed());
        flight.setAircraftIcao(request.getAircraftIcao());

        return flightRepository.save(flight);
    }

    public void deleteFlight(Long id){
        flightRepository.deleteById(id);
    }

    public List<String> getAllDepIata(){
        return flightRepository.findAllDepIata();
    }

    /**
     * Xử lý arrivals với Multi-Layer Smart Caching
     * Triển khai theo RedisCacheWorkFlow.md
     */
    public List<Flight> processArrivals(String iata, List<Flight> newFlights){
        String type = "arrivals";
        
        // Nếu frontend không gửi dữ liệu, sử dụng smart caching
        if (newFlights == null || newFlights.isEmpty()) {
            // STEP 1: Check Negative Cache
            if (cacheService.isNegativeCached(iata, type)) {
                System.out.println("Negative cache HIT for arrivals:" + iata);
                return new ArrayList<>();
            }
            
            // STEP 2: Check Data Cache với Logical Expiration
            FlightCacheServiceV2.CacheResult cacheResult = cacheService.getFlightsFromCache(iata, type);
            
            if (cacheResult.isCacheHit()) {
                if (!cacheResult.isLogicallyExpired()) {
                    // Cache còn fresh - trả về ngay
                    System.out.println("Cache HIT (fresh) for arrivals:" + iata);
                    return cacheResult.getFlights();
                } else {
                    // Cache expired logic - trả về data cũ + async update
                    System.out.println("Cache HIT (stale) for arrivals:" + iata + " - triggering async update");
                    asyncFetchAndSync(iata, type);
                    return cacheResult.getFlights();
                }
            }
            
            // STEP 3: Cache MISS - kiểm tra database
            List<Flight> dbFlights = flightRepository.findByArrIata(iata);
            if (dbFlights != null && !dbFlights.isEmpty()) {
                cacheService.incrementCounter(iata, type);
                long logicalTtl = cacheService.determineLogicalTtl(iata, type);
                cacheService.cacheFlights(iata, type, dbFlights, logicalTtl);
                return dbFlights;
            }
            
            // STEP 4: Database rỗng - fetch từ API
            return fetchAndSyncArrivals(iata);
        }
        
        // Frontend gửi dữ liệu - sync vào DB và cache với dedup
        return syncFlightsToDatabase(iata, type, newFlights);
    }

    /**
     * Xử lý departures với Multi-Layer Smart Caching
     */
    public List<Flight> processDepartures(String iata, List<Flight> newFlights) {
        String type = "departures";
        
        // Nếu frontend không gửi dữ liệu, sử dụng smart caching
        if (newFlights == null || newFlights.isEmpty()) {
            // STEP 1: Check Negative Cache
            if (cacheService.isNegativeCached(iata, type)) {
                System.out.println("Negative cache HIT for departures:" + iata);
                return new ArrayList<>();
            }
            
            // STEP 2: Check Data Cache với Logical Expiration
            FlightCacheServiceV2.CacheResult cacheResult = cacheService.getFlightsFromCache(iata, type);
            
            if (cacheResult.isCacheHit()) {
                if (!cacheResult.isLogicallyExpired()) {
                    System.out.println("Cache HIT (fresh) for departures:" + iata);
                    return cacheResult.getFlights();
                } else {
                    System.out.println("Cache HIT (stale) for departures:" + iata + " - triggering async update");
                    asyncFetchAndSync(iata, type);
                    return cacheResult.getFlights();
                }
            }
            
            // STEP 3: Cache MISS - kiểm tra database
            List<Flight> dbFlights = flightRepository.findByDepIata(iata);
            if (dbFlights != null && !dbFlights.isEmpty()) {
                cacheService.incrementCounter(iata, type);
                long logicalTtl = cacheService.determineLogicalTtl(iata, type);
                cacheService.cacheFlights(iata, type, dbFlights, logicalTtl);
                return dbFlights;
            }
            
            // STEP 4: Database rỗng - fetch từ API
            return fetchAndSyncDepartures(iata);
        }
        
        // Frontend gửi dữ liệu - sync vào DB và cache
        return syncFlightsToDatabase(iata, type, newFlights);
    }
    
    /**
     * Async update khi cache hết hạn logic
     */
    private void asyncFetchAndSync(String iata, String type) {
        // Sử dụng thread mới để không block user
        new Thread(() -> {
            try {
                System.out.println("[ASYNC] Starting background update for " + type + ":" + iata);
                if ("arrivals".equals(type)) {
                    fetchAndSyncArrivals(iata);
                } else {
                    fetchAndSyncDepartures(iata);
                }
                System.out.println("[ASYNC] Completed background update for " + type + ":" + iata);
            } catch (Exception e) {
                System.err.println("[ASYNC] Error: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Fetch arrivals từ API và sync vào DB
     */
    private List<Flight> fetchAndSyncArrivals(String iata) {
        List<Flight> apiFlights = fetchArrivalsFromAPI(iata);
        
        if (apiFlights == null || apiFlights.isEmpty()) {
            cacheService.setNegativeCache(iata, "arrivals");
            return new ArrayList<>();
        }
        
        return syncFlightsToDatabase(iata, "arrivals", apiFlights);
    }
    
    /**
     * Fetch departures từ API và sync vào DB
     */
    private List<Flight> fetchAndSyncDepartures(String iata) {
        List<Flight> apiFlights = fetchDeparturesFromAPI(iata);
        
        if (apiFlights == null || apiFlights.isEmpty()) {
            cacheService.setNegativeCache(iata, "departures");
            return new ArrayList<>();
        }
        
        return syncFlightsToDatabase(iata, "departures", apiFlights);
    }
    
    /**
     * Sync flights vào DB với Hash-based Dedup
     */
    private List<Flight> syncFlightsToDatabase(String iata, String type, List<Flight> flights) {
        cacheService.incrementCounter(iata, type);
        long logicalTtl = cacheService.determineLogicalTtl(iata, type);
        
        List<Flight> processedFlights = new ArrayList<>();
        int inserted = 0, updated = 0, skipped = 0;
        
        for (Flight newFlight : flights) {
            try {
                Flight existingFlight = flightRepository.findByFlightIataAndDepTime(
                    newFlight.getFlightIata(), 
                    newFlight.getDepTime()
                );
                
                if (existingFlight == null) {
                    // INSERT mới
                    Flight savedFlight = flightRepository.save(newFlight);
                    processedFlights.add(savedFlight);
                    inserted++;
                } else {
                    // Kiểm tra có thay đổi không (hash comparison)
                    boolean hasChanged = cacheService.hasChanged(iata, type, newFlight);
                    
                    if (hasChanged) {
                        // UPDATE
                        newFlight.setId(existingFlight.getId());
                        Flight updatedFlight = flightRepository.save(newFlight);
                        processedFlights.add(updatedFlight);
                        updated++;
                    } else {
                        // SKIP - không thay đổi
                        processedFlights.add(existingFlight);
                        skipped++;
                    }
                }
            } catch (Exception e) {
                System.out.println("Skip duplicate flight: " + newFlight.getFlightIata());
            }
        }
        
        System.out.println("DB Sync: " + inserted + " inserted, " + updated + " updated, " + skipped + " skipped");
        
        // Update cache
        cacheService.cacheFlights(iata, type, processedFlights, logicalTtl);
        
        return processedFlights;
    }


    // private boolean isSameFlight(Flight flight1, Flight flight2){
    //     if(flight1.getFlightIata() == null || flight2.getFlightIata() == null){
    //         return false;
    //     }

    //     boolean sameFlightIata = flight1.getFlightIata().equals(flight2.getFlightIata());
    //     boolean sameDepTime = false;

    //     if (flight1.getDepTime() != null && flight2.getDepTime() != null) {
    //         sameDepTime = flight1.getDepTime().equals(flight2.getDepTime());
    //     }
        
    //     return sameFlightIata && sameDepTime;
    // }
    
    // Fetch arrivals từ API public
    @SuppressWarnings("unchecked")
    private List<Flight> fetchArrivalsFromAPI(String iata) {
        try {
            String url = airLabsConfig.getBaseUrl() + "/schedules?arr_iata=" + iata + "&api_key=" + airLabsConfig.getApiKey();
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
            System.err.println("Error fetching arrivals from API: " + e.getMessage());
        }
        return new ArrayList<>();
    }
    
    // Fetch departures từ API public
    @SuppressWarnings("unchecked")
    private List<Flight> fetchDeparturesFromAPI(String iata) {
        try {
            String url = airLabsConfig.getBaseUrl() + "/schedules?dep_iata=" + iata + "&api_key=" + airLabsConfig.getApiKey();
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
            System.err.println("Error fetching departures from API: " + e.getMessage());
        }
        return new ArrayList<>();
    }
    
    // Convert Map từ API sang Flight entity
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

}