package com.corner.pub.dto.response;

import com.corner.pub.model.Event;
import com.corner.pub.model.EventRegistration;
import java.util.List;

public class EventResponse {

    private Long id;
    private String titolo;
    private String descrizione;
    private String data;
    private Integer postiTotali;
    private Long postiOccupati;
    private Long postiDisponibili;

    // ✅ Costruttore con totale partecipanti (long)
    public EventResponse(Event event, long totalePartecipanti) {
        this.id = event.getId();
        this.titolo = event.getTitolo();
        this.descrizione = event.getDescrizione();
        this.data = event.getData().toString();
        this.postiTotali = event.getPostiTotali();

        this.postiOccupati = totalePartecipanti;
        this.postiDisponibili = (event.getPostiTotali() != null)
                ? Math.max(0, event.getPostiTotali() - totalePartecipanti)
                : null;
    }

    // ✅ Costruttore con lista di registrazioni
    public EventResponse(Event event, List<EventRegistration> registrations) {
        this(event, registrations.stream()
                .mapToLong(EventRegistration::getPartecipanti)
                .sum());
    }

    public EventResponse() {}

    // Getter e Setter
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
}
