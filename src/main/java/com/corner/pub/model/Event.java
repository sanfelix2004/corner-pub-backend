package com.corner.pub.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "event")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titolo;
    private String descrizione;

    @Column(name = "data", nullable = false)
    private LocalDateTime data;

    @Column(name = "posti_totali")
    private Integer postiTotali;

    @Column(name = "poster_url", length = 512)
    private String posterUrl;

    @Column(name = "poster_public_id", length = 255)
    private String posterPublicId;

    /** Formula / locandina speciale (es. All You Can Eat). */
    @Column(name = "speciale", nullable = false)
    private boolean speciale = false;

    /** Evento normale subordinato a uno speciale. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_event_id")
    private Event parentEvent;

    @OneToMany(mappedBy = "parentEvent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Event> childEvents = new ArrayList<>();

    /** Se true, si ripete ogni settimana nel giorno indicato. */
    @Column(name = "ricorrente", nullable = false)
    private boolean ricorrente = false;

    /** ISO-8601: 1=Lunedì … 7=Domenica. */
    @Column(name = "giorno_settimana")
    private Integer giornoSettimana;

    /** Fine ricorrenza; null = per sempre. */
    @Column(name = "data_fine")
    private LocalDate dataFine;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventRegistration> registrations;

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

    public LocalDateTime getData() {
        return data;
    }

    public void setData(LocalDateTime data) {
        this.data = data;
    }

    public Integer getPostiTotali() {
        return postiTotali;
    }

    public void setPostiTotali(Integer postiTotali) {
        this.postiTotali = postiTotali;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<EventRegistration> getRegistrations() {
        return registrations;
    }

    public void setRegistrations(List<EventRegistration> registrations) {
        this.registrations = registrations;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getPosterPublicId() {
        return posterPublicId;
    }

    public void setPosterPublicId(String posterPublicId) {
        this.posterPublicId = posterPublicId;
    }

    public boolean isSpeciale() {
        return speciale;
    }

    public void setSpeciale(boolean speciale) {
        this.speciale = speciale;
    }

    public Event getParentEvent() {
        return parentEvent;
    }

    public void setParentEvent(Event parentEvent) {
        this.parentEvent = parentEvent;
    }

    public List<Event> getChildEvents() {
        return childEvents;
    }

    public void setChildEvents(List<Event> childEvents) {
        this.childEvents = childEvents;
    }

    public boolean isRicorrente() {
        return ricorrente;
    }

    public void setRicorrente(boolean ricorrente) {
        this.ricorrente = ricorrente;
    }

    public Integer getGiornoSettimana() {
        return giornoSettimana;
    }

    public void setGiornoSettimana(Integer giornoSettimana) {
        this.giornoSettimana = giornoSettimana;
    }

    public LocalDate getDataFine() {
        return dataFine;
    }

    public void setDataFine(LocalDate dataFine) {
        this.dataFine = dataFine;
    }
}
