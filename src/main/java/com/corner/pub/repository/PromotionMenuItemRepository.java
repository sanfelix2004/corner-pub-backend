package com.corner.pub.repository;

import com.corner.pub.model.PromotionMenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionMenuItemRepository extends JpaRepository<PromotionMenuItem, Long> {}