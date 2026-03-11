package com.corner.pub.model;

import com.corner.pub.model.enums.TableStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "table_sessions")
@Data
@NoArgsConstructor
public class TableSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tableNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TableStatus status = TableStatus.OPEN;

    @Column(columnDefinition = "TEXT")
    private String generalNotes;

    @Column(nullable = false)
    private LocalDateTime openedAt;

    private LocalDateTime closedAt;

    private String openedByUsername;

    @PrePersist
    protected void onCreate() {
        if (this.openedAt == null) {
            this.openedAt = LocalDateTime.now();
        }
    }
}
