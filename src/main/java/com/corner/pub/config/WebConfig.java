package com.corner.pub.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:8080",
                        "http://127.0.0.1:8080",
                        "https://cornerpubgiovinazzo.onrender.com")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true)
                .exposedHeaders("Authorization")
                .maxAge(3600);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/admin").setViewName("redirect:/admin.html");
        registry.addViewController("/login").setViewName("redirect:/login.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(uploadDir).toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);

        registry.addResourceHandler(
                "/css/**", "/js/**", "/images/**", "/img/**", "/fonts/**")
                .addResourceLocations(
                        "classpath:/static/css/",
                        "classpath:/static/js/",
                        "classpath:/static/images/",
                        "classpath:/static/img/",
                        "classpath:/static/fonts/");
    }
}
