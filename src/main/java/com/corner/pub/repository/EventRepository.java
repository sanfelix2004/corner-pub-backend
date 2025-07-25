package com.corner.pub.repository;

import com.corner.pub.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findAllByOrderByDataAsc();


    List<Event> findByDataAfterOrderByDataAsc(LocalDateTime date);

    List<Event> findByDataBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT e FROM Event e WHERE e.data > :date ORDER BY e.data ASC LIMIT :limit")
    List<Event> findTopByDataAfterOrderByDataAsc(
            @Param("date") LocalDateTime date,
            @Param("limit") int limit);
}
