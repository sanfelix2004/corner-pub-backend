package com.corner.pub.repository;

import com.corner.pub.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    Optional<Reservation> findByUser_PhoneAndDate(String phone, LocalDate date);
    List<Reservation> findAllByDate(LocalDate date);
    List<Reservation> findAllByUser_Phone(String phone);
    List<Reservation> findAllByDateAndTime(LocalDate date, java.time.LocalTime time);
    @Query("SELECT r FROM Reservation r LEFT JOIN FETCH r.event WHERE r.date >= :today ORDER BY r.date ASC")
    List<Reservation> findAllFromToday(@Param("today") LocalDate today);
}
