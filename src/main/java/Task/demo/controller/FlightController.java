package Task.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import Task.demo.dto.request.FlightCreateRequest;
import Task.demo.dto.request.FlightUpdateRequest;
import Task.demo.entity.Flight;
import Task.demo.service.FlightService;

@RestController
@RequestMapping("/api/flights")
public class FlightController {
    @Autowired
    private FlightService flightService;

    @GetMapping
    public List<Flight> getAllFlights(){
        return flightService.getAllFlights();
    }

    @GetMapping("/dep/{dep_iata}")
    public List<Flight> getFlightByDepIata(@PathVariable String dep_iata){
        return flightService.getFlightByDepIata(dep_iata);
    }

    @GetMapping("/{id}")
    public Flight getFlightById(@PathVariable Long id){
        return flightService.getFlightById(id);
    }

    @PostMapping
    public Flight createFlight(@RequestBody FlightCreateRequest request){
        return flightService.createFlight(request);
    }

    @PutMapping("/{id}")
    public Flight updateFlight(@PathVariable Long id, @RequestBody FlightUpdateRequest request){
        return flightService.updateFlight(id, request);
    }

    @DeleteMapping("/{id}")
    public String deleteFlight(@PathVariable Long id){
        flightService.deleteFlight(id);
        return "Flight has been deleted successful";
    }

    @GetMapping("/dep_iata/all")
    public List<String> getAllDepIata(){
        return flightService.getAllDepIata();
    }

    // API để xử lý arrivals từ frontend hoặc tự động fetch từ API
    @PostMapping("/arrivals/{iata}")
    public List<Flight> processArrivals(@PathVariable String iata, @RequestBody(required = false) List<Flight> flights) {
        try {
            System.out.println("Processing arrivals for IATA: " + iata);
            System.out.println("Number of flights received: " + (flights != null ? flights.size() : 0));
            
            // Nếu frontend gửi dữ liệu, in log debug
            if (flights != null && !flights.isEmpty()) {
                Flight firstFlight = flights.get(0);
                System.out.println("First flight data:");
                System.out.println("  airline_iata: " + firstFlight.getAirlineIata());
                System.out.println("  flight_iata: " + firstFlight.getFlightIata());
                System.out.println("  arr_time: " + firstFlight.getArrTime());
                System.out.println("  status: " + firstFlight.getStatus());
            } else {
                System.out.println("No data from frontend, will check DB or fetch from API");
            }
            
            return flightService.processArrivals(iata, flights);
        } catch (Exception e) {
            System.err.println("ERROR in processArrivals: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error processing arrivals: " + e.getMessage(), e);
        }
    }
    
    // API để xử lý departures từ frontend hoặc tự động fetch từ API
    @PostMapping("/departures/{iata}")
    public List<Flight> processDepartures(@PathVariable String iata, @RequestBody(required = false) List<Flight> flights) {
        try {
            System.out.println("Processing departures for IATA: " + iata);
            System.out.println("Number of flights received: " + (flights != null ? flights.size() : 0));
            
            if (flights == null || flights.isEmpty()) {
                System.out.println("No data from frontend, will check DB or fetch from API");
            }
            
            return flightService.processDepartures(iata, flights);
        } catch (Exception e) {
            System.err.println("ERROR in processDepartures: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error processing departures: " + e.getMessage(), e);
        }
    }
}
