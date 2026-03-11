package com.corner.pub.repository;

import com.corner.pub.model.TableSession;
import com.corner.pub.model.enums.TableStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TableSessionRepository extends JpaRepository<TableSession, Long> {
    List<TableSession> findByStatusNot(TableStatus status);
}
