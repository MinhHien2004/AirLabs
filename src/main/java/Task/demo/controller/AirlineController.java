package Task.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import Task.demo.entity.Airline;
import Task.demo.service.AirlineService;

@RestController
@RequestMapping("/api/airlines")
public class AirlineController {
    @Autowired
    private AirlineService airlineService;

    @GetMapping
    public List<Airline> getAllAirlines(){
        return airlineService.getAllAirlines();
    }
}
