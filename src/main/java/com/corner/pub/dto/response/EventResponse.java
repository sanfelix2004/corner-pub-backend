package com.corner.pub.dto.response;

import com.corner.pub.model.Event;

public class EventResponse {

    private Long id;
    private String titolo;
    private String descrizione;
    private String data;
    private Integer postiTotali;
    private Long postiOccupati;
    private Long postiDisponibili;

    public EventResponse(Event event, long iscritti) {
        this.id = event.getId();
        this.titolo = event.getTitolo();
        this.descrizione = event.getDescrizione();
        this.data = event.getData().toString();
        this.postiTotali = event.getPostiTotali();
        this.postiOccupati = iscritti;
        this.postiDisponibili = (event.getPostiTotali() != null)
                ? Math.max(0, event.getPostiTotali() - iscritti)
                : null;
    }

    public EventResponse() {

    }

    public EventResponse(EventResponse event, long iscritti) {
        this.id = event.getId();
        this.titolo = event.getTitolo();
    }

    public EventResponse(Event event) {
        this.id = event.getId();
    }

    public static EventResponse from(Event event, long iscritti) {
        EventResponse response = new EventResponse();
        response.id = event.getId();
        response.titolo = event.getTitolo();
        response.descrizione = event.getDescrizione();
        response.data = event.getData().toString(); // puoi formattarla meglio se vuoi
        response.postiTotali = event.getPostiTotali();

        response.postiOccupati = iscritti;
        response.postiDisponibili = (event.getPostiTotali() != null)
                ? Math.max(0, event.getPostiTotali() - iscritti)
                : null;

        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitolo() {
        return titolo;
    }

    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Integer getPostiTotali() {
        return postiTotali;
    }

    public void setPostiTotali(Integer postiTotali) {
        this.postiTotali = postiTotali;
    }

    public Long getPostiOccupati() {
        return postiOccupati;
    }

    public void setPostiOccupati(Long postiOccupati) {
        this.postiOccupati = postiOccupati;
    }

    public Long getPostiDisponibili() {
        return postiDisponibili;
    }

    public void setPostiDisponibili(Long postiDisponibili) {
        this.postiDisponibili = postiDisponibili;
    }
}
