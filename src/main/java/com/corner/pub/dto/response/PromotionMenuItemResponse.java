package com.corner.pub.dto.response;

import com.corner.pub.dto.response.MenuItemResponse;

public class PromotionMenuItemResponse {

    private MenuItemResponse menuItem;
    private double scontoPercentuale;

    public MenuItemResponse getMenuItem() {
        return menuItem;
    }

    public void setMenuItem(MenuItemResponse menuItem) {
        this.menuItem = menuItem;
    }

    public double getScontoPercentuale() {
        return scontoPercentuale;
    }

    public void setScontoPercentuale(double scontoPercentuale) {
        this.scontoPercentuale = scontoPercentuale;
    }
}
