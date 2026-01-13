package Task.demo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@Entity
@Table(name = "products")
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
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private Double price;

    @Column(columnDefinition = "TEXT")
    private String description;
    private String category;
    private String image;

    @Embedded
    private Rating rating;
}
