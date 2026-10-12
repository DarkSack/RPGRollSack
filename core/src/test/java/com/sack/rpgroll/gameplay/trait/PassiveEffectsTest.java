package com.sack.rpgroll.gameplay.trait;

import com.sack.rpgroll.api.playerclass.ClassManager;
import com.sack.rpgroll.api.playerclass.PlayerClass;
import com.sack.rpgroll.api.race.Race;
import com.sack.rpgroll.api.race.RaceManager;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.player.traits.PlayerTraits;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PassiveEffectsTest {

    private static Trait trait(String id, int health, double damage, double defense) {
        return new Trait(id, id, "", 1, new TraitEffect(0, 0, 0, 0, 0, 0, health, 0, damage, defense, 0, 1, 0, 0, 0));
    }

    /** Suman el rasgo de la raza, el de la clase y los adquiridos; uno sin archivo no cuenta ni rompe. */
    @Test
    void addsRaceClassAndAcquiredTraits() {
        TraitManager traits = mock(TraitManager.class);
        when(traits.get(anyString())).thenReturn(Optional.empty());
        for (Trait trait : List.of(trait("furia_orca", 0, 0.05, -0.03), trait("erudicion_arcana", -4, 0.06, 0),
                trait("dragon_blood", 10, 0.05, 0), trait("perk_strength", 0, 0.05, 0))) {
            when(traits.get(trait.id())).thenReturn(Optional.of(trait));
        }

        Race race = mock(Race.class);
        when(race.passiveTraits()).thenReturn(List.of("furia_orca", "no_existe"));
        RaceManager races = mock(RaceManager.class);
        when(races.get("orco")).thenReturn(Optional.of(race));

        PlayerClass playerClass = mock(PlayerClass.class);
        when(playerClass.passiveTraits()).thenReturn(List.of("erudicion_arcana"));
        ClassManager classes = mock(ClassManager.class);
        when(classes.get("mago")).thenReturn(Optional.of(playerClass));

        RPGPlayer player = mock(RPGPlayer.class);
        when(player.getRace()).thenReturn("orco");
        when(player.getPlayerClass()).thenReturn("mago");
        when(player.getTraits()).thenReturn(new PlayerTraits(Set.of("dragon_blood")));
        // Fuerza 16 (+3, y su perk), percepción 12 (+1) y suerte 14 (+2).
        when(player.getStats()).thenReturn(new com.sack.rpgroll.player.stats.PlayerStats(16, 10, 10, 10, 12, 10, 14));

        Plugin plugin = mock(Plugin.class);
        when(plugin.namespace()).thenReturn("rpgroll");
        PassiveEffects effects = new PassiveEffects(plugin, mock(PlayerManager.class), traits, races, classes);

        PassiveEffects.Totals totals = effects.totals(player);

        assertEquals(6, totals.health());
        assertEquals(0.16 + 0.05 + 0.06, totals.damage(), 1e-9);
        assertEquals(-0.03, totals.defense(), 1e-9);
        // Cuatro rasgos con un punto de suerte cada uno, más dos del atributo.
        assertEquals(4 + 2, totals.luck(), 1e-9);
        assertEquals(0.06 + 0.01, totals.critical(), 1e-9);
        assertEquals(0.06, totals.evasion(), 1e-9);
    }


    /**
     * Un rasgo que sube un atributo cuenta como el atributo, cada escalón de perk es su propio archivo, y
     * la vida y el maná de más son los que {@code apply} suma a los máximos.
     */
    @Test
    void traitAttributesPerkStepsAndPools() {
        TraitManager traits = mock(TraitManager.class);
        when(traits.get(anyString())).thenReturn(Optional.empty());
        // Fuerza +4, resistencia +2, inteligencia +3 y 15 de maná.
        when(traits.get("sabio")).thenReturn(Optional.of(new Trait("sabio", "sabio", "", 1,
                new TraitEffect(4, 0, 2, 3, 0, 0, 0, 15, 0, 0, 0, 0, 0, 0, 0))));
        when(traits.get("perk_strength_20")).thenReturn(Optional.of(trait("perk_strength_20", 0, 0.10, 0)));
        when(traits.get("perk_wisdom_18")).thenReturn(Optional.of(trait("perk_wisdom_18", 0, 0.50, 0)));

        RPGPlayer player = mock(RPGPlayer.class);
        when(player.getTraits()).thenReturn(new PlayerTraits(Set.of("sabio")));
        // Fuerza 20 (su escalón de 20 sí), percepción 17 (el de 18 todavía no), inteligencia 11.
        when(player.getStats()).thenReturn(new com.sack.rpgroll.player.stats.PlayerStats(20, 10, 10, 11, 17, 10, 10));

        Plugin plugin = mock(Plugin.class);
        when(plugin.namespace()).thenReturn("rpgroll");
        PassiveEffects effects = new PassiveEffects(plugin, mock(PlayerManager.class), traits,
                mock(RaceManager.class), mock(ClassManager.class));

        // (20 + 4 - 10) / 2 = 7 de modificador, a un 2 % cada uno, más el 10 % del escalón.
        assertEquals(0.14 + 0.10, effects.totals(player).damage(), 1e-9);
        assertEquals(7, effects.modifier(player, com.sack.rpgroll.api.stats.StatType.STRENGTH));
        // Resistencia 12 = +1 de modificador = 5 de vida; inteligencia 14 = +2 (tenía 0) = 10 de maná, más 15.
        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[] {5, 25}, effects.pools(null, player));
    }

    /** El bono de un rasgo entra y sale de los máximos sin dejar rastro en lo que se guarda. */
    @Test
    void theBonusIsNeverWhatGetsSaved() {
        com.sack.rpgroll.gameplay.combat.CombatStats base = com.sack.rpgroll.gameplay.combat.CombatStats.empty();
        com.sack.rpgroll.gameplay.combat.CombatStats boosted = base.withBonus(5, 25).growMana(10).withMana(130);

        assertEquals(105, boosted.maxHealth());
        assertEquals(135, boosted.maxMana());
        assertEquals(110, boosted.withBonus(0, 0).maxMana());
        assertEquals(110, boosted.withBonus(0, 0).currentMana());
        assertEquals(100, boosted.withBonus(0, 0).maxHealth());
        assertEquals(135, boosted.refresh(3, 20).maxMana());
        assertEquals(25, boosted.refresh(3, 20).bonusMana());
        // Un rasgo que restara más vida de la que hay deja 1, y lo guardado sigue siendo lo de siempre.
        assertEquals(1, base.withBonus(-500, 0).maxHealth());
        assertEquals(100, base.withBonus(-500, 0).withBonus(0, 0).maxHealth());
    }
}
