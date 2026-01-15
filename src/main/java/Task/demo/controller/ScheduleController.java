package Task.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import Task.demo.dto.response.FlightDisplayDTO;
import Task.demo.service.ScheduleService;

/**
 * Controller for flight schedule endpoints
 * Handles arrivals and departures display with calculated delay times
 */
@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {
    
    @Autowired
    private ScheduleService scheduleService;

    /**
     * Get arrivals for a specific IATA code
     * Flow: Cache -> Database -> External API
     * Returns processed flight data with calculated delay times
     */
    @GetMapping("/arrivals")
    public List<FlightDisplayDTO> getArrivals(@RequestParam String iata) {
        return scheduleService.getArrivals(iata.toUpperCase());
    }
    
    /**
     * Get departures for a specific IATA code
     * Flow: Cache -> Database -> External API
     * Returns processed flight data with calculated delay times
     */
    @GetMapping("/departures")
    public List<FlightDisplayDTO> getDepartures(@RequestParam String iata) {
        return scheduleService.getDepartures(iata.toUpperCase());
    }
}
