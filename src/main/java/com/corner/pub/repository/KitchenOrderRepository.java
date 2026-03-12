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

    List<KitchenOrder> findByStatusNotIn(List<KitchenOrderStatus> statuses);

    List<KitchenOrder> findByStatusIn(List<KitchenOrderStatus> statuses);
}
