package com.sack.rpgroll.economy.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Los ids de fondo tienen que ser los de infra/maquinas/menus_jugador.py (sackito-network). */
class MenuBackgroundIdsTest {

    @Test
    void idsMatchPack() {
        assertEquals("tienda", ServerShopGUI.BACKGROUND);
        assertEquals("tienda_categoria", ServerShopCategoryGUI.BACKGROUND);
        assertEquals("tiendas_lista", ShopListGUI.BACKGROUND);
        assertEquals("tienda_jugador", ShopViewGUI.BACKGROUND);
        assertEquals("tienda_gestion", ShopManageGUI.BACKGROUND);
        assertEquals("subastas", AuctionHouseGUI.BACKGROUND);
        assertEquals("subastas_mias", AuctionMineGUI.BACKGROUND);
        assertEquals("subastas_cobrar", AuctionCollectGUI.BACKGROUND);
        assertEquals("subastas_confirmar", AuctionConfirmGUI.BACKGROUND);
        assertEquals("banco", BankGUI.BACKGROUND);
        assertEquals("banco_cuenta", BankAccountDetailGUI.BACKGROUND);
        assertEquals("empresas", CompanyListGUI.BACKGROUND);
        assertEquals("empresa", CompanyDetailGUI.BACKGROUND);
        assertEquals("economy", EconomyMenus.MODULE);
    }
}
