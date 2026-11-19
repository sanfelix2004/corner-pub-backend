package com.corner.pub.repository;

import com.corner.pub.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findAllByOrderByDataAsc();

    List<Event> findByDataAfterOrderByDataAsc(LocalDateTime date);

    List<Event> findByDataBetween(LocalDateTime start, LocalDateTime end);

    Optional<Event> findBySpecialeId(Long specialeId);

    @Query("SELECT e FROM Event e WHERE e.data > :date ORDER BY e.data ASC LIMIT :limit")
    List<Event> findTopByDataAfterOrderByDataAsc(
            @Param("date") LocalDateTime date,
            @Param("limit") int limit);

    List<Event> findByParentEventIdOrderByDataAsc(Long parentEventId);

    List<Event> findBySpecialeTrueAndRicorrenteTrue();

    boolean existsByParentEventIdAndData(Long parentEventId, LocalDateTime data);

    long countByParentEventId(Long parentEventId);

    List<Event> findByRunClubTrueOrderByDataAsc();

    List<Event> findByRunClubTrueAndDataAfterOrderByDataAsc(LocalDateTime date);

    /**
     * Eventi ancora prenotabili: single-shot futuri, oppure ricorrenti
     * senza data fine (o con data fine ancora valida).
     */
    @Query("""
            SELECT e FROM Event e
            WHERE (e.ricorrente = false AND e.data > :now)
               OR (e.ricorrente = true AND (e.dataFine IS NULL OR e.dataFine >= :today))
            ORDER BY e.data ASC
            """)
    List<Event> findBookable(@Param("now") LocalDateTime now, @Param("today") LocalDate today);

    @Query("""
            SELECT e FROM Event e
            LEFT JOIN FETCH e.parentEvent p
            WHERE e.id = :id
            """)
    Optional<Event> findDetailedById(@Param("id") Long id);
}
