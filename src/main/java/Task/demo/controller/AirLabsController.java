package Task.demo.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import Task.demo.config.AirLabsConfig;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/airlabs")
public class AirLabsController {
    private AirLabsConfig airLabsConfig;

    public AirLabsController(AirLabsConfig airLabsConfig) {
        this.airLabsConfig = airLabsConfig;
    }

    @GetMapping("/schedules")
    public String getSchedules(@RequestParam String dep_iata){
        RestTemplate restTemplate = new RestTemplate();
        String url = airLabsConfig.getBaseUrl() + "/schedules?api_key=" + airLabsConfig.getApiKey() + "&dep_iata=" + dep_iata;
        return restTemplate.getForObject(url, String.class);
    }
}
