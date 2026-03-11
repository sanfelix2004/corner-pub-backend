package com.corner.pub.service;

import com.corner.pub.dto.request.CreateTableSessionRequest;
import com.corner.pub.model.TableSession;
import com.corner.pub.model.enums.TableStatus;
import com.corner.pub.repository.TableSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TableSessionService {

    @Autowired
    private TableSessionRepository tableSessionRepository;

    @Transactional
    public TableSession createTableSession(CreateTableSessionRequest request, String username) {
        TableSession session = new TableSession();
        session.setTableNumber(request.getTableNumber());
        session.setGeneralNotes(request.getGeneralNotes());
        session.setOpenedByUsername(username);
        return tableSessionRepository.save(session);
    }

    public List<TableSession> getOpenTables() {
        return tableSessionRepository.findByStatusNot(TableStatus.CLOSED);
    }

    public TableSession getTableSessionById(Long id) {
        return tableSessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("TableSession not found with id " + id));
    }

    @Transactional
    public TableSession updateTableSession(Long id, String tableNumber, String notes, TableStatus status) {
        TableSession session = getTableSessionById(id);
        if (tableNumber != null)
            session.setTableNumber(tableNumber);
        if (notes != null)
            session.setGeneralNotes(notes);
        if (status != null)
            session.setStatus(status);
        return tableSessionRepository.save(session);
    }

    @Transactional
    public TableSession closeTableSession(Long id) {
        TableSession session = getTableSessionById(id);
        session.setStatus(TableStatus.CLOSED);
        session.setClosedAt(LocalDateTime.now());
        return tableSessionRepository.save(session);
    }
}
