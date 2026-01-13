package Task.demo.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "airline_iata",
        "airline_icao",
        "flight_iata",
        "flight_icao",
        "flight_number",
        "dep_iata",
        "dep_icao",
        "dep_terminal",
        "dep_gate",
        "dep_time",
        "dep_time_utc",
        "arr_iata",
        "arr_icao",
        "arr_terminal",
        "arr_gate",
        "arr_baggage",
        "arr_time",
        "arr_time_utc",
        "cs_airline_iata",
        "cs_flight_number",
        "cs_flight_iata",
        "status",
        "duration",
        "delayed",
        "dep_delayed",
        "arr_delayed",
        "aircraft_icao",
        "arr_time_ts",
        "dep_time_ts"
})
public class FlightUpdateRequest {
    @JsonProperty("airline_iata")
    private String airlineIata;
    
    @JsonProperty("airline_icao")
    private String airlineIcao;
    
    @JsonProperty("flight_iata")
    private String flightIata;
    
    @JsonProperty("flight_icao")
    private String flightIcao;
    
    @JsonProperty("flight_number")
    private String flightNumber;
    
    @JsonProperty("dep_iata")
    private String depIata;
    
    @JsonProperty("dep_icao")
    private String depIcao;
    
    @JsonProperty("dep_terminal")
    private String depTerminal;
    
    @JsonProperty("dep_gate")
    private String depGate;
    
    @JsonProperty("dep_time")
    private String depTime;
    
    @JsonProperty("dep_time_utc")
    private String depTimeUtc;
    
    @JsonProperty("arr_iata")
    private String arrIata;
    
    @JsonProperty("arr_icao")
    private String arrIcao;
    
    @JsonProperty("arr_terminal")
    private String arrTerminal;
    
    @JsonProperty("arr_gate")
    private String arrGate;
    
    @JsonProperty("arr_baggage")
    private String arrBaggage;
    
    @JsonProperty("arr_time")
    private String arrTime;
    
    @JsonProperty("arr_time_utc")
    private String arrTimeUtc;
    
    @JsonProperty("cs_airline_iata")
    private String csAirlineIata;
    
    @JsonProperty("cs_flight_number")
    private String csFlightNumber;
    
    @JsonProperty("cs_flight_iata")
    private String csFlightIata;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("duration")
    private Integer duration;
    
    @JsonProperty("delayed")
    private Integer delayed;
    
    @JsonProperty("dep_delayed")
    private Integer depDelayed;
    
    @JsonProperty("arr_delayed")
    private Integer arrDelayed;
    
    @JsonProperty("aircraft_icao")
    private String aircraftIcao;
    
    @JsonProperty("arr_time_ts")
    private Long arrTimeTs;
    
    @JsonProperty("dep_time_ts")
    private Long depTimeTs;

    // Getters and Setters
    public String getAirlineIata() {
        return airlineIata;
    }
    public void setAirlineIata(String airlineIata) {
        this.airlineIata = airlineIata;
    }
    public String getAirlineIcao() {
        return airlineIcao;
    }
    public void setAirlineIcao(String airlineIcao) {
        this.airlineIcao = airlineIcao;
    }
    public String getFlightIata() {
        return flightIata;
    }
    public void setFlightIata(String flightIata) {
        this.flightIata = flightIata;
    }
    public String getFlightIcao() {
        return flightIcao;
    }
    public void setFlightIcao(String flightIcao) {
        this.flightIcao = flightIcao;
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
    public String getDepIcao() {
        return depIcao;
    }
    public void setDepIcao(String depIcao) {
        this.depIcao = depIcao;
    }
    public String getDepTerminal() {
        return depTerminal;
    }
    public void setDepTerminal(String depTerminal) {
        this.depTerminal = depTerminal;
    }
    public String getDepGate() {
        return depGate;
    }
    public void setDepGate(String depGate) {
        this.depGate = depGate;
    }
    public String getDepTime() {
        return depTime;
    }
    public void setDepTime(String depTime) {
        this.depTime = depTime;
    }
    public String getDepTimeUtc() {
        return depTimeUtc;
    }
    public void setDepTimeUtc(String depTimeUtc) {
        this.depTimeUtc = depTimeUtc;
    }
    public String getArrIata() {
        return arrIata;
    }
    public void setArrIata(String arrIata) {
        this.arrIata = arrIata;
    }
    public String getArrIcao() {
        return arrIcao;
    }
    public void setArrIcao(String arrIcao) {
        this.arrIcao = arrIcao;
    }
    public String getArrTerminal() {
        return arrTerminal;
    }
    public void setArrTerminal(String arrTerminal) {
        this.arrTerminal = arrTerminal;
    }
    public String getArrGate() {
        return arrGate;
    }
    public void setArrGate(String arrGate) {
        this.arrGate = arrGate;
    }
    public String getArrBaggage() {
        return arrBaggage;
    }
    public void setArrBaggage(String arrBaggage) {
        this.arrBaggage = arrBaggage;
    }
    public String getArrTime() {
        return arrTime;
    }
    public void setArrTime(String arrTime) {
        this.arrTime = arrTime;
    }
    public String getArrTimeUtc() {
        return arrTimeUtc;
    }
    public void setArrTimeUtc(String arrTimeUtc) {
        this.arrTimeUtc = arrTimeUtc;
    }
    public String getCsAirlineIata() {
        return csAirlineIata;
    }
    public void setCsAirlineIata(String csAirlineIata) {
        this.csAirlineIata = csAirlineIata;
    }
    public String getCsFlightNumber() {
        return csFlightNumber;
    }
    public void setCsFlightNumber(String csFlightNumber) {
        this.csFlightNumber = csFlightNumber;
    }
    public String getCsFlightIata() {
        return csFlightIata;
    }
    public void setCsFlightIata(String csFlightIata) {
        this.csFlightIata = csFlightIata;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public Integer getDuration() {
        return duration;
    }
    public void setDuration(Integer duration) {
        this.duration = duration;
    }
    public Integer getDelayed() {
        return delayed;
    }
    public void setDelayed(Integer delayed) {
        this.delayed = delayed;
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
    public String getAircraftIcao() {
        return aircraftIcao;
    }
    public void setAircraftIcao(String aircraftIcao) {
        this.aircraftIcao = aircraftIcao;
    }
    public Long getArrTimeTs() {
        return arrTimeTs;
    }
    public void setArrTimeTs(Long arrTimeTs) {
        this.arrTimeTs = arrTimeTs;
    }
    public Long getDepTimeTs() {
        return depTimeTs;
    }
    public void setDepTimeTs(Long depTimeTs) {
        this.depTimeTs = depTimeTs;
    }
    
}