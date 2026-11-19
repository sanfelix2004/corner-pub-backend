package com.corner.pub.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Hibernate ddl-auto=update non toglie i NOT NULL già presenti.
 * Gli speciali possono durare "per sempre" ⇒ data_fine deve essere nullable.
 */
@Component
public class SpecialeSchemaFix implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SpecialeSchemaFix.class);

    private final JdbcTemplate jdbc;

    public SpecialeSchemaFix(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            Boolean notNull = jdbc.queryForObject("""
                    SELECT is_nullable = 'NO'
                    FROM information_schema.columns
                    WHERE table_schema = current_schema()
                      AND table_name = 'speciale'
                      AND column_name = 'data_fine'
                    """, Boolean.class);
            if (Boolean.TRUE.equals(notNull)) {
                jdbc.execute("ALTER TABLE speciale ALTER COLUMN data_fine DROP NOT NULL");
                log.info("Colonna speciale.data_fine resa nullable (durata illimitata)");
            }
        } catch (Exception e) {
            log.warn("SpecialeSchemaFix: {}", e.getMessage());
        }
    }
}
