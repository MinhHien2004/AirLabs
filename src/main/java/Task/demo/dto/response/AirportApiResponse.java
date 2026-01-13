package Task.demo.dto.response;

import Task.demo.entity.Airport;
import lombok.Data;
import java.util.List;

@Data
public class AirportApiResponse {
    private List<Airport> response;
    private Object request;
}
