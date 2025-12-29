package com.corner.pub.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
public class PromotionItemDetail {
    private Long itemId;
    private String nome;
    private String imageUrl;
    private Double prezzoOriginale; // lasciamo Double per non rompere il FE
    private Double scontoPercentuale; // idem
    private Double prezzoScontato; // idem
    private String categoryName;

    public PromotionItemDetail(MenuItemResponse menuItem, Double scontoPercentuale) {
        this.itemId = menuItem.getId();
        this.nome = menuItem.getTitolo();
        this.imageUrl = menuItem.getImageUrl();
        this.prezzoOriginale = menuItem.getPrezzo(); // può essere null
        this.scontoPercentuale = scontoPercentuale == null ? 0.0 : scontoPercentuale;
        this.categoryName = menuItem.getCategoryName();

        // Calcolo sicuro del prezzo scontato
        if (this.prezzoOriginale != null) {
            BigDecimal base = BigDecimal.valueOf(this.prezzoOriginale);
            BigDecimal sconto = BigDecimal.valueOf(this.scontoPercentuale)
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal fattore = BigDecimal.ONE.subtract(sconto);
            BigDecimal scontato = base.multiply(fattore).setScale(2, RoundingMode.HALF_UP);
            this.prezzoScontato = scontato.doubleValue();
        } else {
            // niente prezzo originale ⇒ niente scontato
            this.prezzoScontato = null;
        }
    }
}
