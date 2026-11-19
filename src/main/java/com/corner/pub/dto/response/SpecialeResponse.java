package com.corner.pub.dto.response;

import com.corner.pub.model.Speciale;
import com.corner.pub.service.RecurrenceSupport;

import java.time.LocalDate;

public class SpecialeResponse {
    private Long id;
    private String titolo;
    private String kicker;
    private String descrizione;
    private LocalDate dataInizio;
    private LocalDate dataFine;
    private String tema;
    private String ctaTesto;
    private String ctaCategoria;
    private String ctaUrl;
    private String immagineUrl;
    private String giorniSettimana;
    private Long eventoId;
    private boolean mostraPrenotati;
    private String periodoLabel;
    private boolean attivo;

    public SpecialeResponse() {
    }

    public SpecialeResponse(Speciale s) {
        this.id = s.getId();
        this.titolo = s.getTitolo();
        this.kicker = s.getKicker();
        this.descrizione = s.getDescrizione();
        this.dataInizio = s.getDataInizio();
        this.dataFine = s.getDataFine();
        this.tema = s.getTema() != null ? s.getTema() : "default";
        this.ctaTesto = s.getCtaTesto();
        this.ctaCategoria = s.getCtaCategoria();
        this.ctaUrl = s.getCtaUrl();
        this.immagineUrl = s.getImmagineUrl();
        this.giorniSettimana = s.getGiorniSettimana();
        this.eventoId = s.getEventoId();
        this.mostraPrenotati = s.isMostraPrenotati() && s.getEventoId() != null;
        this.periodoLabel = RecurrenceSupport.label(
                s.getGiorniSettimana() != null && !s.getGiorniSettimana().isBlank(),
                s.getGiorniSettimana(),
                s.getDataInizio(),
                s.getDataFine());
        this.attivo = s.isAttivo();
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

    public boolean isAttivo() {
        return attivo;
    }

    public void setAttivo(boolean attivo) {
        this.attivo = attivo;
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

    public String getPeriodoLabel() {
        return periodoLabel;
    }

    public void setPeriodoLabel(String periodoLabel) {
        this.periodoLabel = periodoLabel;
    }
}
