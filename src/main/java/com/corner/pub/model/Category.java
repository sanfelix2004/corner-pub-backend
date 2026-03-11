package com.corner.pub.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(columnDefinition = "boolean default true")
    private Boolean active = true;

    public Category(String name) {
        this.name = name;
    }
}
