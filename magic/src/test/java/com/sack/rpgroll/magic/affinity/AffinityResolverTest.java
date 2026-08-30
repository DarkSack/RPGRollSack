package com.sack.rpgroll.magic.affinity;

import com.sack.rpgroll.magic.core.MagicSchool;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AffinityResolverTest {

    /**
     * RPGRollAPI vive en :core y nunca se inicializa (RPGRollAPI.init) en el
     * classpath de estos tests de :magic — isReady() se queda en false para
     * siempre, así que multiplierFor debería devolver siempre el neutral 1.0
     * sin importar el caster/escuela, exactamente el comportamiento
     * documentado para casters sin personaje/consola/mobs.
     */
    @Test
    void multiplierIsNeutralWhenRpgRollApiIsNotReady() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        MagicSchool school = new MagicSchool("fire", "Fire", null, null, null, null, null,
                java.util.Map.of("elf", 0.25), java.util.Map.of("mage", 0.3));

        assertEquals(1.0, AffinityResolver.multiplierFor(player, school));
    }
}
