package com.corner.pub.config;

import com.corner.pub.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class SecurityConfig {

        @Autowired
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
                return authConfig.getAuthenticationManager();
        }

        @Bean
        @Order(1)
        public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
                http
                                .securityMatcher("/api/**", "/ws-orders/**")
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(AbstractHttpConfigurer::disable)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/api/auth/**",
                                                                "/api/menu", "/api/menu/**",
                                                                "/api/in_evidenza", "/api/in_evidenza/**",
                                                                "/api/promotions", "/api/promotions/**",
                                                                "/api/reservations", "/api/reservations/**",
                                                                "/api/events", "/api/events/**",
                                                                "/api/users", "/api/users/**")
                                                .permitAll()
                                                .requestMatchers("/api/cameriere/**").authenticated()
                                                .requestMatchers("/api/cucina/**").authenticated()
                                                .anyRequest().authenticated())
                                .exceptionHandling(e -> e.defaultAuthenticationEntryPointFor(
                                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                                new AntPathRequestMatcher("/api/**")))
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        @Order(2)
        public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(AbstractHttpConfigurer::disable)
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/",
                                                                "/js/**",
                                                                "/css/**",
                                                                "/img/**",
                                                                "/fonts/**",
                                                                "/images/**",
                                                                "/login.html",
                                                                "/index.html",
                                                                "/cameriere",
                                                                "/cameriere.html",
                                                                "/cucina",
                                                                "/cucina.html",
                                                                "/menu/**",
                                                                "/events/**")
                                                .permitAll()
                                                .requestMatchers("/admin/**", "/admin.html").authenticated()
                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage("/login.html")
                                                .loginProcessingUrl("/login")
                                                .defaultSuccessUrl("/admin.html", true)
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login.html")
                                                .permitAll());

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(Arrays.asList(
                                "http://localhost:5501",
                                "http://127.0.0.1:5501",
                                "http://localhost:3000",
                                "http://127.0.0.1:3000",
                                "https://cornerpubgiovinazzo.onrender.com",
                                "https://corner-pub-backend.onrender.com"));
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
                configuration.setAllowedHeaders(
                                Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);
                configuration.setExposedHeaders(Arrays.asList("Authorization"));

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public UserDetailsService users() {
                UserDetails admin = User.withUsername("corner")
                                .password(passwordEncoder().encode("corner123"))
                                .roles("ADMIN", "STAFF")
                                .build();
                return new InMemoryUserDetailsManager(admin);
        }

        @Bean
        public FilterRegistrationBean<CommonsRequestLoggingFilter> logFilter() {
                CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
                filter.setIncludeQueryString(true);
                filter.setIncludePayload(true);
                filter.setMaxPayloadLength(10000);
                filter.setIncludeHeaders(true);
                filter.setAfterMessagePrefix("REQUEST DATA: ");

                FilterRegistrationBean<CommonsRequestLoggingFilter> registrationBean = new FilterRegistrationBean<>(
                                filter);
                registrationBean.setOrder(1);
                return registrationBean;
        }
}
