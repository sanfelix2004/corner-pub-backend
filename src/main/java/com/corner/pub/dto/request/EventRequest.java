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

    /** Fine ricorrenza; vuoto = per sempre. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataFine;

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
    public LocalDate getDataFine() { return dataFine; }
    public void setDataFine(LocalDate dataFine) { this.dataFine = dataFine; }
    public Boolean getCreaEventoNormale() { return creaEventoNormale; }
    public void setCreaEventoNormale(Boolean creaEventoNormale) { this.creaEventoNormale = creaEventoNormale; }
    public String getPeriodoNormale() { return periodoNormale; }
    public void setPeriodoNormale(String periodoNormale) { this.periodoNormale = periodoNormale; }
    public Integer getGiornoSettimanaNormale() { return giornoSettimanaNormale; }
    public void setGiornoSettimanaNormale(Integer giornoSettimanaNormale) { this.giornoSettimanaNormale = giornoSettimanaNormale; }
    public LocalDate getDataFineNormale() { return dataFineNormale; }
    public void setDataFineNormale(LocalDate dataFineNormale) { this.dataFineNormale = dataFineNormale; }
}
