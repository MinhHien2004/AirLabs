package Task.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "airlabs.api")
@Data
public class AirLabsConfig {
    private String baseUrl;
    private String apiKey;
}
