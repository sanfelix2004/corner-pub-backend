package com.corner.pub.model;

import jakarta.persistence.*;

@Entity
@Table(name = "allergens")
public class Allergen {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 40)
    private String code;          // es. MILK

    @Column(nullable = false, length = 100)
    private String label;         // es. Latte e derivati

    @Column(name = "icon_base", nullable = false, length = 200)
    private String iconBase;      // es. corner/icons/allergens/MILK

    // getters/setters
    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getIconBase() { return iconBase; }
    public void setIconBase(String iconBase) { this.iconBase = iconBase; }
}