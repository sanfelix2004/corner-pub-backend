package com.corner.pub.service;

import com.corner.pub.dto.CategoryDTO;
import com.corner.pub.dto.response.CategoryPublicResponse;
import com.corner.pub.model.Category;
import com.corner.pub.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final MenuItemService menuItemService;

    /** Fallback solo se nessuna categoria ha ancora uno sortOrder impostato. */
    private static final List<String> FALLBACK_ORDER = List.of(
            "panini", "bevande", "wrap", "sfizi", "dolci", "birre", "fritti", "starter",
            "insalat", "combo", "bombette", "polpette", "carne", "pinse", "vino", "toast"
    );

    public List<Category> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        boolean anySorted = categories.stream().anyMatch(c -> c.getSortOrder() != null);
        categories.sort(categoryComparator(anySorted));
        return categories;
    }

    public List<CategoryPublicResponse> getPublicCategories() {
        return getAllCategories().stream()
                .filter(c -> c.getActive() == null || Boolean.TRUE.equals(c.getActive()))
                .map(this::toPublic)
                .collect(Collectors.toList());
    }

    /** Mappa nome categoria → meta pubblica (per arricchire i piatti del menù). */
    public Map<String, CategoryPublicResponse> publicByName() {
        Map<String, CategoryPublicResponse> map = new HashMap<>();
        for (CategoryPublicResponse cat : getPublicCategories()) {
            if (cat.getName() != null) {
                map.put(cat.getName().toLowerCase(Locale.ITALY), cat);
            }
        }
        return map;
    }

    public Category addCategory(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Nome categoria obbligatorio");
        }
        if (categoryRepository.existsByName(trimmed)) {
            throw new IllegalArgumentException("La categoria esiste già: " + trimmed);
        }
        Category category = new Category(trimmed);
        category.setSortOrder(nextSortOrder());
        if (trimmed.toLowerCase(Locale.ITALY).contains("smash")) {
            category.setQuantityPricingEnabled(true);
            category.setExtraUnitPrice(2.0);
            category.setQuantityUnitLabel("smash");
            category.setMaxQuantity(3);
        }
        return categoryRepository.save(category);
    }

    @Transactional
    public Category updateCategory(Long id, CategoryDTO payload) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoria non trovata con ID: " + id));

        if (payload.getName() != null) {
            String newName = payload.getName().trim();
            if (newName.isEmpty()) {
                throw new IllegalArgumentException("Nome categoria obbligatorio");
            }
            if (!category.getName().equals(newName) && categoryRepository.existsByName(newName)) {
                throw new IllegalArgumentException("Una categoria con questo nome esiste già: " + newName);
            }
            category.setName(newName);
        }

        if (payload.getSortOrder() != null) {
            category.setSortOrder(payload.getSortOrder());
        }

        if (payload.getQuantityPricingEnabled() != null) {
            category.setQuantityPricingEnabled(payload.getQuantityPricingEnabled());
        }

        if (payload.getExtraUnitPrice() != null) {
            double price = payload.getExtraUnitPrice();
            if (price < 0) {
                throw new IllegalArgumentException("Il supplemento non può essere negativo");
            }
            category.setExtraUnitPrice(price);
        }

        if (payload.getQuantityUnitLabel() != null) {
            String label = payload.getQuantityUnitLabel().trim();
            category.setQuantityUnitLabel(label.isEmpty() ? null : label);
        }

        if (payload.getMaxQuantity() != null) {
            int max = payload.getMaxQuantity();
            if (max < 2 || max > 10) {
                throw new IllegalArgumentException("Le unità massime devono essere tra 2 e 10");
            }
            category.setMaxQuantity(max);
        }

        // Se abilitato senza prezzo, default 2€ (tipico smash)
        if (category.isQuantityPricingEnabled()
                && (category.getExtraUnitPrice() == null || category.getExtraUnitPrice() <= 0)) {
            category.setExtraUnitPrice(2.0);
        }
        if (category.isQuantityPricingEnabled() && category.getMaxQuantity() == null) {
            category.setMaxQuantity(3);
        }
        if (category.isQuantityPricingEnabled()
                && (category.getQuantityUnitLabel() == null || category.getQuantityUnitLabel().isBlank())
                && category.getName() != null
                && category.getName().toLowerCase(Locale.ITALY).contains("smash")) {
            category.setQuantityUnitLabel("smash");
        }

        return categoryRepository.save(category);
    }

    /** Compatibilità: rinomina sola (vecchie chiamate admin). */
    @Transactional
    public Category updateCategory(Long id, String newName) {
        CategoryDTO dto = new CategoryDTO();
        dto.setName(newName);
        return updateCategory(id, dto);
    }

    @Transactional
    public List<Category> reorder(List<Long> orderedIds) {
        if (orderedIds == null || orderedIds.isEmpty()) {
            return getAllCategories();
        }
        int order = 0;
        for (Long id : orderedIds) {
            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Categoria non trovata: " + id));
            category.setSortOrder(order++);
            categoryRepository.save(category);
        }
        // Categorie non presenti nella lista restano in coda
        List<Category> remaining = categoryRepository.findAll().stream()
                .filter(c -> !orderedIds.contains(c.getId()))
                .sorted(categoryComparator(true))
                .collect(Collectors.toList());
        for (Category category : remaining) {
            category.setSortOrder(order++);
            categoryRepository.save(category);
        }
        return getAllCategories();
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoria non trovata con ID: " + id));
        menuItemService.deleteMenuItemsByCategory(category.getName());
        categoryRepository.delete(category);
    }

    private int nextSortOrder() {
        return categoryRepository.findAll().stream()
                .map(Category::getSortOrder)
                .filter(o -> o != null)
                .max(Integer::compareTo)
                .orElse(-1) + 1;
    }

    private Comparator<Category> categoryComparator(boolean preferSortOrder) {
        return (c1, c2) -> {
            if (preferSortOrder) {
                int s1 = c1.getSortOrder() != null ? c1.getSortOrder() : 10_000;
                int s2 = c2.getSortOrder() != null ? c2.getSortOrder() : 10_000;
                if (s1 != s2) {
                    return Integer.compare(s1, s2);
                }
            } else {
                int index1 = fallbackRank(c1.getName());
                int index2 = fallbackRank(c2.getName());
                if (index1 != index2) {
                    return Integer.compare(index1, index2);
                }
            }
            String n1 = c1.getName() == null ? "" : c1.getName();
            String n2 = c2.getName() == null ? "" : c2.getName();
            return n1.compareToIgnoreCase(n2);
        };
    }

    private int fallbackRank(String name) {
        if (name == null) {
            return 999;
        }
        String value = name.toLowerCase(Locale.ITALY);
        for (int i = 0; i < FALLBACK_ORDER.size(); i++) {
            if (value.contains(FALLBACK_ORDER.get(i))) {
                return i;
            }
        }
        return 999;
    }

    private CategoryPublicResponse toPublic(Category category) {
        CategoryPublicResponse response = new CategoryPublicResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setSortOrder(category.getSortOrder());
        response.setQuantityPricingEnabled(category.isQuantityPricingEnabled());
        response.setExtraUnitPrice(category.getExtraUnitPrice());
        response.setQuantityUnitLabel(category.getQuantityUnitLabel());
        response.setMaxQuantity(category.getMaxQuantity());
        return response;
    }
}
