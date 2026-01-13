package Task.demo.dto.response;

import Task.demo.entity.Rating;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@Data
@JsonPropertyOrder({
    "id",
    "title",
    "price",
    "description",
    "category",
    "image",
    "rating"
})
public class ProductApiResponse {
    private Long id;
    private String title;
    private Double price;
    private String description;
    private String category;
    private String image;
    private Rating rating;
}

