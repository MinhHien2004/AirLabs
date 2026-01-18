package Task.demo.dto;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import Task.demo.dto.response.FlightDisplayDTO;
import Task.demo.entity.Flight;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO để lưu trữ thông tin Flight trong Redis Cache
 * Hỗ trợ cả single Flight và List<FlightDisplayDTO>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlightCacheEntry implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Danh sách flights (dùng cho cache theo IATA)
     */
    @JsonProperty("flights")
    private List<FlightDisplayDTO> flights;
    
    /**
     * Dữ liệu chuyến bay đơn lẻ (dùng cho cache từng flight)
     */
    @JsonProperty("flight")
    private Flight flight;
    
    /**
     * Mã hash của đối tượng Flight để so sánh thay đổi
     */
    @JsonProperty("content_hash")
    private String contentHash;
    
    /**
     * Thời điểm cache được cập nhật (Unix timestamp milliseconds)
     */
    @JsonProperty("cached_at")
    private long cachedAt;
    
    /**
     * Thời điểm hết hạn (Unix timestamp milliseconds)
     */
    @JsonProperty("expire_at")
    private long expireAt;
    
    /**
     * Thời điểm hết hạn logic (soft expiration) - Unix timestamp milliseconds
     */
    @JsonProperty("logical_expire_at")
    private long logicalExpireAt;
    
    /**
     * Composite key dùng để xác định unique flight
     */
    @JsonProperty("composite_key")
    private String compositeKey;
    
    /**
     * Constructor cho List<FlightDisplayDTO> với Duration
     */
    public FlightCacheEntry(List<FlightDisplayDTO> flights, Instant cachedAt, Duration ttl) {
        this.flights = flights;
        this.cachedAt = cachedAt.toEpochMilli();
        this.expireAt = cachedAt.plus(ttl).toEpochMilli();
        this.logicalExpireAt = this.expireAt;
    }
    
    /**
     * Constructor từ Flight entity
     */
    public FlightCacheEntry(Flight flight, String contentHash, long logicalTtlMinutes) {
        this.flight = flight;
        this.contentHash = contentHash;
        this.cachedAt = Instant.now().toEpochMilli();
        this.logicalExpireAt = this.cachedAt + (logicalTtlMinutes * 60 * 1000);
        this.expireAt = this.logicalExpireAt;
        this.compositeKey = generateCompositeKey(flight);
    }
    
    /**
     * Kiểm tra xem cache entry đã hết hạn chưa
     */
    public boolean isExpired() {
        return Instant.now().toEpochMilli() > this.expireAt;
    }
    
    /**
     * Kiểm tra xem cache entry đã hết hạn logic chưa
     */
    public boolean isLogicallyExpired() {
        return Instant.now().toEpochMilli() > this.logicalExpireAt;
    }
    
    /**
     * Lấy cachedAt dưới dạng Instant
     */
    public Instant getCachedAtInstant() {
        return Instant.ofEpochMilli(this.cachedAt);
    }
    
    /**
     * Tạo composite key từ Flight
     */
    public static String generateCompositeKey(Flight flight) {
        String depIata = flight.getDepIata() != null ? flight.getDepIata() : "";
        String flightIata = flight.getFlightIata() != null ? flight.getFlightIata() : "";
        String depTime = flight.getDepTime() != null ? flight.getDepTime() : "";
        return depIata + "_" + flightIata + "_" + depTime;
    }
}
