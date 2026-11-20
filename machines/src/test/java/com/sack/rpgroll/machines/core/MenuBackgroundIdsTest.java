package com.sack.rpgroll.machines.core;

import com.sack.rpgroll.machines.furnace.FurnaceMenu;
import com.sack.rpgroll.machines.spawner.SpawnerMenu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Los ids de fondo tienen que ser los de infra/maquinas/menus_jugador.py (sackito-network). */
class MenuBackgroundIdsTest {

    @Test
    void idsMatchPack() {
        assertEquals("horno", FurnaceMenu.BACKGROUND);
        assertEquals("generador", SpawnerMenu.BACKGROUND);
        assertEquals("machines", MachineMenus.MODULE);
    }
}
