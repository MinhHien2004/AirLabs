package Task.demo.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import Task.demo.entity.Flight;

import java.util.List;
import java.util.Set;

@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {
    List<Flight> findByDepIata(String depIata);

    boolean existsByFlightIataAndDepTime(String flightIata, String depTime);
    
    Flight findByFlightIataAndDepTime(String flightIata, String depTime);

    @Query("SELECT DISTINCT flight.depIata FROM Flight flight ORDER BY flight.depIata")
    List<String> findAllDepIata();

    List<Flight> findByArrIata(String arrIata);
    
    /**
     * Batch query để tìm nhiều flights cùng lúc - tránh N+1 problem
     */
    @Query("SELECT f FROM Flight f WHERE CONCAT(f.flightIata, '|', f.depTime) IN :compositeKeys")
    List<Flight> findByCompositeKeys(@Param("compositeKeys") Set<String> compositeKeys);
    
    /**
     * Batch check existence để giảm số query
     */
    @Query("SELECT CONCAT(f.flightIata, '|', f.depTime) FROM Flight f WHERE CONCAT(f.flightIata, '|', f.depTime) IN :compositeKeys")
    Set<String> findExistingCompositeKeys(@Param("compositeKeys") Set<String> compositeKeys);
}
