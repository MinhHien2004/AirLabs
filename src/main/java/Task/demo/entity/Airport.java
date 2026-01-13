package Task.demo.entity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;


import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@Entity
@Table(name = "airports")
@Data
@JsonPropertyOrder({
    "name",
    "iata_code",
    "icao_code",
    "lat",
    "lon",
    "country_code"
})
public class Airport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String iata_code;
    private String icao_code;
    private Double lat;
    private Double lon;
    private String country_code;


}
