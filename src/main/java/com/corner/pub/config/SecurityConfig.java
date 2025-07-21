package com.corner.pub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .headers(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // permettiamo health e error (usati da Render)
                        .requestMatchers("/health", "/error").permitAll()
                        // manteniamo public le API esistenti
                        .requestMatchers("/api/**", "/admin/**").permitAll()
                        // tutto il resto richiede autenticazione
                        .anyRequest().authenticated()
                )
                .httpBasic(withDefaults());

        return http.build();
    }

    @Bean
    public FilterRegistrationBean<CommonsRequestLoggingFilter> logFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(10000);
        filter.setIncludeHeaders(false);

        FilterRegistrationBean<CommonsRequestLoggingFilter> registrationBean =
                new FilterRegistrationBean<>(filter);
        registrationBean.setOrder(1);
        return registrationBean;
    }
}
