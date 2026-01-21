package Task.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Health check endpoint để:
 * 1. Render có thể kiểm tra app còn sống
 * 2. External services (UptimeRobot, etc.) ping để tránh cold start
 */
@RestController
public class HealthController {
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
    
    // XÓA mapping "/" để Spring Boot serve index.html từ /static
    // Route "/" giờ sẽ tự động serve index.html nhờ WebConfig
}
