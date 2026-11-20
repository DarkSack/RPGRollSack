package com.sack.rpgroll.magic.listener;

import com.sack.rpgroll.common.event.magic.SpellChannelEndEvent;
import com.sack.rpgroll.common.event.magic.SpellChannelEndEvent.Reason;
import com.sack.rpgroll.common.event.magic.SpellChannelStartEvent;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.magic.core.Spell;
import com.sack.rpgroll.magic.engine.CastResult;
import com.sack.rpgroll.magic.engine.SpellCastEngine;
import com.sack.rpgroll.magic.runtime.PlayerSpellbook;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpellChannelEventsTest {

    private MockedStatic<Bukkit> bukkit;
    private Runnable tick;
    /** Eventos lanzados y, entre ellos, el marcador "CAST" cuando se llama a engine.cast. */
    private final List<Object> fired = new ArrayList<>();
    private final Player player = mock(Player.class);
    private final World world = mock(World.class);
    private final World otherWorld = mock(World.class);
    private final Location origin = new Location(world, 0, 64, 0);
    private final SpellCastEngine engine = mock(SpellCastEngine.class);
    private SpellChannelManager manager;

    private final Spell spell = new Spell("rayo", "Rayo", null, null, "tormenta", null, 0, null, 3, 20, null, null, 0,
            Set.of(), null, List.of());

    @BeforeEach
    void setUp() {
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        BukkitTask task = mock(BukkitTask.class);
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        when(scheduler.runTaskTimer(any(Plugin.class), captor.capture(), anyLong(), anyLong())).thenReturn(task);
        bukkit = Mockito.mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);

        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isOnline()).thenReturn(true);
        when(player.getLocation()).thenAnswer(invocation -> origin.clone());
        when(player.getWorld()).thenReturn(world);
        when(engine.cast(any(), any(), any(), any())).thenAnswer(invocation -> {
            fired.add("CAST");
            return CastResult.ok();
        });

        manager = new SpellChannelManager(mock(Plugin.class), engine, mock(LangManager.class), fired::add) {
            @Override
            void drawChannel(Player p, Spell s, int elapsed, int totalTicks) {
                // Sound/Particle necesitan el registro de Paper, que no existe en los tests.
            }
        };
        manager.start(player, spell, new PlayerSpellbook(player.getUniqueId()), null);
        tick = captor.getValue();
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    private SpellChannelEndEvent end() {
        return assertInstanceOf(SpellChannelEndEvent.class, fired.get(1));
    }

    @Test
    void channelStartFiresStartEvent() {
        assertEquals(1, fired.size());
        SpellChannelStartEvent event = assertInstanceOf(SpellChannelStartEvent.class, fired.get(0));
        assertEquals("rayo", event.spellId());
        assertEquals("tormenta", event.schoolId());
        assertEquals(3, event.castTimeTicks());
        assertEquals(player, event.getPlayer());
    }

    @Test
    void channelMoveFiresEndMoved() {
        origin.add(5, 0, 0);
        tick.run();
        assertEquals(2, fired.size());
        assertEquals(Reason.MOVED, end().reason());
        assertEquals("rayo", end().spellId());
    }

    @Test
    void channelDamageFiresEndDamaged() {
        EntityDamageEvent damage = mock(EntityDamageEvent.class);
        when(damage.getEntity()).thenReturn(player);
        manager.onDamage(damage);
        assertEquals(2, fired.size());
        assertEquals(Reason.DAMAGED, end().reason());
    }

    @Test
    void channelCompleteFiresEndCompletedBeforeCast() {
        tick.run();
        tick.run();
        tick.run();
        assertEquals(3, fired.size());
        assertEquals(Reason.COMPLETED, end().reason());
        assertEquals("CAST", fired.get(2));
    }

    @Test
    void channelOfflineFiresEndOther() {
        when(player.isOnline()).thenReturn(false);
        tick.run();
        assertEquals(2, fired.size());
        assertEquals(Reason.OTHER, end().reason());
    }

    @Test
    void channelWorldChangeFiresEndOther() {
        when(player.getWorld()).thenReturn(otherWorld);
        when(player.getLocation()).thenAnswer(invocation -> new Location(otherWorld, 0, 64, 0));
        tick.run();
        assertEquals(2, fired.size());
        assertEquals(Reason.OTHER, end().reason());
    }

    @Test
    void channelEndsOnlyOnce() {
        origin.add(5, 0, 0);
        tick.run();
        EntityDamageEvent damage = mock(EntityDamageEvent.class);
        when(damage.getEntity()).thenReturn(player);
        manager.onDamage(damage);
        assertEquals(2, fired.size());
    }
}
