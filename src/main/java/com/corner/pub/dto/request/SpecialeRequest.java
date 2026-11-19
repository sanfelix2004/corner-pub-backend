package com.corner.pub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public class SpecialeRequest {

    @NotBlank
    @Size(max = 120)
    private String titolo;

    @Size(max = 120)
    private String kicker;

    @Size(max = 2000)
    private String descrizione;

    @NotNull
    private LocalDate dataInizio;

    /** Null o assente = per sempre. */
    private LocalDate dataFine;

    @Size(max = 40)
    private String tema = "default";

    @Size(max = 80)
    private String ctaTesto;

    @Size(max = 120)
    private String ctaCategoria;

    @Size(max = 512)
    private String ctaUrl;

    private Boolean attivo = true;

    @Size(max = 32)
    private String giorniSettimana;

    private Boolean creaEventoSubordinato = false;

    private Boolean mostraPrenotati;

    /** STESSO = stesso periodo dello speciale; RICORRENTE = giorni dedicati. */
    @Size(max = 20)
    private String eventoPeriodoMode;

    @Size(max = 32)
    private String eventoGiorniSettimana;

    private LocalTime eventoOra;

    private Integer eventoPostiTotali;

    @Size(max = 100)
    private String eventoTitolo;

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

    public Boolean getAttivo() {
        return attivo;
    }

    public void setAttivo(Boolean attivo) {
        this.attivo = attivo;
    }

    public String getGiorniSettimana() {
        return giorniSettimana;
    }

    public void setGiorniSettimana(String giorniSettimana) {
        this.giorniSettimana = giorniSettimana;
    }

    public Boolean getCreaEventoSubordinato() {
        return creaEventoSubordinato;
    }

    public void setCreaEventoSubordinato(Boolean creaEventoSubordinato) {
        this.creaEventoSubordinato = creaEventoSubordinato;
    }

    public Boolean getMostraPrenotati() {
        return mostraPrenotati;
    }

    public void setMostraPrenotati(Boolean mostraPrenotati) {
        this.mostraPrenotati = mostraPrenotati;
    }

    public String getEventoPeriodoMode() {
        return eventoPeriodoMode;
    }

    public void setEventoPeriodoMode(String eventoPeriodoMode) {
        this.eventoPeriodoMode = eventoPeriodoMode;
    }

    public String getEventoGiorniSettimana() {
        return eventoGiorniSettimana;
    }

    public void setEventoGiorniSettimana(String eventoGiorniSettimana) {
        this.eventoGiorniSettimana = eventoGiorniSettimana;
    }

    public LocalTime getEventoOra() {
        return eventoOra;
    }

    public void setEventoOra(LocalTime eventoOra) {
        this.eventoOra = eventoOra;
    }

    public Integer getEventoPostiTotali() {
        return eventoPostiTotali;
    }

    public void setEventoPostiTotali(Integer eventoPostiTotali) {
        this.eventoPostiTotali = eventoPostiTotali;
    }

    public String getEventoTitolo() {
        return eventoTitolo;
    }

    public void setEventoTitolo(String eventoTitolo) {
        this.eventoTitolo = eventoTitolo;
    }
}
