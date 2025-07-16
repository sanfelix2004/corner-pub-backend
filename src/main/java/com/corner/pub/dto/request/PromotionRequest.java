package com.corner.pub.dto.request;

import java.time.LocalDate;
import java.util.List;

public class PromotionRequest {
    private String nome;
    private boolean attiva;
    private String descrizione;
    private LocalDate dataInizio;
    private LocalDate dataFine;

    private List<PromotionMenuItemRequest> items;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public boolean isAttiva() {
        return attiva;
    }

    public void setAttiva(boolean attiva) {
        this.attiva = attiva;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public LocalDate getDataInizio() {
        return dataInizio;
    }

    public void setDataInizio(LocalDate dataInizio) {
        this.dataInizio = dataInizio;
    }

    public LocalDate getDataFine() {
        return dataFine;
    }

    public void setDataFine(LocalDate dataFine) {
        this.dataFine = dataFine;
    }

    public List<PromotionMenuItemRequest> getItems() {
        return items;
    }

    public void setItems(List<PromotionMenuItemRequest> items) {
        this.items = items;
    }
}
