package com.corner.pub.repository;

import com.corner.pub.model.EventRegistration;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    @Query("SELECT COUNT(e) FROM EventRegistration e WHERE e.event.id = :eventId")
    long countByEventId(@Param("eventId") Long eventId);

    @Transactional
    void deleteByEventId(Long eventId);

    List<EventRegistration> findByEventId(Long eventId);

}
