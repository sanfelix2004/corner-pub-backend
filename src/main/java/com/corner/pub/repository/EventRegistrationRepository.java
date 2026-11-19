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

    boolean existsByUserIdAndEventIdAndOccurrenceDate(Long userId, Long eventId, LocalDate occurrenceDate);

    Optional<EventRegistration> findByEventIdAndUserId(Long eventId, Long userId);

    @Query("SELECT COUNT(e) FROM EventRegistration e WHERE e.event.id = :eventId")
    long countByEventId(@Param("eventId") Long eventId);

    @Transactional
    void deleteByEventId(Long eventId);

    Optional<EventRegistration> findByEventIdAndUser_Phone(Long eventId, String phone);

    @Query("SELECT COALESCE(SUM(r.partecipanti), 0) FROM EventRegistration r WHERE r.event.id = :eventId")
    Optional<Long> sumPartecipantiByEventId(@Param("eventId") Long eventId);

    @Query("SELECT COALESCE(SUM(r.partecipanti), 0) FROM EventRegistration r WHERE r.event.id = :eventId AND r.occurrenceDate = :date")
    Optional<Long> sumPartecipantiByEventIdAndOccurrenceDate(@Param("eventId") Long eventId,
                                                             @Param("date") LocalDate date);

    List<EventRegistration> findByEventId(Long eventId);

    List<EventRegistration> findByEventIdAndOccurrenceDateOrderByCreatedAtAsc(Long eventId, LocalDate occurrenceDate);

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN TRUE ELSE FALSE END
            FROM EventRegistration r
            WHERE r.user.phone = :phone
              AND r.event.runClub = :runClub
              AND (
                    r.occurrenceDate = :date
                 OR (r.occurrenceDate IS NULL
                     AND r.event.data >= :dayStart
                     AND r.event.data < :dayEnd)
              )
            """)
    boolean existsByPhoneAndEventDateAndRunClub(@Param("phone") String phone,
                                                @Param("date") LocalDate date,
                                                @Param("runClub") boolean runClub,
                                                @Param("dayStart") java.time.LocalDateTime dayStart,
                                                @Param("dayEnd") java.time.LocalDateTime dayEnd);

    boolean existsByTicketCode(String ticketCode);

    Optional<EventRegistration> findByTicketCode(String ticketCode);

    List<EventRegistration> findByUser_Phone(String phone);

    Optional<EventRegistration> findByConfirmationToken(String confirmationToken);

}
