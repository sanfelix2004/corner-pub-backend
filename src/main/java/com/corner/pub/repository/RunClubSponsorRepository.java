package com.corner.pub.repository;

import com.corner.pub.model.RunClubSponsor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RunClubSponsorRepository extends JpaRepository<RunClubSponsor, Long> {
    List<RunClubSponsor> findAllByOrderBySortOrderAscIdAsc();
}
