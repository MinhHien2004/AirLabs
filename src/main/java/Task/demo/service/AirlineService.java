package Task.demo.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import Task.demo.Repository.AirlineRepository;
import Task.demo.entity.Airline;

@Service
public class AirlineService {
    @Autowired
    private AirlineRepository airlineRepository;

    public List<Airline> getAllAirlines(){
        return airlineRepository.findAll();
    }
}
