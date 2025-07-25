package com.corner.pub.dto.response;

import lombok.Data;

@Data
public class PromotionItemDetail {
    private Long itemId;
    private String nome;
    private String imageUrl;
    private Double prezzoOriginale;
    private Double scontoPercentuale;
    private Double prezzoScontato;
    private String categoria;

    public PromotionItemDetail(MenuItemResponse menuItem, Double scontoPercentuale) {
        this.itemId = menuItem.getId();
        this.nome = menuItem.getTitolo();
        this.imageUrl = menuItem.getImageUrl();
        this.prezzoOriginale = menuItem.getPrezzo();
        this.scontoPercentuale = scontoPercentuale;
        this.prezzoScontato = menuItem.getPrezzo() * (1 - scontoPercentuale/100);
        this.categoria = menuItem.getCategoria();
    }

}