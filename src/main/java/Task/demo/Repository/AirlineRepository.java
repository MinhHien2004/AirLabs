package Task.demo.Repository;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import Task.demo.entity.Airline;

@Repository
public interface AirlineRepository extends JpaRepository<Airline, Long> {

    @Query("SELECT a FROM Airline a WHERE a.iata_code = ?1")
    Optional<Airline> findByIata_code(String iata_code);
    
}
