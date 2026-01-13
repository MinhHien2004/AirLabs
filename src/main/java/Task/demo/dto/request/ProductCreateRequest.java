package Task.demo.dto.request;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import Task.demo.entity.Rating;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
    "title",
    "price",
    "description",
    "category",
    "image",
    "rating"
})
public class ProductCreateRequest {
    private String title;
    private Double price;

    @Column(columnDefinition = "TEXT")
    private String description;
    private String category;
    private String image;

    @Embedded
    private Rating rating;

    // Getters and Setters
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public Double getPrice() {
        return price;
    }
    public void setPrice(Double price) {
        this.price = price;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }
    public String getImage() {
        return image;
    }
    public void setImage(String image) {
        this.image = image;
    }
    public Rating getRating() {
        return rating;
    }
    public void setRating(Rating rating) {
        this.rating = rating;
    }

    
}
