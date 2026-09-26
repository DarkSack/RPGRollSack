package com.sack.rpgroll.economy.servershop;

import java.util.List;

/**
 * Una línea de la tienda del servidor: qué se entrega, en qué cantidad por
 * compra y a qué precio se compra y se vende.
 *
 * @param kind   qué tipo de cosa es
 * @param key    el material, el id del ítem/encantamiento o el tipo de poción
 * @param level  nivel del libro encantado (1 si no aplica)
 * @param form   para pociones: POTION, SPLASH_POTION o LINGERING_POTION
 * @param amount unidades que entrega cada compra (y que pide cada venta)
 * @param buy    precio de compra del lote; 0 = no se vende al jugador
 * @param sell   lo que se le paga al jugador por el lote; 0 = no se compra
 * @param market producto del mercado de Economy que fija el precio (o null)
 * @param name   nombre a mostrar en vez del del ítem (o null)
 * @param lore   líneas extra bajo el nombre
 */
public record ServerShopEntry(Kind kind, String key, int level, String form, int amount, double buy, double sell,
        String market, String name, List<String> lore) {

    public enum Kind {
        /** Ítem vanilla. Es lo único que el jugador puede vender. */
        MATERIAL,
        /** Ítem de RPGRoll-Items. */
        ITEM,
        /** Libro de un encantamiento de RPGRoll-Enchantments. */
        ENCHANT,
        /** Libro encantado vanilla. */
        BOOK,
        /** Poción vanilla. */
        POTION
    }

    public ServerShopEntry {
        level = Math.max(1, level);
        amount = Math.max(1, amount);
        buy = Math.max(0, buy);
        sell = Math.max(0, sell);
        market = market == null || market.isBlank() ? null : market.trim();
        name = name == null || name.isBlank() ? null : name;
        lore = lore == null ? List.of() : List.copyOf(lore);
    }

    /** Solo lo vanilla sin nada especial se puede vender: el resto no se distingue de forma segura. */
    public boolean sellable() {
        return kind == Kind.MATERIAL;
    }

}
