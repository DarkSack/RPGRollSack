package com.sack.rpgroll.magic.engine;

import com.sack.rpgroll.common.event.magic.SpellCastEvent;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.magic.core.MagicSchool;
import com.sack.rpgroll.magic.core.RuneManager;
import com.sack.rpgroll.magic.core.SchoolManager;
import com.sack.rpgroll.magic.core.Spell;
import com.sack.rpgroll.magic.runtime.PlayerSpellbook;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpellCastEventFiringTest {

    private final List<Event> fired = new ArrayList<>();
    private final Player caster = mock(Player.class);
    private final SchoolManager schools = mock(SchoolManager.class);
    private final LangManager lang = mock(LangManager.class,
            invocation -> invocation.getMethod().getReturnType() == String.class ? "razón" : null);
    private final SpellCastEngine engine;

    private final Spell spell = new Spell("bola", "Bola", null, null, "fuego", null, 0, null, 0, 20, null, null, 0,
            Set.of("fuego", "area"), null, List.of());

    SpellCastEventFiringTest() {
        when(caster.getUniqueId()).thenReturn(UUID.randomUUID());
        when(schools.get("fuego")).thenReturn(Optional.of(
                new MagicSchool("fuego", "Fuego", null, null, null, null, null, null, null)));
        engine = new SpellCastEngine(mock(Plugin.class), schools, mock(RuneManager.class), lang, fired::add);
    }

    @Test
    void castFiresEventOnlyOnSuccess() {
        PlayerSpellbook book = new PlayerSpellbook(caster.getUniqueId());
        book.startCooldown("bola", 60_000, System.currentTimeMillis());

        CastResult blocked = engine.cast(spell, caster, book, null);

        assertFalse(blocked.success());
        assertTrue(fired.isEmpty(), "en enfriamiento no debe lanzarse ningún evento");

        PlayerSpellbook fresh = new PlayerSpellbook(caster.getUniqueId());
        CastResult ok = engine.cast(spell, caster, fresh, null);

        assertTrue(ok.success());
        assertEquals(1, fired.size());
        SpellCastEvent event = (SpellCastEvent) fired.get(0);
        assertEquals("bola", event.spellId());
        assertEquals("fuego", event.schoolId());
        assertEquals(Set.of("fuego", "area"), event.tags());
        assertEquals(caster, event.getPlayer());
        assertTrue(fresh.isOnCooldown("bola", System.currentTimeMillis()), "el evento va tras iniciar el enfriamiento");
    }
}
