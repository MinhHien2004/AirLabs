package Task.demo.dto;

import java.io.Serializable;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

import Task.demo.entity.Flight;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO để lưu trữ thông tin Flight trong Redis Cache
 * Bao gồm thêm metadata để hỗ trợ chiến lược caching thông minh
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlightCacheEntry implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Dữ liệu chuyến bay
     */
    @JsonProperty("flight")
    private Flight flight;
    
    /**
     * Mã hash của đối tượng Flight để so sánh thay đổi
     * Dùng SHA-256 hash của toàn bộ nội dung
     */
    @JsonProperty("content_hash")
    private String contentHash;
    
    /**
     * Thời điểm cache được cập nhật (Unix timestamp milliseconds)
     */
    @JsonProperty("cached_at")
    private long cachedAt;
    
    /**
     * Thời điểm hết hạn logic (soft expiration) - Unix timestamp milliseconds
     * Sau thời điểm này, dữ liệu vẫn được trả về nhưng sẽ kích hoạt async update
     */
    @JsonProperty("logical_expire_at")
    private long logicalExpireAt;
    
    /**
     * Composite key dùng để xác định unique flight: dep_iata + flight_iata + dep_time
     */
    @JsonProperty("composite_key")
    private String compositeKey;
    
    /**
     * Constructor từ Flight entity
     */
    public FlightCacheEntry(Flight flight, String contentHash, long logicalTtlMinutes) {
        this.flight = flight;
        this.contentHash = contentHash;
        this.cachedAt = Instant.now().toEpochMilli();
        this.logicalExpireAt = this.cachedAt + (logicalTtlMinutes * 60 * 1000);
        this.compositeKey = generateCompositeKey(flight);
    }
    
    /**
     * Kiểm tra xem cache entry đã hết hạn logic chưa
     */
    public boolean isLogicallyExpired() {
        return Instant.now().toEpochMilli() > this.logicalExpireAt;
    }
    
    /**
     * Tạo composite key từ Flight
     * Format: {dep_iata}_{flight_iata}_{dep_time}
     */
    public static String generateCompositeKey(Flight flight) {
        String depIata = flight.getDepIata() != null ? flight.getDepIata() : "";
        String flightIata = flight.getFlightIata() != null ? flight.getFlightIata() : "";
        String depTime = flight.getDepTime() != null ? flight.getDepTime() : "";
        return depIata + "_" + flightIata + "_" + depTime;
    }
}
