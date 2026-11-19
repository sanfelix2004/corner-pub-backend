package com.corner.pub.repository;

import com.corner.pub.model.Speciale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SpecialeRepository extends JpaRepository<Speciale, Long> {

    List<Speciale> findAllByOrderByDataInizioDesc();

    java.util.Optional<Speciale> findByEventoId(Long eventoId);

    @Query("""
            SELECT s FROM Speciale s
            WHERE s.attivo = true
              AND s.dataInizio <= :today
              AND (s.dataFine IS NULL OR s.dataFine >= :today)
            ORDER BY s.dataInizio ASC, s.id ASC
            """)
    List<Speciale> findActiveValid(@Param("today") LocalDate today);
}
