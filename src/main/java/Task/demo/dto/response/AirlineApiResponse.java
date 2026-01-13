package Task.demo.dto.response;

import Task.demo.entity.Airline;
import lombok.Data;
import java.util.List;

@Data
public class AirlineApiResponse {
    private List<Airline> response;
    private Object request;
}
