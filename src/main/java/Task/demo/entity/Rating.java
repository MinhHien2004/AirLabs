package Task.demo.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@Embeddable
@Data
@JsonPropertyOrder({"rate", "count"})
public class Rating {
    private Double rate;
    private Integer count;
}
