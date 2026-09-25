package com.sack.rpgroll.common.assets;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

/**
 * SackResourcePack es compileOnly en :common, así que no está en el
 * classpath de estos tests: es exactamente la situación de un servidor que
 * no lo tiene instalado.
 */
class ModuleAssetSyncTest {

    @Test
    void creatingTheSyncDoesNotRequireSackResourcePack() {
        // Antes el mapa de tipos vivía en un campo estático de esta clase y
        // referenciaba AssetsAPI: crear la instancia lanzaba
        // NoClassDefFoundError y tumbaba a Items, Mobs, Fishing, Ranching y
        // Workers al habilitarse.
        assertDoesNotThrow(() -> new ModuleAssetSync(mock(Plugin.class), "test"));
    }

}
