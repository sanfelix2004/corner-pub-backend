package com.corner.pub.repository;

import com.corner.pub.model.EventRegistration;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    Optional<EventRegistration> findByEventIdAndUserId(Long eventId, Long userId);

    @Query("SELECT COUNT(e) FROM EventRegistration e WHERE e.event.id = :eventId")
    long countByEventId(@Param("eventId") Long eventId);

    @Transactional
    void deleteByEventId(Long eventId);

    @Query("SELECT COALESCE(SUM(r.partecipanti), 0) FROM EventRegistration r WHERE r.event.id = :eventId")
    Optional<Long> sumPartecipantiByEventId(@Param("eventId") Long eventId);

    List<EventRegistration> findByEventId(Long eventId);

}
