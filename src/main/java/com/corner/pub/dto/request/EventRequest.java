package com.corner.pub.dto.request;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public class EventRequest {
    @NotBlank(message = "Il titolo è obbligatorio")
    @Size(max = 100, message = "Il titolo non può superare i 100 caratteri")
    private String titolo;

    @Size(max = 500, message = "La descrizione non può superare i 500 caratteri")
    private String descrizione;

    @Future(message = "La data deve essere nel futuro")
    private LocalDateTime data;

    @Min(value = 1, message = "I posti totali devono essere almeno 1")
    private Integer postiTotali;

    // Getters and Setters
    public String getTitolo() { return titolo; }
    public void setTitolo(String titolo) { this.titolo = titolo; }
    public String getDescrizione() { return descrizione; }
    public void setDescrizione(String descrizione) { this.descrizione = descrizione; }
    public LocalDateTime getData() { return data; }
    public void setData(LocalDateTime data) { this.data = data; }
    public Integer getPostiTotali() { return postiTotali; }
    public void setPostiTotali(Integer postiTotali) { this.postiTotali = postiTotali; }
}