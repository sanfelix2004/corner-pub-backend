package com.corner.pub.repository;

import com.corner.pub.model.EventRegistration;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    Optional<EventRegistration> findByEventIdAndUserId(Long eventId, Long userId);

    @Query("SELECT COUNT(e) FROM EventRegistration e WHERE e.event.id = :eventId")
    long countByEventId(@Param("eventId") Long eventId);

    @Transactional
    void deleteByEventId(Long eventId);

    Optional<EventRegistration> findByEventIdAndUser_Phone(Long eventId, String phone);

    @Query("SELECT COALESCE(SUM(r.partecipanti), 0) FROM EventRegistration r WHERE r.event.id = :eventId")
    Optional<Long> sumPartecipantiByEventId(@Param("eventId") Long eventId);

    List<EventRegistration> findByEventId(Long eventId);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN TRUE ELSE FALSE END " +
            "FROM EventRegistration r " +
            "WHERE r.user.phone = :phone " +
            "AND DATE(r.event.data) = :date")
    boolean existsByPhoneAndEventDate(@Param("phone") String phone,
                                      @Param("date") LocalDate date);

    List<EventRegistration> findByUser_Phone(String phone);

}
