// src/main/java/com/corner/pub/dto/request/InEvidenzaRequest.java
package com.corner.pub.dto.request;

public class InEvidenzaRequest {
    private String categoria;
    private Long itemId;
    // getters & setters

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }
}
