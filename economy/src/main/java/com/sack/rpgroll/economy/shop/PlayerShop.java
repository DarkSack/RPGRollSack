package com.sack.rpgroll.economy.shop;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Una tienda de jugador — vive puramente como GUI, no requiere un cofre ni ubicación física en el mundo. */
public class PlayerShop {

    private final UUID id;
    private final UUID ownerId;
    private String name;
    private String currencyId;
    private final List<ShopListing> listings = new ArrayList<>();
    private boolean open = true;

    public PlayerShop(UUID id, UUID ownerId, String name, String currencyId) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.currencyId = currencyId;
    }

    public UUID id() {
        return id;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String currencyId() {
        return currencyId;
    }

    public void setCurrencyId(String currencyId) {
        this.currencyId = currencyId;
    }

    public List<ShopListing> listings() {
        return listings;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

}
