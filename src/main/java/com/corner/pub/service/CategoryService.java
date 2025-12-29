package com.corner.pub.service;

import com.corner.pub.model.Category;
import com.corner.pub.repository.CategoryRepository;
import com.corner.pub.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuItemService menuItemService;

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category addCategory(String name) {
        if (categoryRepository.existsByName(name)) {
            throw new IllegalArgumentException("La categoria esiste già: " + name);
        }
        return categoryRepository.save(new Category(name));
    }

    @Transactional
    public Category updateCategory(Long id, String newName) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoria non trovata con ID: " + id));

        if (!category.getName().equals(newName)) {
            if (categoryRepository.existsByName(newName)) {
                throw new IllegalArgumentException("Una categoria con questo nome esiste già: " + newName);
            }
            // Aggiorna anche i menu items
            menuItemRepository.updateCategoryName(category.getName(), newName);

            category.setName(newName);
            return categoryRepository.save(category);
        }
        return category;
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoria non trovata con ID: " + id));

        // Elimina tutti i piatti di questa categoria (gestendo anche le immagini)
        menuItemService.deleteMenuItemsByCategory(category.getName());

        // Infine elimina la categoria
        categoryRepository.delete(category);
    }
}
