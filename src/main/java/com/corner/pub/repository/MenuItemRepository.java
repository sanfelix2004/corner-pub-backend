package com.corner.pub.repository;

import com.corner.pub.model.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("UPDATE MenuItem m SET m.categoria = :newName WHERE m.categoria = :oldName")
    void updateCategoryName(@org.springframework.data.repository.query.Param("oldName") String oldName,
            @org.springframework.data.repository.query.Param("newName") String newName);

    java.util.List<MenuItem> findByCategoria(String categoria);
}
