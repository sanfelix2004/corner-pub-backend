package com.corner.pub.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Il vincolo storico UNIQUE(user_id, event_id) impedisce più date della stessa
 * serie ricorrente. Resta solo UNIQUE(user_id, event_id, occurrence_date).
 */
@Component
public class EventRegistrationSchemaFix implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EventRegistrationSchemaFix.class);

    private final JdbcTemplate jdbc;

    public EventRegistrationSchemaFix(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            Integer count = jdbc.queryForObject("""
                    SELECT COUNT(*)
                    FROM pg_constraint
                    WHERE conname = 'uk4luonmc0xuogbj97rm90k5r8b'
                    """, Integer.class);
            if (count != null && count > 0) {
                jdbc.execute("ALTER TABLE event_registration DROP CONSTRAINT uk4luonmc0xuogbj97rm90k5r8b");
                log.info("Rimosso vincolo UNIQUE(user_id, event_id) da event_registration");
            }
        } catch (Exception e) {
            log.warn("EventRegistrationSchemaFix: {}", e.getMessage());
        }
    }
}
