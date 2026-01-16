package Task.demo.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Cấu hình Thread Pool cho Async operations
 * Dùng cho Logical Expiration - async update khi cache hết hạn logic
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {
    
    @Bean(name = "flightUpdateExecutor")
    public Executor flightUpdateExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core pool size - số thread luôn sẵn sàng
        executor.setCorePoolSize(2);
        
        // Max pool size - số thread tối đa khi queue đầy
        executor.setMaxPoolSize(5);
        
        // Queue capacity - số task có thể chờ trong queue
        executor.setQueueCapacity(100);
        
        // Thread name prefix để dễ debug
        executor.setThreadNamePrefix("FlightAsyncUpdate-");
        
        // Cho phép thread timeout để giải phóng resource
        executor.setAllowCoreThreadTimeOut(true);
        executor.setKeepAliveSeconds(60);
        
        executor.initialize();
        return executor;
    }
    
    @Override
    public Executor getAsyncExecutor() {
        return flightUpdateExecutor();
    }
}
