package com.corner.pub.dto.response;

import com.corner.pub.model.Event;
import com.corner.pub.model.EventRegistration;
import com.corner.pub.service.StorageService;
import java.util.List;

public class EventResponse {

    private Long id;
    private String titolo;
    private String posterUrl;
    private String descrizione;
    private String data;
    private Integer postiTotali;
    private Long postiOccupati;
    private Long postiDisponibili;
    private boolean speciale;
    private Long parentEventId;
    private boolean ricorrente;
    private Integer giornoSettimana;
    private String dataFine;
    private int figliCount;

    public EventResponse(Event event, long totalePartecipanti) {
        this.id = event.getId();
        this.titolo = event.getTitolo();
        String ownPoster = StorageService.toLocalUrl(event.getPosterUrl());
        if ((ownPoster == null || ownPoster.isBlank()) && event.getParentEvent() != null) {
            this.posterUrl = StorageService.toLocalUrl(event.getParentEvent().getPosterUrl());
        } else {
            this.posterUrl = ownPoster;
        }
        this.descrizione = event.getDescrizione();
        this.data = event.getData() != null ? event.getData().toString() : null;
        this.postiTotali = event.getPostiTotali();
        this.postiOccupati = totalePartecipanti;
        this.postiDisponibili = (event.getPostiTotali() != null)
                ? Math.max(0, event.getPostiTotali() - totalePartecipanti)
                : null;
        this.speciale = event.isSpeciale();
        this.parentEventId = event.getParentEvent() != null ? event.getParentEvent().getId() : null;
        this.ricorrente = event.isRicorrente();
        this.giornoSettimana = event.getGiornoSettimana();
        this.dataFine = event.getDataFine() != null ? event.getDataFine().toString() : null;
        this.figliCount = 0;
    }

    public EventResponse(Event event, List<EventRegistration> registrations) {
        this(event, registrations.stream()
                .mapToLong(EventRegistration::getPartecipanti)
                .sum());
    }

    public EventResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitolo() { return titolo; }
    public void setTitolo(String titolo) { this.titolo = titolo; }
    public String getDescrizione() { return descrizione; }
    public void setDescrizione(String descrizione) { this.descrizione = descrizione; }
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
    public Integer getPostiTotali() { return postiTotali; }
    public void setPostiTotali(Integer postiTotali) { this.postiTotali = postiTotali; }
    public Long getPostiOccupati() { return postiOccupati; }
    public void setPostiOccupati(Long postiOccupati) { this.postiOccupati = postiOccupati; }
    public Long getPostiDisponibili() { return postiDisponibili; }
    public void setPostiDisponibili(Long postiDisponibili) { this.postiDisponibili = postiDisponibili; }
    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
    public boolean isSpeciale() { return speciale; }
    public void setSpeciale(boolean speciale) { this.speciale = speciale; }
    public Long getParentEventId() { return parentEventId; }
    public void setParentEventId(Long parentEventId) { this.parentEventId = parentEventId; }
    public boolean isRicorrente() { return ricorrente; }
    public void setRicorrente(boolean ricorrente) { this.ricorrente = ricorrente; }
    public Integer getGiornoSettimana() { return giornoSettimana; }
    public void setGiornoSettimana(Integer giornoSettimana) { this.giornoSettimana = giornoSettimana; }
    public String getDataFine() { return dataFine; }
    public void setDataFine(String dataFine) { this.dataFine = dataFine; }
    public int getFigliCount() { return figliCount; }
    public void setFigliCount(int figliCount) { this.figliCount = figliCount; }
}
