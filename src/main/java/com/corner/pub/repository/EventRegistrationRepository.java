package com.corner.pub.repository;

import com.corner.pub.model.EventRegistration;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    long countByEventId(Long eventId); // 👉 questo conta il numero di iscritti all'evento

    @Transactional
    void deleteByEventId(Long eventId);

    List<EventRegistration> findByEventId(Long eventId);

}
