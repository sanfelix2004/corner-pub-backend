package com.corner.pub.repository;

import com.corner.pub.model.KitchenOrder;
import com.corner.pub.model.enums.KitchenOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KitchenOrderRepository extends JpaRepository<KitchenOrder, Long> {
    List<KitchenOrder> findByStatusNot(KitchenOrderStatus status);

    Optional<KitchenOrder> findByTableSessionIdAndStatusNot(Long tableSessionId, KitchenOrderStatus status);

    List<KitchenOrder> findByTableSessionId(Long tableSessionId);

    /** Cerca una comanda con uno status specifico per un dato tavolo (es. DRAFT) */
    Optional<KitchenOrder> findByTableSessionIdAndStatus(Long tableSessionId, KitchenOrderStatus status);

    /**
     * Query nativa per velocizzare storico cucina escludendo caricamento in RAM
     * intero db
     */
    List<KitchenOrder> findByStatusAndUpdatedAtAfterOrderByUpdatedAtDesc(KitchenOrderStatus status,
            java.time.LocalDateTime date);

    /**
     * Query nativa per dashboard attiva ignorando archivio e draft, ordinate per
     * creazione
     */
    List<KitchenOrder> findByStatusNotInOrderByCreatedAtAsc(List<KitchenOrderStatus> statuses);
}
