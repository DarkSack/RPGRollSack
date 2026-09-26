package com.sack.rpgroll.economy.auction;

import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionRulesTest {

    private final List<String> warnings = new ArrayList<>();

    private AuctionSettings settings(String yaml) throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return AuctionSettings.from(config, warnings::add);
    }

    @Test
    void shippedConfigLoadsClean() {

        YamlConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(
                getClass().getResourceAsStream("/config.yml"), StandardCharsets.UTF_8));
        AuctionSettings settings = AuctionSettings.from(config, warnings::add);

        assertEquals(List.of(), warnings);
        assertEquals(List.of("ah", "subastas"), settings.aliases());
        assertEquals(48 * 3_600_000L, settings.durationMillis());
        assertTrue(settings.blacklist().contains(Material.BEDROCK));
        assertTrue(settings.blockedDataKeys().contains("rpgrollextras:backpack_bound"));
    }

    @Test
    void oldDurationKeyIsStillHonoured() throws InvalidConfigurationException {
        assertEquals(12 * 3_600_000L, settings("auction-default-duration-hours: 12").durationMillis());
        assertEquals(6 * 3_600_000L,
                settings("auction-default-duration-hours: 12\nauction-house: {duration-hours: 6}").durationMillis());
    }

    @Test
    void prices() throws InvalidConfigurationException {

        AuctionSettings settings = settings("auction-house: {min-price: 10, max-price: 1000}");

        assertNull(settings.checkPrice(10));
        assertNull(settings.checkPrice(1000));
        assertEquals("price_too_low", settings.checkPrice(9.99));
        assertEquals("price_too_high", settings.checkPrice(1000.01));
        assertEquals("invalid_price", settings.checkPrice(Double.NaN));
        assertEquals("invalid_price", settings.checkPrice(Double.POSITIVE_INFINITY));
        assertEquals("invalid_price", settings.checkPrice(-5));
    }

    @Test
    void limitsTakeTheBestPermission() throws InvalidConfigurationException {

        AuctionSettings settings = settings("""
                auction-house:
                  max-listings: 5
                  limits: {vip: 10, mvp: 20, staff: 0}
                """);

        assertEquals(5, settings.limitFor(permission -> false));
        assertEquals(10, settings.limitFor(Set.of("rpgrolleconomy.auction.limit.vip")::contains));
        assertEquals(20, settings.limitFor(Set.of("rpgrolleconomy.auction.limit.vip",
                "rpgrolleconomy.auction.limit.mvp")::contains));
        assertEquals(0, settings.limitFor(Set.of("rpgrolleconomy.auction.limit.staff")::contains));
    }

    @Test
    void badValuesAreWarnedAndFixed() throws InvalidConfigurationException {

        AuctionSettings settings = settings("""
                auction-house:
                  duration-hours: 0
                  listing-fee-percent: 150
                  min-price: 50
                  max-price: 10
                  blacklist: [NOT_A_THING]
                """);

        assertEquals(48 * 3_600_000L, settings.durationMillis());
        assertEquals(100, settings.listingFeePercent());
        assertEquals(0, settings.maxPrice());
        assertEquals(4, warnings.size(), warnings.toString());
    }

    @Test
    void fee() throws InvalidConfigurationException {
        assertEquals(25.0, settings("auction-house: {listing-fee-percent: 2.5}").fee(1000));
        assertEquals(0.0, settings("auction-house: {listing-fee-percent: 0}").fee(1000));
    }

    @Test
    void pricesTypedByPlayers() {
        assertEquals(250, AuctionPrices.parse("250"));
        assertEquals(1500, AuctionPrices.parse(" 1.5k "));
        assertEquals(2_000_000, AuctionPrices.parse("2M"));
        assertEquals(12_500, AuctionPrices.parse("12,500"));
        assertTrue(Double.isNaN(AuctionPrices.parse("mucho")));
        assertTrue(Double.isNaN(AuctionPrices.parse("NaN")));
        assertTrue(Double.isNaN(AuctionPrices.parse("1e999")));
    }

    @Test
    void categories() {
        assertEquals(AuctionCategory.SPECIAL, AuctionCategory.classify("DIAMOND_SWORD", true, false, false));
        assertEquals(AuctionCategory.BOOKS, AuctionCategory.classify("ENCHANTED_BOOK", false, false, false));
        assertEquals(AuctionCategory.POTIONS, AuctionCategory.classify("SPLASH_POTION", false, false, false));
        assertEquals(AuctionCategory.ARMOR, AuctionCategory.classify("NETHERITE_CHESTPLATE", false, false, false));
        assertEquals(AuctionCategory.ARMOR, AuctionCategory.classify("ELYTRA", false, false, false));
        assertEquals(AuctionCategory.EQUIPMENT, AuctionCategory.classify("IRON_PICKAXE", false, false, false));
        assertEquals(AuctionCategory.EQUIPMENT, AuctionCategory.classify("MACE", false, false, false));
        assertEquals(AuctionCategory.FOOD, AuctionCategory.classify("GOLDEN_CARROT", false, true, false));
        assertEquals(AuctionCategory.BLOCKS, AuctionCategory.classify("OAK_LOG", false, false, true));
        assertEquals(AuctionCategory.MATERIALS, AuctionCategory.classify("IRON_INGOT", false, false, false));
        assertEquals(AuctionCategory.MATERIALS, AuctionCategory.ALL.previous());
        assertEquals(AuctionCategory.SPECIAL, AuctionCategory.ALL.next());
    }

    @Test
    void timeLeft() {
        assertEquals("2d 4h", AuctionItems.timeLeft((2 * 24 + 4) * 3_600_000L + 59_000));
        assertEquals("3h 5m", AuctionItems.timeLeft(3 * 3_600_000L + 5 * 60_000L));
        assertEquals("1h", AuctionItems.timeLeft(3_600_000L));
        assertEquals("12m", AuctionItems.timeLeft(12 * 60_000L + 30_000));
        assertEquals("9s", AuctionItems.timeLeft(9_999));
        assertEquals("0s", AuctionItems.timeLeft(-50));
    }

}
