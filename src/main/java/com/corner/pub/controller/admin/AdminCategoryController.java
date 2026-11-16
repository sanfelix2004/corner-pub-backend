package com.corner.pub.controller.admin;

import com.corner.pub.dto.CategoryDTO;
import com.corner.pub.model.Category;
import com.corner.pub.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @PostMapping
    public ResponseEntity<?> addCategory(@RequestBody CategoryDTO payload) {
        try {
            String name = payload.getName();
            if (name == null || name.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Nome obbligatorio"));
            }
            return ResponseEntity.ok(categoryService.addCategory(name));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PutMapping("/reorder")
    public ResponseEntity<?> reorder(@RequestBody List<Long> orderedIds) {
        try {
            return ResponseEntity.ok(categoryService.reorder(orderedIds));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody CategoryDTO payload) {
        try {
            if (payload.getName() != null && payload.getName().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Nome obbligatorio"));
            }
            return ResponseEntity.ok(categoryService.updateCategory(id, payload));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
