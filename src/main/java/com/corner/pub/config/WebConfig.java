package com.corner.pub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:5173",
                        "http://127.0.0.1:5500",
                        "https://corner-frontend.onrender.com"
                )
                .allowedMethods("GET", "POST", "DELETE")
                .allowCredentials(true)
                .allowedHeaders("*");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Il forward di admin.html solo su /admin e /admin/
        registry.addViewController("/admin")
                .setViewName("forward:/admin.html");
        registry.addViewController("/admin/")
                .setViewName("forward:/admin.html");
        // Non mappare /admin/**, così /admin/in_evidenza resta sui controller REST
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // risorse statiche CSS/JS/... (se lo hai già)
        registry.addResourceHandler("/css/**", "/js/**", "/images/**")
                .addResourceLocations("classpath:/static/css/", "classpath:/static/js/", "classpath:/static/images/");

        // aggiungi questo per caricare placeholder e eventuali img locali
        registry.addResourceHandler("/img/**")
                .addResourceLocations("classpath:/static/img/");
    }
}