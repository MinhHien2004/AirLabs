package Task.demo.service;

import Task.demo.dto.request.ProductCreateRequest;
import Task.demo.dto.request.ProductUpdateRequest;
import Task.demo.entity.Product;
import Task.demo.Repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;

    public List<Product> getAllProducts(){
        return productRepository.findAll();
    }

    public Product createProduct(ProductCreateRequest request){
        Product product = new Product();

        product.setTitle(request.getTitle());
        product.setPrice(request.getPrice());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setImage(request.getImage());
        product.setRating(request.getRating());

        productRepository.save(product);
        return product;
    }

    public Product getProductById(Long id){
        return productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found!"));
    }

    public Product updateProduct(Long id, ProductUpdateRequest request){
        Product product = getProductById(id);

        product.setTitle(request.getTitle());
        product.setPrice(request.getPrice());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setImage(request.getImage());
        product.setRating(request.getRating());

        productRepository.save(product);
        return product;
    }

    public void deleteProduct(Long id){
        productRepository.deleteById(id);
    }
}
