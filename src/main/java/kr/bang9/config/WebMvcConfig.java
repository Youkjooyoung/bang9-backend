package kr.bang9.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@ConditionalOnProperty(name = "app.upload.mode", havingValue = "local", matchIfMissing = true)
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.local.storage-path:./uploads}")
    private String storagePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path base = Paths.get(storagePath).toAbsolutePath().normalize();
        String location = "file:" + base + "/";
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations(location)
            .setCachePeriod(3600);
    }
}
