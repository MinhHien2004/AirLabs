package Task.demo.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import Task.demo.Repository.AirportRepository;
import Task.demo.Repository.FlightRepository;
import Task.demo.entity.Airline;
import Task.demo.entity.Airport;
import Task.demo.entity.Flight;
import Task.demo.Repository.AirlineRepository;
import Task.demo.dto.response.*;
import Task.demo.config.*;

@Service
public class DataSyncService {
    private final AirlineRepository airlineRepository;
    private final AirportRepository airportRepository;
    private final FlightRepository flightRepository;
    private final RestTemplate restTemplate;
    private final AirLabsConfig airLabsConfig;

    public DataSyncService( 
                        AirlineRepository airlineRepository, 
                        AirportRepository airportRepository,
                        FlightRepository flightRepository,
                        AirLabsConfig airLabsConfig,
                        RestTemplate restTemplate){
        this.airlineRepository = airlineRepository;
        this.airportRepository = airportRepository;
        this.flightRepository = flightRepository;
        this.airLabsConfig = airLabsConfig;
        this.restTemplate = restTemplate;
    }

    public List<Airline> fetchAndSaveAirline(String iata_code){

        //Database check first
        //If data present in th DB, no call to Public API
        // If data doesn't present , then call Public API and sava data to DB.
        Optional<Airline> existingAirline = airlineRepository.findByIata_code(iata_code);
        if(existingAirline.isPresent()){
            System.out.println("Dữ liệu hãng hàng không đã tồn tại trong DB. Trả về dữ liệu từ DB.");
            return List.of(existingAirline.get());
        }
        String url = airLabsConfig.getBaseUrl() +"/airlines?iata_code=" + iata_code + "&api_key=" + airLabsConfig.getApiKey();

        ResponseEntity<AirlineApiResponse> response = restTemplate.getForEntity(url, AirlineApiResponse.class);
        AirlineApiResponse apiResponse = response.getBody();

        if(apiResponse != null && apiResponse.getResponse() != null){
            List<Airline> airlines = apiResponse.getResponse();

            airlineRepository.saveAll(airlines);

            System.out.println("Đã lưu thành công " + airlines.size() + " hãng hàng không vào DB");
            return airlines;
        }
        return List.of();
    }

    public List<Airport> fetchAndSaveAirport(){
        long existingCount = airportRepository.count();
        if(existingCount > 0){
            System.out.println("Dữ liệu sân bay đã tồn tại trong DB. Trả về dữ liệu từ DB.");
            return airportRepository.findAll();
        }
        
        String url = airLabsConfig.getBaseUrl() + "/airports?api_key=" + airLabsConfig.getApiKey();
        
        ResponseEntity<AirportApiResponse> response = restTemplate.getForEntity(url, AirportApiResponse.class);
        AirportApiResponse apiResponse = response.getBody();    
        if(apiResponse != null && apiResponse.getResponse() != null){
            List<Airport> airports = apiResponse.getResponse();

            airportRepository.saveAll(airports);

            System.out.println("Đã lưu thành công " + airports.size() + " sân bay vào DB");
            return airports;
        }
        return List.of();
    }

    public List<Flight> fetchAndSaveFlights(String dep_iata){
        // Kiểm tra xem đã có flights cho dep_iata này chưa
        List<Flight> existingFlights = flightRepository.findByDepIata(dep_iata);
        if(!existingFlights.isEmpty()){
            System.out.println("Dữ liệu chuyến bay cho " + dep_iata + " đã tồn tại trong DB (" + existingFlights.size() + " flights). Trả về dữ liệu từ DB.");
            return existingFlights;
        }

        // Chưa có dữ liệu cho dep_iata này, gọi API để lấy
        String url = airLabsConfig.getBaseUrl() + "/schedules?dep_iata=" + dep_iata + "&api_key=" + airLabsConfig.getApiKey();
        System.out.println("Gọi API để lấy dữ liệu mới cho " + dep_iata + ": " + url);
        
        ResponseEntity<FlightScheduleApiResponse> response = restTemplate.getForEntity(url, FlightScheduleApiResponse.class);
        FlightScheduleApiResponse apiResponse = response.getBody();
        if(apiResponse != null && apiResponse.getResponse() != null){
            List<Flight> flights = apiResponse.getResponse();

            // Lưu thêm vào DB (append, không xóa dữ liệu cũ)
            flightRepository.saveAll(flights);

            long totalFlightsInDb = flightRepository.count();
            System.out.println("Đã thêm " + flights.size() + " chuyến bay cho " + dep_iata + " vào DB. Tổng số flights trong DB: " + totalFlightsInDb);
            return flights;
        }

        System.out.println("Không có dữ liệu từ API cho " + dep_iata);
        return List.of();
    }
}
