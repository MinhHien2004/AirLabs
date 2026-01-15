package Task.demo.service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import Task.demo.Repository.FlightRepository;
import Task.demo.config.AirLabsConfig;
import Task.demo.dto.response.FlightDisplayDTO;
import Task.demo.entity.Flight;

/**
 * Service for handling flight schedules
 * Responsible for fetching, caching, and processing flight schedule data
 */
@Service
public class ScheduleService {
    
    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private FlightCacheService cacheService;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private AirLabsConfig airLabsConfig;

    /**
     * Get arrivals for an IATA code
     * Flow: Cache -> Database -> External API
     */
    public List<FlightDisplayDTO> getArrivals(String iata) {
        System.out.println("[ARRIVALS] Fetching for IATA: " + iata);
        
        // 1. Check cache first
        List<Flight> cachedFlights = cacheService.getArrivalsFromCache(iata);
        if (cachedFlights != null && !cachedFlights.isEmpty()) {
            System.out.println("[ARRIVALS] Found in cache: " + cachedFlights.size() + " flights");
            return convertToDisplayDTOs(cachedFlights);
        }
        System.out.println("[ARRIVALS] Cache miss");
        
        // 2. Check database
        List<Flight> dbFlights = flightRepository.findByArrIata(iata);
        if (dbFlights != null && !dbFlights.isEmpty()) {
            System.out.println("[ARRIVALS] Found in database: " + dbFlights.size() + " flights");
            cacheService.cacheArrivals(iata, dbFlights);
            return convertToDisplayDTOs(dbFlights);
        }
        System.out.println("[ARRIVALS] Database miss, calling external API...");
        
        // 3. Fetch from external API
        List<Flight> apiFlights = fetchArrivalsFromAPI(iata);
        System.out.println("[ARRIVALS] API returned: " + (apiFlights != null ? apiFlights.size() : "null") + " flights");
        
        if (apiFlights != null && !apiFlights.isEmpty()) {
            List<Flight> savedFlights = saveFlights(apiFlights);
            cacheService.cacheArrivals(iata, savedFlights);
            return convertToDisplayDTOs(savedFlights);
        }
        
        System.out.println("[ARRIVALS] No data available");
        return new ArrayList<>();
    }

    /**
     * Get departures for an IATA code
     * Flow: Cache -> Database -> External API
     */
    public List<FlightDisplayDTO> getDepartures(String iata) {
        System.out.println("[DEPARTURES] Fetching for IATA: " + iata);
        
        // 1. Check cache first
        List<Flight> cachedFlights = cacheService.getDeparturesFromCache(iata);
        if (cachedFlights != null && !cachedFlights.isEmpty()) {
            System.out.println("[DEPARTURES] Found in cache: " + cachedFlights.size() + " flights");
            return convertToDisplayDTOs(cachedFlights);
        }
        System.out.println("[DEPARTURES] Cache miss");
        
        // 2. Check database
        List<Flight> dbFlights = flightRepository.findByDepIata(iata);
        if (dbFlights != null && !dbFlights.isEmpty()) {
            System.out.println("[DEPARTURES] Found in database: " + dbFlights.size() + " flights");
            cacheService.cacheDepartures(iata, dbFlights);
            return convertToDisplayDTOs(dbFlights);
        }
        System.out.println("[DEPARTURES] Database miss, calling external API...");
        
        // 3. Fetch from external API
        List<Flight> apiFlights = fetchDeparturesFromAPI(iata);
        System.out.println("[DEPARTURES] API returned: " + (apiFlights != null ? apiFlights.size() : "null") + " flights");
        
        if (apiFlights != null && !apiFlights.isEmpty()) {
            List<Flight> savedFlights = saveFlights(apiFlights);
            cacheService.cacheDepartures(iata, savedFlights);
            return convertToDisplayDTOs(savedFlights);
        }
        
        System.out.println("[DEPARTURES] No data available");
        return new ArrayList<>();
    }

    /**
     * Fetch arrivals from external AirLabs API
     */
    @SuppressWarnings("unchecked")
    private List<Flight> fetchArrivalsFromAPI(String iata) {
        try {
            String url = airLabsConfig.getBaseUrl() + "/schedules?arr_iata=" + iata + "&api_key=" + airLabsConfig.getApiKey();
            System.out.println("[API] Calling AirLabs API: " + url.replace(airLabsConfig.getApiKey(), "***"));
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            System.out.println("[API] Response received: " + (response != null ? response.keySet() : "null"));
            
            if (response != null && response.containsKey("response")) {
                List<Map<String, Object>> apiFlights = (List<Map<String, Object>>) response.get("response");
                System.out.println("[API] Parsed flights: " + (apiFlights != null ? apiFlights.size() : "null"));
                
                List<Flight> flights = new ArrayList<>();
                for (Map<String, Object> apiData : apiFlights) {
                    flights.add(mapToFlight(apiData));
                }
                
                return flights;
            }
        } catch (Exception e) {
            System.err.println("[API] Error fetching arrivals from API: " + e.getMessage());
            e.printStackTrace();
        }
        return new ArrayList<>();
    }
    
