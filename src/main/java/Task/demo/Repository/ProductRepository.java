package Task.demo.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import Task.demo.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

}
