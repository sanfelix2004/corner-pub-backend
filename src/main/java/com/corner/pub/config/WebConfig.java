package com.corner.pub.config;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5501", "https://cornerpubgiovinazzo.onrender.com")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true)
                .exposedHeaders("Authorization") // Add this if you use JWT
                .maxAge(3600);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Reindirizza /admin → /admin.html
        registry.addViewController("/admin").setViewName("redirect:/admin.html");
        // Reindirizza /login → /login.html
        registry.addViewController("/login").setViewName("redirect:/login.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
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