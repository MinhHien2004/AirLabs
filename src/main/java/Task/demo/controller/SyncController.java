package Task.demo.controller;

import Task.demo.service.DataSyncService;
import Task.demo.entity.Product;
import Task.demo.entity.Airline;
import Task.demo.entity.Airport;
import Task.demo.entity.Flight;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SyncController {
    private final DataSyncService dataSyncService;

    public SyncController(DataSyncService dataSyncService){
        this.dataSyncService = dataSyncService;
    }

    @GetMapping("/sync/products")
    public ResponseEntity<List<Product>> syncProducts(){
        List<Product> products = dataSyncService.fetchAndSaveProduct();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/sync/airlines")
    public ResponseEntity<List<Airline>> syncAirlines(@RequestParam String iata_code){
        List<Airline> airlines = dataSyncService.fetchAndSaveAirline(iata_code);
        return ResponseEntity.ok(airlines);
    }

    @GetMapping("/sync/airports")
    public ResponseEntity<List<Airport>> syncAirports(){
        List<Airport> airports = dataSyncService.fetchAndSaveAirport();
        return ResponseEntity.ok(airports);
    }

    @GetMapping("/sync/flights")
    public ResponseEntity<List<Flight>> syncFlights(@RequestParam String dep_iata){
        List<Flight> flights = dataSyncService.fetchAndSaveFlights(dep_iata);
        return ResponseEntity.ok(flights);
    }
}
