// src/main/java/com/corner/pub/repository/InEvidenzaRepository.java
package com.corner.pub.repository;

import com.corner.pub.model.InEvidenza;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InEvidenzaRepository extends JpaRepository<InEvidenza, Long> {
    // eventualmente metodi custom…
}