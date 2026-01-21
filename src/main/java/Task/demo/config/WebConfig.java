package Task.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Cấu hình Web MVC để hỗ trợ React SPA routing
 * Tất cả các route không phải API sẽ trả về index.html
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve static resources (JS, CSS, images)
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/");
        
        // Serve index.html for all non-API routes (SPA routing)
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);
                        
                        // Nếu file tồn tại, return file đó
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }
                        
                        // Nếu không tồn tại và không phải API route, return index.html (cho React Router)
                        if (!resourcePath.startsWith("api/")) {
                            return new ClassPathResource("/static/index.html");
                        }
                        
                        return null;
                    }
                });
    }
}
