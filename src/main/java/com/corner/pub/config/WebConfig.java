package com.corner.pub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5500") // <--- AGGIUNGI QUESTO
                .allowedMethods("GET", "POST", "DELETE")
                .allowCredentials(true)  // Se vuoi permettere invio cookie/autenticazione
                .allowedHeaders("*");    // Header che vuoi permettere (es. Authorization)
    }
}
