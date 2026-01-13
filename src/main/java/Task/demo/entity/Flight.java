package Task.demo.entity;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Entity
@Table(name = "flights", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"flight_iata", "dep_time"})
})
@Data
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
public class Flight {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
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
}
