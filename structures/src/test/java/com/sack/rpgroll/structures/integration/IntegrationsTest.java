package com.sack.rpgroll.structures.integration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationsTest {

    private static Integrations load(String yaml, boolean present, Map<Hook, Supplier<String>> probes, Logger log) {
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.loadFromString(yaml);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return Integrations.load(y, name -> present, probes, log);
    }

    private static Integrations.Status of(Integrations in, Hook h) {
        return in.report().stream().filter(s -> s.hook() == h).findFirst().orElseThrow();
    }

    @Test void disabledInYamlIsInactiveWithReason() {
        Integrations in = load("vault:\n  enabled: false\n", true, Map.of(), Logger.getAnonymousLogger());
        assertFalse(in.active(Hook.VAULT));
        assertEquals("integrations.reason.disabled", of(in, Hook.VAULT).reasonKey());
        assertTrue(in.active(Hook.WORLDGUARD));
    }

    @Test void missingPluginIsInactive() {
        Integrations in = load("", false, Map.of(), Logger.getAnonymousLogger());
        assertFalse(in.active(Hook.FLOODGATE));
        assertEquals("integrations.reason.missing", of(in, Hook.FLOODGATE).reasonKey());
        assertFalse(of(in, Hook.FLOODGATE).present());
    }

    @Test void probeFailureIsInactive() {
        Integrations in = load("", true, Map.of(Hook.VAULT, () -> "integrations.reason.no-economy"),
                Logger.getAnonymousLogger());
        assertFalse(in.active(Hook.VAULT));
        assertEquals("integrations.reason.no-economy", of(in, Hook.VAULT).reasonKey());
    }

    @Test void probeThatThrowsCountsAsBroken() {
        Integrations in = load("", true, Map.of(Hook.VAULT, () -> {
            throw new IllegalStateException("x");
        }), Logger.getAnonymousLogger());
        assertFalse(in.active(Hook.VAULT));
        assertEquals("integrations.reason.broken", of(in, Hook.VAULT).reasonKey());
    }

    @Test void probeThatThrowsLinkageErrorCountsAsBroken() {
        Integrations in = load("", true, Map.of(Hook.VAULT, () -> {
            throw new NoClassDefFoundError("net/milkbowl/vault/economy/Economy");
        }), Logger.getAnonymousLogger());
        assertFalse(in.active(Hook.VAULT));
        assertEquals("integrations.reason.broken", of(in, Hook.VAULT).reasonKey());
    }

    @Test void reloadingPicksUpAnEconomyThatRegisteredLater() {
        String[] answer = {"integrations.reason.no-economy"};
        Map<Hook, Supplier<String>> probes = Map.of(Hook.VAULT, () -> answer[0]);
        Integrations first = load("", true, probes, Logger.getAnonymousLogger());
        assertFalse(first.active(Hook.VAULT));
        answer[0] = null; // EssentialsX registró su economía después
        assertTrue(load("", true, probes, Logger.getAnonymousLogger()).active(Hook.VAULT));
    }

    @Test void markBrokenWarnsOnce() {
        Logger log = Logger.getAnonymousLogger();
        log.setUseParentHandlers(false);
        int[] warnings = {0};
        log.addHandler(new Handler() {
            public void publish(LogRecord r) { warnings[0]++; }
            public void flush() { }
            public void close() { }
        });
        Integrations in = load("", true, Map.of(), log);
        in.markBroken(Hook.FLOODGATE, "a");
        in.markBroken(Hook.FLOODGATE, "b");
        assertEquals(1, warnings[0]);
        assertFalse(in.active(Hook.FLOODGATE));
        assertEquals("integrations.reason.broken", of(in, Hook.FLOODGATE).reasonKey());
    }

    @Test void missingKeysDefaultToEnabled() {
        Integrations in = load("", true, Map.of(), Logger.getAnonymousLogger());
        assertEquals(List.of(Hook.values()), in.report().stream().map(Integrations.Status::hook).toList());
        assertTrue(in.report().stream().allMatch(s -> s.enabled() && s.active() && s.reasonKey() == null));
    }

    @Test void brokenOnlyWhenEnabledPresentAndBroken() {
        Integrations off = load("worldguard:\n  enabled: false\n", true, Map.of(), Logger.getAnonymousLogger());
        assertFalse(off.broken(Hook.WORLDGUARD), "apagada en el yml no es rota");
        Integrations missing = load("", false, Map.of(), Logger.getAnonymousLogger());
        assertFalse(missing.broken(Hook.WORLDGUARD), "sin el plugin no es rota");
        Integrations in = load("", true, Map.of(), Logger.getAnonymousLogger());
        assertFalse(in.broken(Hook.WORLDGUARD));
        in.markBroken(Hook.WORLDGUARD, "x");
        assertTrue(in.broken(Hook.WORLDGUARD));
        Integrations probe = load("", true, Map.of(Hook.WORLDGUARD, () -> {
            throw new IllegalStateException("x");
        }), Logger.getAnonymousLogger());
        assertTrue(probe.broken(Hook.WORLDGUARD), "la comprobación que lanza también la deja rota");
    }
}
