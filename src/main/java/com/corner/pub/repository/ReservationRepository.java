package com.corner.pub.repository;

import com.corner.pub.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
           select r from Reservation r
           where r.user.phone = :phone
             and r.date = :date
           """)
    Optional<Reservation> findByUserPhoneAndDate(@Param("phone") String phone,
                                                 @Param("date") LocalDate date);

    @Query("""
           select r from Reservation r
           where r.date = :date
           order by r.time asc
           """)
    List<Reservation> findAllByDate(@Param("date") LocalDate date);

    @Query("""
           select r from Reservation r
           where r.user.phone = :phone
           order by r.date asc, r.time asc
           """)
    List<Reservation> findAllByUserPhone(@Param("phone") String phone);

    @Query("""
           select r from Reservation r
           where r.date = :date
             and r.time = :time
           """)
    List<Reservation> findAllByDateAndTime(@Param("date") LocalDate date,
                                           @Param("time") LocalTime time);

    @Query("""
           select distinct r from Reservation r
           left join fetch r.event
           where r.user.phone = :phone
           order by r.date asc, r.time asc
           """)
    List<Reservation> findAllWithEventByUserPhone(@Param("phone") String phone);

    @Query("""
           select distinct r from Reservation r
           left join fetch r.event
           where r.date >= :today
           order by r.date asc, r.time asc
           """)
    List<Reservation> findAllFromToday(@Param("today") LocalDate today);
}
