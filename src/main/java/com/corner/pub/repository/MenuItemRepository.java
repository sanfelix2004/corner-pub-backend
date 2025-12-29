package com.corner.pub.repository;

import com.corner.pub.model.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    java.util.List<MenuItem> findByCategory_Name(String categoryName);
}
