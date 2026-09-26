package com.sack.rpgroll.quests.requirement;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.character.Characters;
import com.sack.rpgroll.common.character.RPGCharacters;
import com.sack.rpgroll.common.integration.VaultEconomy;
import com.sack.rpgroll.quests.core.ItemRequirement;
import com.sack.rpgroll.quests.core.QuestRequirements;
import com.sack.rpgroll.quests.player.QuestPlayerState;
import com.sack.rpgroll.quests.region.RegionManager;

import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Valida los {@link QuestRequirements} de una quest contra el estado actual
 * de un jugador. Devuelve la lista de motivos de rechazo (vacía = puede
 * iniciarla) en vez de un simple boolean, para poder mostrarle al jugador
 * exactamente qué le falta.
 * <p>
 * Los requisitos de personaje (nivel, raza, clase, oficio, rasgo) solo se
 * comprueban con el core de RPGRoll instalado; sin él no hay personajes y se
 * dan por cumplidos, para que las quests funcionen igual en un servidor sin core.
 */
public class QuestRequirementChecker {

    private final RegionManager regionManager;
    private final LangManager lang;

    public QuestRequirementChecker(RegionManager regionManager, LangManager lang) {
        this.regionManager = regionManager;
        this.lang = lang;
    }

    public List<String> check(Player player, QuestRequirements req, QuestPlayerState state) {

        List<String> reasons = new ArrayList<>();

        RPGCharacters characters = Characters.get().orElse(null);
        UUID uuid = player.getUniqueId();

        if (characters != null) {
            checkLevel(req, characters, uuid, reasons);
            checkRace(req, characters, uuid, reasons);
            checkClass(req, characters, uuid, reasons);
            checkProfession(req, characters, uuid, reasons);
            checkTrait(req, characters, uuid, reasons);
        }
        checkPermission(req, player, reasons);
        checkMoney(req, player, reasons);
        checkItems(req, player, reasons);
        checkCompletedQuests(req, state, reasons);
        checkWorld(req, player, reasons);
        checkRegion(req, player, reasons);
        checkBiome(req, player, reasons);
        checkWeather(req, player, reasons);
        checkTimeRange(req, player, reasons);

        return reasons;
    }

    private void checkLevel(QuestRequirements req, RPGCharacters characters, UUID uuid, List<String> reasons) {

        if (req.level() <= 0) {
            return;
        }

        int level = characters.level(uuid);

        if (level < req.level()) {
            reasons.add(lang.raw("requirement.level", "required", req.level(), "current", level));
        }
    }

    private void checkRace(QuestRequirements req, RPGCharacters characters, UUID uuid, List<String> reasons) {

        if (req.race() == null || req.race().isBlank()) {
            return;
        }

        String race = characters.race(uuid).orElse(null);

        if (race == null || !race.equalsIgnoreCase(req.race())) {
            reasons.add(lang.raw("requirement.race", "race", req.race()));
        }
    }

    private void checkClass(QuestRequirements req, RPGCharacters characters, UUID uuid, List<String> reasons) {

        if (req.playerClass() == null || req.playerClass().isBlank()) {
            return;
        }

        String playerClass = characters.playerClass(uuid).orElse(null);

        if (playerClass == null || !playerClass.equalsIgnoreCase(req.playerClass())) {
            reasons.add(lang.raw("requirement.class", "class", req.playerClass()));
        }
    }

    private void checkProfession(QuestRequirements req, RPGCharacters characters, UUID uuid, List<String> reasons) {

        if (req.profession() == null || req.profession().isBlank()) {
            return;
        }

        boolean hasJob = characters.hasJob(uuid, req.profession());

        if (!hasJob) {
            reasons.add(lang.raw("requirement.profession", "profession", req.profession()));
        }
    }

    private void checkTrait(QuestRequirements req, RPGCharacters characters, UUID uuid, List<String> reasons) {

        if (req.trait() == null || req.trait().isBlank()) {
            return;
        }

        boolean hasTrait = characters.hasTrait(uuid, req.trait());

        if (!hasTrait) {
            reasons.add(lang.raw("requirement.trait", "trait", req.trait()));
        }
    }

    private void checkPermission(QuestRequirements req, Player player, List<String> reasons) {

        if (req.permission() == null || req.permission().isBlank()) {
            return;
        }

        if (!player.hasPermission(req.permission())) {
            reasons.add(lang.raw("requirement.permission"));
        }
    }

    private void checkMoney(QuestRequirements req, Player player, List<String> reasons) {

        if (req.money() <= 0 || !VaultEconomy.isAvailable()) {
            return;
        }

        double balance = VaultEconomy.get().map(eco -> eco.getBalance(player)).orElse(0.0);

        if (balance < req.money()) {
            reasons.add(lang.raw("requirement.money", "amount", req.money()));
        }
    }

    private void checkItems(QuestRequirements req, Player player, List<String> reasons) {

        if (req.items().isEmpty()) {
            return;
        }

        PlayerInventory inventory = player.getInventory();

        for (ItemRequirement item : req.items()) {
            if (inventory.all(item.material()).values().stream().mapToInt(stack -> stack.getAmount())
                    .sum() < item.amount()) {
                reasons.add(lang.raw("requirement.item", "amount", item.amount(), "material", item.material()));
            }
        }
    }

    private void checkCompletedQuests(QuestRequirements req, QuestPlayerState state, List<String> reasons) {

        for (String questId : req.completedQuests()) {
            if (!state.hasCompleted(questId)) {
                reasons.add(lang.raw("requirement.completed_quest", "quest", questId));
            }
        }
    }

    private void checkWorld(QuestRequirements req, Player player, List<String> reasons) {

        if (req.world() == null || req.world().isBlank()) {
            return;
        }

        if (!player.getWorld().getName().equalsIgnoreCase(req.world())) {
            reasons.add(lang.raw("requirement.world", "world", req.world()));
        }
    }

    private void checkRegion(QuestRequirements req, Player player, List<String> reasons) {

        if (req.region() == null || req.region().isBlank()) {
            return;
        }

        boolean inRegion = regionManager.findAt(player.getLocation())
                .map(region -> region.id().equalsIgnoreCase(req.region()))
                .orElse(false);

        if (!inRegion) {
            reasons.add(lang.raw("requirement.region", "region", req.region()));
        }
    }

    private void checkBiome(QuestRequirements req, Player player, List<String> reasons) {

        if (req.biome() == null || req.biome().isBlank()) {
            return;
        }

        String biome = player.getLocation().getBlock().getBiome().getKey().getKey();

        if (!biome.equalsIgnoreCase(req.biome())) {
            reasons.add(lang.raw("requirement.biome", "biome", req.biome()));
        }
    }

    private void checkWeather(QuestRequirements req, Player player, List<String> reasons) {

        if (req.weather() == null || req.weather().isBlank()) {
            return;
        }

        String weather = player.getWorld().isThundering() ? "STORM"
                : player.getWorld().hasStorm() ? "RAIN" : "CLEAR";

        if (!weather.equalsIgnoreCase(req.weather())) {
            reasons.add(lang.raw("requirement.weather", "weather", req.weather()));
        }
    }

    private void checkTimeRange(QuestRequirements req, Player player, List<String> reasons) {

        if (!req.hasTimeRange()) {
            return;
        }

        long time = player.getWorld().getTime();

        if (time < req.hourMin() || time > req.hourMax()) {
            reasons.add(lang.raw("requirement.time_range", "min", req.hourMin(), "max", req.hourMax()));
        }
    }

}
