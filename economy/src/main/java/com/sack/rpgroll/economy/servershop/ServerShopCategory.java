package com.sack.rpgroll.economy.servershop;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;

/**
 * Una sección de la tienda del servidor ({@code server-shop/<id>.yml}).
 *
 * @param slot       dónde aparece en el menú principal (0-44)
 * @param permission permiso para entrar (o null: todos)
 * @param premium    se muestra como sección destacada
 * @param currency   moneda de sus precios (o null: la moneda por defecto)
 */
public record ServerShopCategory(String id, String displayName, String icon, int slot, List<String> description,
        String permission, boolean premium, String currency, List<ServerShopEntry> entries) implements RPGContent {

    public ServerShopCategory {
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        icon = icon == null || icon.isBlank() ? "CHEST" : icon;
        description = description == null ? List.of() : List.copyOf(description);
        permission = permission == null || permission.isBlank() ? null : permission.trim();
        currency = currency == null || currency.isBlank() ? null : currency.trim();
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

}
