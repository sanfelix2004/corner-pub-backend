package com.corner.pub.repository;

import com.corner.pub.model.Allergen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AllergenRepository extends JpaRepository<Allergen, Long> {
    Optional<Allergen> findByCode(String code);
    List<Allergen> findAllByCodeIn(Collection<String> codes);
    List<Allergen> findAllByOrderByCodeAsc();
}