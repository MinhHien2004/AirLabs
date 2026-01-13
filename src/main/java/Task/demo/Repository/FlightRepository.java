package Task.demo.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import Task.demo.entity.Flight;

import java.util.List;

@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {
    List<Flight> findByDepIata(String depIata);

    boolean existsByFlightIataAndDepTime(String flightIata, String depTime);
    
    Flight findByFlightIataAndDepTime(String flightIata, String depTime);

    @Query("SELECT DISTINCT flight.depIata FROM Flight flight ORDER BY flight.depIata")
    List<String> findAllDepIata();

    List<Flight> findByArrIata(String arrIata);
}
