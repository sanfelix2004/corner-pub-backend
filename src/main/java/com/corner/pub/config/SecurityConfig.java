package com.corner.pub.config;

import com.corner.pub.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig {

        @Autowired
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
                return authConfig.getAuthenticationManager();
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .cors(withDefaults())
                                .csrf(AbstractHttpConfigurer::disable)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                // Permitted logic from earlier
                                                .requestMatchers(
                                                                "/",
                                                                "/api/auth/**",
                                                                "/api/menu/**",
                                                                "/api/reservations/**",
                                                                "/api/events/**",
                                                                "/api/users/**", // Expose standard API public
                                                                                 // interactions (if they were open
                                                                                 // before)
                                                                "/js/**",
                                                                "/css/**",
                                                                "/img/**",
                                                                "/fonts/**",
                                                                "/images/**",
                                                                "/login.html",
                                                                "/index.html",
                                                                "/menu/**",
                                                                "/events/**",
                                                                "/ws-orders/**", // enable websocket handshake
                                                                "/cameriere.html",
                                                                "/cameriere",
                                                                "/cucina.html",
                                                                "/cucina")
                                                .permitAll()
                                                // Protected staff routes
                                                .requestMatchers("/api/cameriere/**").authenticated()
                                                .requestMatchers("/api/cucina/**").authenticated()
                                                .requestMatchers("/admin/**", "/admin.html").authenticated()
                                                // Default
                                                .anyRequest().authenticated())
                                .exceptionHandling(e -> e.defaultAuthenticationEntryPointFor(
                                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                                new AntPathRequestMatcher("/api/**")))
                                // Add JWT before username/pass filter
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
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
