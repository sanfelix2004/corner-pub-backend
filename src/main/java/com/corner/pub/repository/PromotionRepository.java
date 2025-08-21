package com.corner.pub.repository;

import com.corner.pub.model.Promotion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    @EntityGraph(attributePaths = {"items", "items.menuItem"})
    List<Promotion> findAll();

    @EntityGraph(attributePaths = {"items", "items.menuItem"})
    Optional<Promotion> findById(Long id);
}