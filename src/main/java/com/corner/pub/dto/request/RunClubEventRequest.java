package com.corner.pub.dto.request;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class RunClubEventRequest {

    private String titolo;
    private String descrizione;
    private LocalDateTime data;
    private Integer postiTotali;
    private String luogo;
    private String distanza;
    private String ritmo;
    private Integer quotaEuro;
    private Boolean ricorrente;
    private Integer giornoSettimana;
    private LocalDate dataFine;
    /** Aggiorna testi e posti su tutte le date della stessa serie. */
    private Boolean applicaATuttaLaSerie;

    public String getTitolo() { return titolo; }
    public void setTitolo(String titolo) { this.titolo = titolo; }
    public String getDescrizione() { return descrizione; }
    public void setDescrizione(String descrizione) { this.descrizione = descrizione; }
    public LocalDateTime getData() { return data; }
    public void setData(LocalDateTime data) { this.data = data; }
    public Integer getPostiTotali() { return postiTotali; }
    public void setPostiTotali(Integer postiTotali) { this.postiTotali = postiTotali; }
    public String getLuogo() { return luogo; }
    public void setLuogo(String luogo) { this.luogo = luogo; }
    public String getDistanza() { return distanza; }
    public void setDistanza(String distanza) { this.distanza = distanza; }
    public String getRitmo() { return ritmo; }
    public void setRitmo(String ritmo) { this.ritmo = ritmo; }
    public Integer getQuotaEuro() { return quotaEuro; }
    public void setQuotaEuro(Integer quotaEuro) { this.quotaEuro = quotaEuro; }
    public Boolean getRicorrente() { return ricorrente; }
    public void setRicorrente(Boolean ricorrente) { this.ricorrente = ricorrente; }
    public Integer getGiornoSettimana() { return giornoSettimana; }
    public void setGiornoSettimana(Integer giornoSettimana) { this.giornoSettimana = giornoSettimana; }
    public LocalDate getDataFine() { return dataFine; }
    public void setDataFine(LocalDate dataFine) { this.dataFine = dataFine; }
    public Boolean getApplicaATuttaLaSerie() { return applicaATuttaLaSerie; }
    public void setApplicaATuttaLaSerie(Boolean applicaATuttaLaSerie) { this.applicaATuttaLaSerie = applicaATuttaLaSerie; }
}