    /**
     * Fetch departures from external AirLabs API
     */
    @SuppressWarnings("unchecked")
    private List<Flight> fetchDeparturesFromAPI(String iata) {
        try {
            String url = airLabsConfig.getBaseUrl() + "/schedules?dep_iata=" + iata + "&api_key=" + airLabsConfig.getApiKey();
            System.out.println("[API] Calling AirLabs API: " + url.replace(airLabsConfig.getApiKey(), "***"));
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            System.out.println("[API] Response received: " + (response != null ? response.keySet() : "null"));
            
            if (response != null && response.containsKey("response")) {
                List<Map<String, Object>> apiFlights = (List<Map<String, Object>>) response.get("response");
                System.out.println("[API] Parsed flights: " + (apiFlights != null ? apiFlights.size() : "null"));
                
                List<Flight> flights = new ArrayList<>();
                for (Map<String, Object> apiData : apiFlights) {
                    flights.add(mapToFlight(apiData));
                }
                
                return flights;
            }
        } catch (Exception e) {
            System.err.println("[API] Error fetching departures from API: " + e.getMessage());
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    /**
     * Save list of flights to database, handling duplicates
     */
    private List<Flight> saveFlights(List<Flight> flights) {
        List<Flight> savedFlights = new ArrayList<>();
        
        for (Flight flight : flights) {
            try {
                Flight existingFlight = flightRepository.findByFlightIataAndDepTime(
                    flight.getFlightIata(), 
                    flight.getDepTime()
                );
                
                if (existingFlight != null) {
                    flight.setId(existingFlight.getId());
                }
                
                Flight savedFlight = flightRepository.save(flight);
                savedFlights.add(savedFlight);
            } catch (Exception e) {
                System.err.println("Error saving flight: " + flight.getFlightIata() + " - " + e.getMessage());
            }
        }
        
        return savedFlights;
    }

    /**
     * Convert Flight entities to FlightDisplayDTOs with calculated delay times
     */
    private List<FlightDisplayDTO> convertToDisplayDTOs(List<Flight> flights) {
        List<FlightDisplayDTO> dtos = new ArrayList<>();
        
        for (Flight flight : flights) {
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
            
            // Calculate scheduled and actual times for departure
            if (flight.getDepTime() != null) {
                String scheduledDepTime = extractTime(flight.getDepTime());
                dto.setScheduledDepTime(scheduledDepTime);
                
                if (flight.getDepDelayed() != null && flight.getDepDelayed() > 0) {
                    dto.setActualDepTime(calculateDelayedTime(scheduledDepTime, flight.getDepDelayed()));
                } else {
                    dto.setActualDepTime(scheduledDepTime);
                }
            }
            
            // Calculate scheduled and actual times for arrival
            if (flight.getArrTime() != null) {
                String scheduledArrTime = extractTime(flight.getArrTime());
                dto.setScheduledArrTime(scheduledArrTime);
                
                if (flight.getArrDelayed() != null && flight.getArrDelayed() > 0) {
                    dto.setActualArrTime(calculateDelayedTime(scheduledArrTime, flight.getArrDelayed()));
                } else {
                    dto.setActualArrTime(scheduledArrTime);
                }
            }
            
            dtos.add(dto);
        }
        
        return dtos;
    }

    /**
     * Extract time from datetime string (format: "2024-01-01 12:30")
     * Returns time in HH:mm format
     */
    private String extractTime(String datetime) {
        if (datetime == null || !datetime.contains(" ")) {
            return "N/A";
        }
        String[] parts = datetime.split(" ");
        if (parts.length < 2) {
            return "N/A";
        }
        String time = parts[1];
        return time.length() >= 5 ? time.substring(0, 5) : time;
    }

    /**
     * Calculate delayed time by adding delay minutes to scheduled time
     */
    private String calculateDelayedTime(String scheduledTime, int delayMinutes) {
        try {
            if (scheduledTime == null || scheduledTime.equals("N/A")) {
                return "N/A";
            }
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            LocalTime time = LocalTime.parse(scheduledTime, formatter);
            LocalTime delayedTime = time.plusMinutes(delayMinutes);
            
            return delayedTime.format(formatter);
        } catch (Exception e) {
            System.err.println("Error calculating delayed time: " + e.getMessage());
            return scheduledTime;
        }
    }

    /**
     * Map API response data to Flight entity
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
}
