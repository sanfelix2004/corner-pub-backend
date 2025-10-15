package com.corner.pub.dto.request;

public class MenuItemRequest {
    private String categoria;
    private String titolo;
    private String descrizione;
    private double prezzo;
    private java.util.List<AllergenSelection> allergens;

    // Getters & Setters

    public java.util.List<AllergenSelection> getAllergens(){ return allergens; }
    public void setAllergens(java.util.List<AllergenSelection> allergens){ this.allergens = allergens; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getTitolo() { return titolo; }
    public void setTitolo(String titolo) { this.titolo = titolo; }

    public String getDescrizione() { return descrizione; }
    public void setDescrizione(String descrizione) { this.descrizione = descrizione; }

    public double getPrezzo() { return prezzo; }
    public void setPrezzo(double prezzo) { this.prezzo = prezzo; }
}
