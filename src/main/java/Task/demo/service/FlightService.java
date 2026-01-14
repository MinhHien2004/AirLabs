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

@Service
public class FlightService {
    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private FlightCacheService cacheService;
    
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

    public List<Flight> processArrivals(String iata, List<Flight> newFlights){
        // Nếu frontend không gửi dữ liệu, tự động fetch từ API
        if (newFlights == null || newFlights.isEmpty()) {
            // Kiểm tra database trước
            List<Flight> dbFlights = flightRepository.findByArrIata(iata);
            if (dbFlights != null && !dbFlights.isEmpty()) {
                // Có dữ liệu trong DB, trả về luôn
                cacheService.cacheArrivals(iata, dbFlights);
                return dbFlights;
            }
            
            // Database rỗng, fetch từ API public
            newFlights = fetchArrivalsFromAPI(iata);
            if (newFlights == null || newFlights.isEmpty()) {
                return new ArrayList<>();
            }
        }
        
        // 1. Get data from cache
        List<Flight> cachedFlights = cacheService.getArrivalsFromCache(iata);

        // if cache is empty, get data from DB
        if (cachedFlights == null) {
            cachedFlights = new ArrayList<>();
            List<Flight> dbFlights = flightRepository.findByArrIata(iata);
            if(dbFlights != null && !dbFlights.isEmpty()) {
                cachedFlights = dbFlights;
                // Save in cache
                cacheService.cacheArrivals(iata, cachedFlights);
            }
        }

        List<Flight> processedFlights = new ArrayList<>();
        
        for (Flight newFlight : newFlights) {
            try {
                // Check if flight already exists in DB (based on unique constraint)
                Flight existingFlight = flightRepository.findByFlightIataAndDepTime(
                    newFlight.getFlightIata(), 
                    newFlight.getDepTime()
                );
                
                if (existingFlight != null) {
                    // Flight already exists, update it
                    newFlight.setId(existingFlight.getId());
                    Flight updatedFlight = flightRepository.save(newFlight);
                    processedFlights.add(updatedFlight);
                } else {
                    // New flight, save to DB
                    Flight savedFlight = flightRepository.save(newFlight);
                    processedFlights.add(savedFlight);
                }
            } catch (Exception e) {
                // Skip duplicate error (may be from concurrent request)
                System.out.println("Skip duplicate flight: " + newFlight.getFlightIata());
            }
        }

        //4. Update cache
        cacheService.cacheArrivals((iata), processedFlights);

        return processedFlights;

    }

    // Xử lý departures với cache
    public List<Flight> processDepartures(String iata, List<Flight> newFlights) {
        // Nếu frontend không gửi dữ liệu, tự động fetch từ API
        if (newFlights == null || newFlights.isEmpty()) {
            // Kiểm tra database trước
            List<Flight> dbFlights = flightRepository.findByDepIata(iata);
            if (dbFlights != null && !dbFlights.isEmpty()) {
                // Có dữ liệu trong DB, trả về luôn
                cacheService.cacheDepartures(iata, dbFlights);
                return dbFlights;
            }
            
            // Database rỗng, fetch từ API public
            newFlights = fetchDeparturesFromAPI(iata);
            if (newFlights == null || newFlights.isEmpty()) {
                return new ArrayList<>();
            }
        }
        
        // 1. Lấy dữ liệu từ cache
        List<Flight> cachedFlights = cacheService.getDeparturesFromCache(iata);
        
        // Nếu cache rỗng, lấy từ database
        if (cachedFlights == null) {
            cachedFlights = new ArrayList<>();
            List<Flight> dbFlights = flightRepository.findByDepIata(iata);
            if (dbFlights != null && !dbFlights.isEmpty()) {
                cachedFlights = dbFlights;
                cacheService.cacheDepartures(iata, cachedFlights);
            }
        }
        
        // 2. So sánh và cập nhật
        List<Flight> processedFlights = new ArrayList<>();
        
        for (Flight newFlight : newFlights) {
            try {
                // Kiểm tra xem flight đã tồn tại trong DB chưa (dựa vào unique constraint)
                Flight existingFlight = flightRepository.findByFlightIataAndDepTime(
                    newFlight.getFlightIata(), 
                    newFlight.getDepTime()
                );
                
                if (existingFlight != null) {
                    newFlight.setId(existingFlight.getId());
                    Flight updatedFlight = flightRepository.save(newFlight);
                    processedFlights.add(updatedFlight);
                } else {
                    // Flight mới, lưu vào DB
                    Flight savedFlight = flightRepository.save(newFlight);
                    processedFlights.add(savedFlight);
                }
            } catch (Exception e) {
                // Bỏ qua lỗi duplicate (có thể do concurrent request)
                System.out.println("Skip duplicate flight: " + newFlight.getFlightIata());
            }
        }
        
        // 4. Cập nhật cache
        cacheService.cacheDepartures(iata, processedFlights);
        
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