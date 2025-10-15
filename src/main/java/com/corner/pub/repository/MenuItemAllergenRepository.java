package com.corner.pub.repository;

import com.corner.pub.model.MenuItemAllergen;
import com.corner.pub.model.MenuItemAllergen.Key;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemAllergenRepository extends JpaRepository<MenuItemAllergen, Key> {
    List<MenuItemAllergen> findByMenuItem_Id(Long menuItemId);
    void deleteByMenuItem_Id(Long menuItemId);
}