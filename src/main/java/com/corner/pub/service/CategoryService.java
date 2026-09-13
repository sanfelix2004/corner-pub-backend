package com.corner.pub.service;

import com.corner.pub.model.Category;
import com.corner.pub.repository.CategoryRepository;
// import com.corner.pub.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    // private final MenuItemRepository menuItemRepository; // removed unused
    private final MenuItemService menuItemService;

    private static final List<String> PUBLIC_CATEGORY_ORDER = List.of(
            "panini",
            "bevande",
            "wrap",
            "sfizi",
            "dolci",
            "birre",
            "fritti",
            "starter",
            "insalat",
            "combo",
            "bombette",
            "polpette",
            "carne",
            "pinse",
            "vino",
            "toast"
    );

    public List<Category> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();

        categories.sort((c1, c2) -> {
            int index1 = categoryRank(c1.getName());
            int index2 = categoryRank(c2.getName());
            if (index1 != index2) {
                return Integer.compare(index1, index2);
            }

            Integer s1 = c1.getSortOrder() != null ? c1.getSortOrder() : 999;
            Integer s2 = c2.getSortOrder() != null ? c2.getSortOrder() : 999;
            if (!s1.equals(s2)) {
                return s1.compareTo(s2);
            }

            return c1.getName().compareToIgnoreCase(c2.getName());
        });

        return categories;
    }

    private int categoryRank(String name) {
        if (name == null) {
            return 999;
        }
        String value = name.toLowerCase();
        for (int i = 0; i < PUBLIC_CATEGORY_ORDER.size(); i++) {
            if (value.contains(PUBLIC_CATEGORY_ORDER.get(i))) {
                return i;
            }
        }
        return 999;
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
            // menuItemRepository.updateCategoryName(category.getName(), newName);
            // Non serve più con la relazione @ManyToOne, basta rinominare la categoria!

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
