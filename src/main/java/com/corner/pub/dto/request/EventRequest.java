package com.corner.pub.dto.request;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class EventRequest {
    @NotBlank(message = "Il titolo è obbligatorio")
    @Size(max = 100, message = "Il titolo non può superare i 100 caratteri")
    private String titolo;

    @Size(max = 500, message = "La descrizione non può superare i 500 caratteri")
    private String descrizione;

    /** Prima data/ora (o riferimento orario per la ricorrenza). */
    @NotNull(message = "La data è obbligatoria")
    private LocalDateTime data;

    @Min(value = 1, message = "I posti totali devono essere almeno 1")
    private Integer postiTotali;

    /** true = formula/locandina speciale. */
    private Boolean speciale;

    /** Ripeti ogni settimana. */
    private Boolean ricorrente;

    /** 1=Lunedì … 7=Domenica (ISO). */
    @Min(1)
    @Max(7)
    private Integer giornoSettimana;

    /** Giorni CSV (1–7), es. "5" o "5,6" — modello Speciali / ricorrenza virtuale. */
    @Size(max = 32)
    private String giorniSettimana;

    /** Fine ricorrenza; vuoto = per sempre. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataFine;

    /** Collegamento a uno speciale di periodo. */
    private Long specialeId;

    /** Se true, crea anche un evento normale subordinato allo speciale. */
    private Boolean creaEventoNormale;

    /**
     * Periodo dell'evento normale subordinato:
     * STESSO = stesso periodo della specialità;
     * RICORRENTE = ricorrenza dedicata (es. ogni venerdì).
     */
    private String periodoNormale;

    @Min(1)
    @Max(7)
    private Integer giornoSettimanaNormale;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataFineNormale;

    /** true = uscita Corner Run Club. */
    private Boolean runClub;

    @Size(max = 180)
    private String luogo;

    @Size(max = 40)
    private String distanza;

    @Size(max = 40)
    private String ritmo;

    @Min(0)
    private Integer quotaEuro;

    public String getTitolo() { return titolo; }
    public void setTitolo(String titolo) { this.titolo = titolo; }
    public String getDescrizione() { return descrizione; }
    public void setDescrizione(String descrizione) { this.descrizione = descrizione; }
    public LocalDateTime getData() { return data; }
    public void setData(LocalDateTime data) { this.data = data; }
    public Integer getPostiTotali() { return postiTotali; }
    public void setPostiTotali(Integer postiTotali) { this.postiTotali = postiTotali; }
    public Boolean getSpeciale() { return speciale; }
    public void setSpeciale(Boolean speciale) { this.speciale = speciale; }
    public Boolean getRicorrente() { return ricorrente; }
    public void setRicorrente(Boolean ricorrente) { this.ricorrente = ricorrente; }
    public Integer getGiornoSettimana() { return giornoSettimana; }
    public void setGiornoSettimana(Integer giornoSettimana) { this.giornoSettimana = giornoSettimana; }
    public String getGiorniSettimana() { return giorniSettimana; }
    public void setGiorniSettimana(String giorniSettimana) { this.giorniSettimana = giorniSettimana; }
    public LocalDate getDataFine() { return dataFine; }
    public void setDataFine(LocalDate dataFine) { this.dataFine = dataFine; }
    public Long getSpecialeId() { return specialeId; }
    public void setSpecialeId(Long specialeId) { this.specialeId = specialeId; }
    public Boolean getCreaEventoNormale() { return creaEventoNormale; }
    public void setCreaEventoNormale(Boolean creaEventoNormale) { this.creaEventoNormale = creaEventoNormale; }
    public String getPeriodoNormale() { return periodoNormale; }
    public void setPeriodoNormale(String periodoNormale) { this.periodoNormale = periodoNormale; }
    public Integer getGiornoSettimanaNormale() { return giornoSettimanaNormale; }
    public void setGiornoSettimanaNormale(Integer giornoSettimanaNormale) { this.giornoSettimanaNormale = giornoSettimanaNormale; }
    public LocalDate getDataFineNormale() { return dataFineNormale; }
    public void setDataFineNormale(LocalDate dataFineNormale) { this.dataFineNormale = dataFineNormale; }
    public Boolean getRunClub() { return runClub; }
    public void setRunClub(Boolean runClub) { this.runClub = runClub; }
    public String getLuogo() { return luogo; }
    public void setLuogo(String luogo) { this.luogo = luogo; }
    public String getDistanza() { return distanza; }
    public void setDistanza(String distanza) { this.distanza = distanza; }
    public String getRitmo() { return ritmo; }
    public void setRitmo(String ritmo) { this.ritmo = ritmo; }
    public Integer getQuotaEuro() { return quotaEuro; }
    public void setQuotaEuro(Integer quotaEuro) { this.quotaEuro = quotaEuro; }
}
