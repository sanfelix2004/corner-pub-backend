package com.corner.pub.repository;

import com.corner.pub.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    Optional<Reservation> findByUser_PhoneAndDate(String phone, LocalDate date);
}
