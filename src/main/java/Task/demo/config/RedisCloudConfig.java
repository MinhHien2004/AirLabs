package Task.demo.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.SslOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.api.StatefulConnection;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;

/**
 * Cấu hình Redis với Connection Pooling và SSL cho Redis Cloud
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
    
    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean sslEnabled;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        // Redis server configuration
        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration();
        serverConfig.setHostName(redisHost);
        serverConfig.setPort(redisPort);
        serverConfig.setUsername(redisUsername);
        serverConfig.setPassword(redisPassword);
        
        // Connection pool configuration - QUAN TRỌNG để tăng performance
        GenericObjectPoolConfig<StatefulConnection<?, ?>> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(16);           // Max active connections
        poolConfig.setMaxIdle(8);             // Max idle connections
        poolConfig.setMinIdle(2);             // Min idle (keep warm)
        poolConfig.setMaxWait(Duration.ofSeconds(2));  // Max wait time
        poolConfig.setTestOnBorrow(true);     // Validate connection before use
        poolConfig.setTestWhileIdle(true);    // Validate idle connections
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
        
        // Socket options cho kết nối nhanh hơn
        SocketOptions socketOptions = SocketOptions.builder()
                .connectTimeout(Duration.ofSeconds(15))  // Tăng timeout lên 15s cho Redis Cloud
                .keepAlive(true)
                .tcpNoDelay(true)  // Tắt Nagle algorithm
                .build();
        
        // Client options
        ClientOptions.Builder clientOptionsBuilder = ClientOptions.builder()
                .socketOptions(socketOptions)
                .autoReconnect(true)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(5)));
        
        if (sslEnabled) {
            SslOptions sslOptions = SslOptions.builder()
                    .jdkSslProvider()
                    .build();
            clientOptionsBuilder.sslOptions(sslOptions);
        }
        
        // Build Lettuce client configuration với connection pool
        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder clientConfigBuilder = 
                LettucePoolingClientConfiguration.builder()
                        .poolConfig(poolConfig)
                        .clientOptions(clientOptionsBuilder.build())
                        .commandTimeout(Duration.ofSeconds(5))
                        .shutdownTimeout(Duration.ofMillis(100));
        
        if (sslEnabled) {
            clientConfigBuilder.useSsl().disablePeerVerification();
        }
        
        LettuceConnectionFactory factory = new LettuceConnectionFactory(
                serverConfig, 
                clientConfigBuilder.build()
        );
        
        // TẮT Eager init để không block startup nếu Redis chậm
        factory.setEagerInitialization(false);
        
        return factory;
    }
    
    @Bean
    @SuppressWarnings("deprecation")  // Jackson2JsonRedisSerializer deprecated but still works
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // String serializer cho key
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // JSON serializer cho value (hỗ trợ List<Flight>) - sử dụng Jackson2JsonRedisSerializer
        Jackson2JsonRedisSerializer<Object> jsonSerializer = 
            new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
        
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        // Enable transaction support
        template.setEnableTransactionSupport(false);  // Tắt transaction để tăng tốc

        return template;
    }
}
