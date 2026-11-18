package com.sack.rpgroll.economy.servershop;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SellQuotaTest {

    private static SellQuota quota() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("full-price-units", 10);
        yaml.set("max-units", 20);
        yaml.set("reduced-ratio", 0.5);
        SellQuota quota = new SellQuota();
        quota.configure(yaml);
        return quota;
    }

    @Test
    void paysFullThenReducedThenNothing() {
        SellQuota quota = quota();
        assertEquals(new SellQuota.Deal(8, 8.0), quota.quote(0, 8, 1.0));
        assertEquals(new SellQuota.Deal(8, 6.0), quota.quote(6, 8, 1.0));
        assertEquals(new SellQuota.Deal(4, 2.0), quota.quote(16, 8, 1.0));
        assertEquals(new SellQuota.Deal(0, 0.0), quota.quote(20, 8, 1.0));
    }

    @Test
    void recordsPerPlayerAndKey() {
        SellQuota quota = quota();
        UUID a = UUID.randomUUID();
        quota.record(a, "COBBLESTONE", 15);
        assertEquals(15, quota.soldToday(a, "COBBLESTONE"));
        assertEquals(0, quota.soldToday(a, "DIRT"));
        assertEquals(0, quota.soldToday(UUID.randomUUID(), "COBBLESTONE"));
        assertEquals(new SellQuota.Deal(5, 2.5), quota.quote(a, "COBBLESTONE", 10, 1.0));
    }

    @Test
    void disabledWithoutSection() {
        SellQuota quota = new SellQuota();
        quota.configure(null);
        assertEquals(new SellQuota.Deal(1000, 1000.0), quota.quote(0, 1000, 1.0));
    }
}
