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
                .allowedOrigins(
                  //      "http://localhost:5500",    // Live Server default
                    //    "http://127.0.0.1:5500"     // Alternative Live Server
                    //    "http://localhost:3000",     // React default
                   //     "http://127.0.0.1:3000",     // React alternative
                     //   "http://localhost:8080",     // Backend/alternative FE
                     //   "http://127.0.0.1:8080"    // Backend alternative
                   //     "http://localhost:8081",     // Altri server FE
                  //      "http://127.0.0.1:8081",      // Altri server FE
                        "https://cornerpubgiovinazzo.onrender.com"
                )
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true)
                .exposedHeaders("Authorization")
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