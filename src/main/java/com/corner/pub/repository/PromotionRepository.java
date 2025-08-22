package com.corner.pub.repository;

import com.corner.pub.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    @Query("""
           select distinct p from Promotion p
           left join fetch p.items i
           left join fetch i.menuItem m
           """)
    List<Promotion> findAllFetched();

    @Query("""
           select distinct p from Promotion p
           left join fetch p.items i
           left join fetch i.menuItem m
           where p.id = :id
           """)
    Optional<Promotion> findByIdFetched(@Param("id") Long id);

    @Query("""
           select distinct p from Promotion p
           left join fetch p.items i
           left join fetch i.menuItem m
           where p.attiva = true
           """)
    List<Promotion> findActiveFetched();

    // ✅ Attive + valide per data, senza literal
    @Query("""
           select distinct p from Promotion p
           left join fetch p.items i
           left join fetch i.menuItem m
           where p.attiva = true
             and (p.dataInizio is null or p.dataInizio <= :today)
             and (p.dataFine   is null or p.dataFine   >= :today)
           """)
    List<Promotion> findActiveValidFetched(@Param("today") LocalDate today);
}
