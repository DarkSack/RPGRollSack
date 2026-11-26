package com.sack.rpgroll.structures.protect;

import com.sack.rpgroll.structures.integration.Hook;
import com.sack.rpgroll.structures.integration.Integrations;
import com.sack.rpgroll.structures.storage.Box;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class AreaGuardsTest {

    static final UUID OWNER = UUID.randomUUID();
    static final Box BOX = new Box(0, 64, 0, 9, 70, 9);

    /** Registro con solo los plugins de {@code present} instalados. */
    static Integrations with(Set<String> present) {
        return Integrations.load(null, present::contains, Map.of(), Logger.getAnonymousLogger());
    }

    static AreaGuard denies(Hook h, String key, AtomicInteger calls) {
        return (owner, world, box) -> {
            calls.incrementAndGet();
            return Optional.of(new AreaGuard.Denial(h, key));
        };
    }

    @Test void firstDenialWins() {
        AtomicInteger first = new AtomicInteger(), second = new AtomicInteger();
        AreaGuard g = AreaGuards.of(List.of(AreaGuard.allowAll(),
                denies(Hook.WORLDGUARD, "protect.worldguard", first),
                denies(Hook.GRIEFPREVENTION, "protect.griefprevention", second)));
        assertEquals(Optional.of(new AreaGuard.Denial(Hook.WORLDGUARD, "protect.worldguard")),
                g.check(OWNER, "w", BOX));
        assertEquals(1, first.get());
        assertEquals(0, second.get(), "tras la primera negativa no se pregunta a más");
    }

    @Test void allowAllAllows() {
        assertTrue(AreaGuard.allowAll().check(OWNER, "w", BOX).isEmpty());
        assertTrue(AreaGuards.of(List.of()).check(OWNER, "w", BOX).isEmpty());
    }

    @Test void onlyActiveHooksAreUsed() {
        AtomicInteger wg = new AtomicInteger(), gp = new AtomicInteger();
        Integrations in = with(Set.of("GriefPrevention")); // WorldGuard no está
        AreaGuard g = AreaGuards.fromIntegrations(() -> in, Map.of(
                Hook.WORLDGUARD, denies(Hook.WORLDGUARD, "protect.worldguard", wg),
                Hook.GRIEFPREVENTION, (o, w, b) -> {
                    gp.incrementAndGet();
                    return Optional.empty();
                }));
        assertTrue(g.check(OWNER, "w", BOX).isEmpty());
        assertEquals(0, wg.get(), "WorldGuard inactivo: su guard no se llama");
        assertEquals(1, gp.get());
    }

    @Test void readsTheCurrentIntegrationsOnEveryCheck() {
        AtomicInteger wg = new AtomicInteger();
        Integrations[] current = { with(Set.of()) };
        AreaGuard g = AreaGuards.fromIntegrations(() -> current[0],
                Map.of(Hook.WORLDGUARD, denies(Hook.WORLDGUARD, "protect.worldguard", wg)));
        assertTrue(g.check(OWNER, "w", BOX).isEmpty());
        current[0] = with(Set.of("WorldGuard")); // recarga: ahora sí está
        assertTrue(g.check(OWNER, "w", BOX).isPresent());
        assertEquals(1, wg.get());
    }

    @Test void guardThatThrowsDeniesAndMarksBroken() {
        Integrations in = with(Set.of("WorldGuard"));
        AreaGuard g = AreaGuards.fromIntegrations(() -> in, Map.of(Hook.WORLDGUARD, (o, w, b) -> {
            throw new IllegalStateException("reflexión rota");
        }));
        assertEquals(Optional.of(new AreaGuard.Denial(Hook.WORLDGUARD, "protect.error")), g.check(OWNER, "w", BOX),
                "falla cerrado: no se construye en lo dudoso");
        assertFalse(in.active(Hook.WORLDGUARD), "markBroken apaga la integración");
        assertEquals(Integrations.REASON_BROKEN, in.report().stream().filter(s -> s.hook() == Hook.WORLDGUARD)
                .findFirst().orElseThrow().reasonKey());
    }

    @Test void brokenHookKeepsDenyingWithoutAskingTheAdapter() {
        AtomicInteger calls = new AtomicInteger();
        Integrations in = with(Set.of("WorldGuard"));
        AreaGuard g = AreaGuards.fromIntegrations(() -> in, Map.of(Hook.WORLDGUARD, (o, w, b) -> {
            calls.incrementAndGet();
            throw new IllegalStateException("reflexión rota");
        }));
        Optional<AreaGuard.Denial> error = Optional.of(new AreaGuard.Denial(Hook.WORLDGUARD, "protect.error"));
        assertEquals(error, g.check(OWNER, "w", BOX));
        assertEquals(error, g.check(OWNER, "w", BOX), "sigue cerrado tras markBroken, hasta recargar");
        assertEquals(1, calls.get(), "roto: no se vuelve a llamar al adaptador");
    }

    @Test void hookBrokenAtLoadDeniesWithoutCallingTheAdapter() {
        AtomicInteger calls = new AtomicInteger();
        Integrations in = Integrations.load(null, Set.of("GriefPrevention")::contains,
                Map.of(Hook.GRIEFPREVENTION, () -> {
                    throw new IllegalStateException("x");
                }), Logger.getAnonymousLogger());
        AreaGuard g = AreaGuards.fromIntegrations(() -> in, Map.of(Hook.GRIEFPREVENTION, (o, w, b) -> {
            calls.incrementAndGet();
            return Optional.empty();
        }));
        assertEquals(Optional.of(new AreaGuard.Denial(Hook.GRIEFPREVENTION, "protect.error")),
                g.check(OWNER, "w", BOX));
        assertEquals(0, calls.get());
    }
}
