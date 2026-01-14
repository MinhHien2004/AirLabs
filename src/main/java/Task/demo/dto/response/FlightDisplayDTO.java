package Task.demo.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for displaying flight information with calculated times
 * Used by ScheduleService to return processed flight data to frontend
 */
public class FlightDisplayDTO {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("airline_iata")
    private String airlineIata;

    @JsonProperty("flight_iata")
    private String flightIata;

    @JsonProperty("flight_number")
    private String flightNumber;

    @JsonProperty("dep_iata")
    private String depIata;

    @JsonProperty("arr_iata")
    private String arrIata;

    @JsonProperty("dep_time")
    private String depTime;

    @JsonProperty("arr_time")
    private String arrTime;

    @JsonProperty("status")
    private String status;

    @JsonProperty("dep_delayed")
    private Integer depDelayed;

    @JsonProperty("arr_delayed")
    private Integer arrDelayed;

    // Calculated fields for frontend display
    @JsonProperty("scheduled_dep_time")
    private String scheduledDepTime;

    @JsonProperty("scheduled_arr_time")
    private String scheduledArrTime;

    @JsonProperty("actual_dep_time")
    private String actualDepTime;

    @JsonProperty("actual_arr_time")
    private String actualArrTime;

    // Constructors
    public FlightDisplayDTO() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAirlineIata() {
        return airlineIata;
    }

    public void setAirlineIata(String airlineIata) {
        this.airlineIata = airlineIata;
    }

    public String getFlightIata() {
        return flightIata;
    }

    public void setFlightIata(String flightIata) {
        this.flightIata = flightIata;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public String getDepIata() {
        return depIata;
    }

    public void setDepIata(String depIata) {
        this.depIata = depIata;
    }

    public String getArrIata() {
        return arrIata;
    }

    public void setArrIata(String arrIata) {
        this.arrIata = arrIata;
    }

    public String getDepTime() {
        return depTime;
    }

    public void setDepTime(String depTime) {
        this.depTime = depTime;
    }

    public String getArrTime() {
        return arrTime;
    }

    public void setArrTime(String arrTime) {
        this.arrTime = arrTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getDepDelayed() {
        return depDelayed;
    }

    public void setDepDelayed(Integer depDelayed) {
        this.depDelayed = depDelayed;
    }

    public Integer getArrDelayed() {
        return arrDelayed;
    }

    public void setArrDelayed(Integer arrDelayed) {
        this.arrDelayed = arrDelayed;
    }

    public String getScheduledDepTime() {
        return scheduledDepTime;
    }

    public void setScheduledDepTime(String scheduledDepTime) {
        this.scheduledDepTime = scheduledDepTime;
    }

    public String getScheduledArrTime() {
        return scheduledArrTime;
    }

    public void setScheduledArrTime(String scheduledArrTime) {
        this.scheduledArrTime = scheduledArrTime;
    }

    public String getActualDepTime() {
        return actualDepTime;
    }

    public void setActualDepTime(String actualDepTime) {
        this.actualDepTime = actualDepTime;
    }

    public String getActualArrTime() {
        return actualArrTime;
    }

    public void setActualArrTime(String actualArrTime) {
        this.actualArrTime = actualArrTime;
    }
}
