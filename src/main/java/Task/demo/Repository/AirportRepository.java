package Task.demo.Repository;

import Task.demo.entity.Airport;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AirportRepository extends JpaRepository<Airport, Long> {

    @Query("SELECT a FROM Airport a WHERE a.icao_code = ?1")
    Optional<Airport> findByIcao_code(String icao_code);
    
}