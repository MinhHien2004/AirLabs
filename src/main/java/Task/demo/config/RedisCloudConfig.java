package Task.demo.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SslOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Cấu hình Redis với SSL cho Redis Cloud
 */
@Configuration
public class RedisCloudConfig {
    
    @Value("${spring.data.redis.host}")
    private String redisHost;
    
    @Value("${spring.data.redis.port}")
    private int redisPort;
    
    @Value("${spring.data.redis.username:default}")
    private String redisUsername;
    
    @Value("${spring.data.redis.password}")
    private String redisPassword;
    
    @Value("${spring.data.redis.ssl.enabled:true}")
    private boolean sslEnabled;
    
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        // Redis server configuration
        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration();
        serverConfig.setHostName(redisHost);
        serverConfig.setPort(redisPort);
        serverConfig.setUsername(redisUsername);
        serverConfig.setPassword(redisPassword);
        
        // Lettuce client configuration with SSL
        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfig = 
                LettuceClientConfiguration.builder();
        
        if (sslEnabled) {
            // SSL configuration cho Redis Cloud
            SslOptions sslOptions = SslOptions.builder()
                    .jdkSslProvider()  // Sử dụng JDK SSL provider
                    .build();
            
            ClientOptions clientOptions = ClientOptions.builder()
                    .sslOptions(sslOptions)
                    .build();
            
            clientConfig.clientOptions(clientOptions)
                       .useSsl()  // Bật SSL
                       .disablePeerVerification();  // Tắt verify certificate (cho Redis Cloud)
        }
        
        clientConfig.commandTimeout(Duration.ofSeconds(10));
        clientConfig.shutdownTimeout(Duration.ofMillis(100));
        
        LettuceConnectionFactory factory = new LettuceConnectionFactory(
                serverConfig, 
                clientConfig.build()
        );
        
        return factory;
    }
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // String serializer cho key
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // JSON serializer cho value (hỗ trợ List<Flight>)
        GenericJackson2JsonRedisSerializer jsonSerializer = 
            new GenericJackson2JsonRedisSerializer();
        
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        return template;
    }
}
