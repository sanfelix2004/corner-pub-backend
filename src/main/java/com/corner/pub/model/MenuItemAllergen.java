package com.corner.pub.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "menu_item_allergens")
public class MenuItemAllergen {

    @EmbeddedId
    private Key id = new Key();

    @ManyToOne(fetch = FetchType.LAZY) @MapsId("menuItemId")
    @JoinColumn(name = "menu_item_id")
    private MenuItem menuItem;

    @ManyToOne(fetch = FetchType.LAZY) @MapsId("allergenId")
    @JoinColumn(name = "allergen_id")
    private Allergen allergen;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AllergenStatus status = AllergenStatus.CONTAINS;

    public MenuItemAllergen() {}
    public MenuItemAllergen(MenuItem item, Allergen allergen, AllergenStatus status) {
        this.menuItem = item; this.allergen = allergen; this.status = status;
        this.id = new Key(item.getId(), allergen.getId());
    }

    // getters/setters
    public MenuItem getMenuItem() { return menuItem; }
    public Allergen getAllergen() { return allergen; }
    public AllergenStatus getStatus() { return status; }
    public void setStatus(AllergenStatus status) { this.status = status; }

    @Embeddable
    public static class Key implements Serializable {
        private Long menuItemId;
        private Long allergenId;
        public Key() {}
        public Key(Long menuItemId, Long allergenId) { this.menuItemId = menuItemId; this.allergenId = allergenId; }
        @Override public boolean equals(Object o){ if(this==o) return true; if(!(o instanceof Key k)) return false; return Objects.equals(menuItemId,k.menuItemId)&&Objects.equals(allergenId,k.allergenId); }
        @Override public int hashCode(){ return Objects.hash(menuItemId, allergenId); }
    }
}