package Task.demo.dto.response;

import java.util.List;

import Task.demo.entity.Flight;
import lombok.Data;

@Data
public class FlightScheduleApiResponse {
    private List<Flight> response;
    private Object request;
}
