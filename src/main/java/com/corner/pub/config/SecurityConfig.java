package com.corner.pub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;

@Configuration
public class SecurityConfig {

        @Bean
        @Order(1)
        public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
                http
                                .securityMatcher("/api/**")
                                .headers(headers -> headers.cacheControl(cache -> cache.disable()))
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(AbstractHttpConfigurer::disable)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/api/menu", "/api/menu/**",
                                                                "/api/in_evidenza", "/api/in_evidenza/**",
                                                                "/api/promotions", "/api/promotions/**",
                                                                "/api/reservations", "/api/reservations/**",
                                                                "/api/events", "/api/events/**",
                                                                "/api/speciali", "/api/speciali/**",
                                                                "/api/run-club", "/api/run-club/**",
                                                                "/api/booking-settings", "/api/booking-settings/**",
                                                                "/api/users", "/api/users/**")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .exceptionHandling(e -> e.defaultAuthenticationEntryPointFor(
                                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                                new AntPathRequestMatcher("/api/**")));

                return http.build();
        }

        @Bean
        @Order(2)
        public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
                http
                                .headers(headers -> headers.cacheControl(cache -> cache.disable()))
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(AbstractHttpConfigurer::disable)
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/",
                                                                "/health",
                                                                "/js/**",
                                                                "/css/**",
                                                                "/img/**",
                                                                "/fonts/**",
                                                                "/images/**",
                                                                "/favicon.ico",
                                                                "/uploads/**",
                                                                "/login.html",
                                                                "/index.html",
                                                                "/privacy.html",
                                                                "/cookie.html",
                                                                "/conferma-evento.html",
                                                                "/run-club.html",
                                                                "/run-club-login.html",
                                                                "/regolamento-run-club.html",
                                                                "/menu/**",
                                                                "/events/**")
                                                .permitAll()
                                                .requestMatchers("/run-club-admin.html", "/admin/run-club/**")
                                                .hasAnyRole("ADMIN", "RUNCLUB")
                                                .requestMatchers("/admin/**", "/admin.html").hasRole("ADMIN")
                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage("/login.html")
                                                .loginProcessingUrl("/login")
                                                .successHandler(loginSuccessHandler())
                                                .failureHandler(loginFailureHandler())
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
                                                .logoutSuccessHandler(logoutSuccessHandler())
                                                .permitAll())
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint((request, response, authException) -> response
                                                                .sendRedirect(loginPageFor(request))));

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(Arrays.asList(
                                "http://localhost:8080",
                                "http://127.0.0.1:8080",
                                "http://localhost:5501",
                                "http://127.0.0.1:5501",
                                "http://localhost:3000",
                                "http://127.0.0.1:3000",
                                "http://cornerpubgiovinazzo.com",
                                "https://cornerpubgiovinazzo.com",
                                "http://www.cornerpubgiovinazzo.com",
                                "https://www.cornerpubgiovinazzo.com",
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
                                .roles("ADMIN")
                                .build();
                UserDetails runClub = User.withUsername("runclub")
                                .password(passwordEncoder().encode("runclub123"))
                                .roles("RUNCLUB")
                                .build();
                return new InMemoryUserDetailsManager(admin, runClub);
        }

        @Bean
        public SavedRequestAwareAuthenticationSuccessHandler loginSuccessHandler() {
                SavedRequestAwareAuthenticationSuccessHandler handler = new SavedRequestAwareAuthenticationSuccessHandler() {
                        @Override
                        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        org.springframework.security.core.Authentication authentication)
                                        throws IOException, jakarta.servlet.ServletException {
                                boolean admin = authentication.getAuthorities().stream()
                                                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
                                if (!admin) {
                                        clearAuthenticationAttributes(request);
                                        getRedirectStrategy().sendRedirect(request, response, "/run-club-admin.html");
                                        return;
                                }
                                super.onAuthenticationSuccess(request, response, authentication);
                        }
                };
                handler.setDefaultTargetUrl("/admin.html");
                handler.setAlwaysUseDefaultTargetUrl(false);
                return handler;
        }

        private AuthenticationFailureHandler loginFailureHandler() {
                return (request, response, exception) -> response.sendRedirect(failurePageFor(request));
        }

        private LogoutSuccessHandler logoutSuccessHandler() {
                return (request, response, authentication) -> response.sendRedirect(loginPageFor(request));
        }

        private static String loginPageFor(HttpServletRequest request) {
                String uri = request.getRequestURI() == null ? "" : request.getRequestURI();
                String referer = request.getHeader("Referer");
                if (uri.startsWith("/run-club") || uri.startsWith("/admin/run-club")
                                || (referer != null && referer.contains("run-club"))) {
                        return "/run-club-login.html";
                }
                return "/login.html";
        }

        private static String failurePageFor(HttpServletRequest request) {
                String referer = request.getHeader("Referer");
                if (referer != null && referer.contains("run-club-login")) {
                        return "/run-club-login.html?error";
                }
                return "/login.html?error";
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
