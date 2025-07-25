package com.corner.pub.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://aws-0-eu-north-1.pooler.supabase.com:6543/postgres");
        config.setUsername("postgres.ovirextifdmzsdkjydgc");
        config.setPassword("Frasanf12092004_");
        config.setMaximumPoolSize(3);

        // Aggiungi queste proprietà per gestire meglio i picchi
        config.setInitializationFailTimeout(30000);
        config.setConnectionTimeout(10000);
        config.setIdleTimeout(30000);
        config.setMaxLifetime(60000);

        return new HikariDataSource(config);
    }
}