package com.corner.pub.dto.request;

public class MenuItemRequest {
    private String titolo;
    private String descrizione;
    private double prezzo;

    // Getters & Setters
    public String getTitolo() { return titolo; }
    public void setTitolo(String titolo) { this.titolo = titolo; }

    public String getDescrizione() { return descrizione; }
    public void setDescrizione(String descrizione) { this.descrizione = descrizione; }

    public double getPrezzo() { return prezzo; }
    public void setPrezzo(double prezzo) { this.prezzo = prezzo; }
}
