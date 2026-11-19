package com.corner.pub.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "speciale")
public class Speciale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String titolo;

    @Column(length = 120)
    private String kicker;

    @Column(length = 2000)
    private String descrizione;

    @Column(name = "data_inizio", nullable = false)
    private LocalDate dataInizio;

    /** Null = durata illimitata (per sempre). */
    @Column(name = "data_fine")
    private LocalDate dataFine;

    @Column(length = 40)
    private String tema = "default";

    @Column(name = "cta_testo", length = 80)
    private String ctaTesto;

    @Column(name = "cta_categoria", length = 120)
    private String ctaCategoria;

    @Column(name = "cta_url", length = 512)
    private String ctaUrl;

    @Column(name = "immagine_url", length = 512)
    private String immagineUrl;

    @Column(name = "immagine_public_id", length = 255)
    private String immaginePublicId;

    @Column(name = "giorni_settimana", length = 32)
    private String giorniSettimana;

    @Column(name = "evento_id")
    private Long eventoId;

    /** Mostra il bottone "Prenotati ora" se c'è un evento collegato. */
    @Column(name = "mostra_prenotati", nullable = false, columnDefinition = "boolean not null default false")
    private boolean mostraPrenotati = false;

    @Column(nullable = false)
    private boolean attivo = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

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

    public String getKicker() {
        return kicker;
    }

    public void setKicker(String kicker) {
        this.kicker = kicker;
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

    public String getTema() {
        return tema;
    }

    public void setTema(String tema) {
        this.tema = tema;
    }

    public String getCtaTesto() {
        return ctaTesto;
    }

    public void setCtaTesto(String ctaTesto) {
        this.ctaTesto = ctaTesto;
    }

    public String getCtaCategoria() {
        return ctaCategoria;
    }

    public void setCtaCategoria(String ctaCategoria) {
        this.ctaCategoria = ctaCategoria;
    }

    public String getCtaUrl() {
        return ctaUrl;
    }

    public void setCtaUrl(String ctaUrl) {
        this.ctaUrl = ctaUrl;
    }

    public String getImmagineUrl() {
        return immagineUrl;
    }

    public void setImmagineUrl(String immagineUrl) {
        this.immagineUrl = immagineUrl;
    }

    public String getImmaginePublicId() {
        return immaginePublicId;
    }

    public void setImmaginePublicId(String immaginePublicId) {
        this.immaginePublicId = immaginePublicId;
    }

    public boolean isAttivo() {
        return attivo;
    }

    public void setAttivo(boolean attivo) {
        this.attivo = attivo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getGiorniSettimana() {
        return giorniSettimana;
    }

    public void setGiorniSettimana(String giorniSettimana) {
        this.giorniSettimana = giorniSettimana;
    }

    public Long getEventoId() {
        return eventoId;
    }

    public void setEventoId(Long eventoId) {
        this.eventoId = eventoId;
    }

    public boolean isMostraPrenotati() {
        return mostraPrenotati;
    }

    public void setMostraPrenotati(boolean mostraPrenotati) {
        this.mostraPrenotati = mostraPrenotati;
    }
}
