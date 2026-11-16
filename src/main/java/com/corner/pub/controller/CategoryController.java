package com.corner.pub.controller;

import com.corner.pub.dto.response.CategoryPublicResponse;
import com.corner.pub.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /** Categorie ordinate come le vede il cliente, con eventuale prezzo multiplo. */
    @GetMapping
    public ResponseEntity<List<CategoryPublicResponse>> list() {
        return ResponseEntity.ok(categoryService.getPublicCategories());
    }
}
