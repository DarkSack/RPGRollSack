package com.sack.rpgroll.quests.requirement;

import com.sack.rpgroll.api.RPGRollAPI;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.quests.core.ItemRequirement;
import com.sack.rpgroll.quests.core.QuestRequirements;
import com.sack.rpgroll.quests.player.QuestPlayerState;
import com.sack.rpgroll.quests.region.RegionManager;

import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Valida los {@link QuestRequirements} de una quest contra el estado actual
 * de un jugador. Devuelve la lista de motivos de rechazo (vacía = puede
 * iniciarla) en vez de un simple boolean, para poder mostrarle al jugador
 * exactamente qué le falta.
 */
public class QuestRequirementChecker {

    private final RegionManager regionManager;

    public QuestRequirementChecker(RegionManager regionManager) {
        this.regionManager = regionManager;
    }

    public List<String> check(Player player, QuestRequirements req, QuestPlayerState state) {

        List<String> reasons = new ArrayList<>();

        RPGPlayer rpgPlayer = RPGRollAPI.isReady() ? RPGRollAPI.get().getPlayer(player.getUniqueId()).orElse(null)
                : null;

        checkLevel(req, rpgPlayer, reasons);
        checkRace(req, rpgPlayer, reasons);
        checkClass(req, rpgPlayer, reasons);
        checkProfession(req, rpgPlayer, reasons);
        checkTrait(req, rpgPlayer, reasons);
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

    private void checkLevel(QuestRequirements req, RPGPlayer rpgPlayer, List<String> reasons) {

        if (req.level() <= 0) {
            return;
        }

        int level = rpgPlayer != null ? rpgPlayer.getLevel() : 0;

        if (level < req.level()) {
            reasons.add("Necesitas nivel " + req.level() + " (tenés " + level + ").");
        }
    }

    private void checkRace(QuestRequirements req, RPGPlayer rpgPlayer, List<String> reasons) {

        if (req.race() == null || req.race().isBlank()) {
            return;
        }

        String race = rpgPlayer != null ? rpgPlayer.getRace() : null;

        if (race == null || !race.equalsIgnoreCase(req.race())) {
            reasons.add("Necesitas ser de la raza: " + req.race());
        }
    }

    private void checkClass(QuestRequirements req, RPGPlayer rpgPlayer, List<String> reasons) {

        if (req.playerClass() == null || req.playerClass().isBlank()) {
            return;
        }

        String playerClass = rpgPlayer != null ? rpgPlayer.getPlayerClass() : null;

        if (playerClass == null || !playerClass.equalsIgnoreCase(req.playerClass())) {
            reasons.add("Necesitas ser de la clase: " + req.playerClass());
        }
    }

    private void checkProfession(QuestRequirements req, RPGPlayer rpgPlayer, List<String> reasons) {

        if (req.profession() == null || req.profession().isBlank()) {
            return;
        }

        boolean hasJob = rpgPlayer != null && rpgPlayer.getJobs().hasJob(req.profession());

        if (!hasJob) {
            reasons.add("Necesitas la profesión: " + req.profession());
        }
    }

    private void checkTrait(QuestRequirements req, RPGPlayer rpgPlayer, List<String> reasons) {

        if (req.trait() == null || req.trait().isBlank()) {
            return;
        }

        boolean hasTrait = rpgPlayer != null && rpgPlayer.getTraits().hasTrait(req.trait());

        if (!hasTrait) {
            reasons.add("Necesitas el trait: " + req.trait());
        }
    }

    private void checkPermission(QuestRequirements req, Player player, List<String> reasons) {

        if (req.permission() == null || req.permission().isBlank()) {
            return;
        }

        if (!player.hasPermission(req.permission())) {
            reasons.add("No tenés el permiso requerido para esta quest.");
        }
    }

    private void checkMoney(QuestRequirements req, Player player, List<String> reasons) {

        if (req.money() <= 0 || !RPGRollAPI.isReady()) {
            return;
        }

        var economy = RPGRollAPI.get().getEconomyProvider();

        if (!economy.isAvailable()) {
            return;
        }

        double balance = economy.getEconomy().map(eco -> eco.getBalance(player)).orElse(0.0);

        if (balance < req.money()) {
            reasons.add("Necesitas al menos " + req.money() + " de dinero.");
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
                reasons.add("Necesitas " + item.amount() + "x " + item.material());
            }
        }
    }

    private void checkCompletedQuests(QuestRequirements req, QuestPlayerState state, List<String> reasons) {

        for (String questId : req.completedQuests()) {
            if (!state.hasCompleted(questId)) {
                reasons.add("Necesitas haber completado la quest: " + questId);
            }
        }
    }

    private void checkWorld(QuestRequirements req, Player player, List<String> reasons) {

        if (req.world() == null || req.world().isBlank()) {
            return;
        }

        if (!player.getWorld().getName().equalsIgnoreCase(req.world())) {
            reasons.add("Tenés que estar en el mundo: " + req.world());
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
            reasons.add("Tenés que estar en la región: " + req.region());
        }
    }

    private void checkBiome(QuestRequirements req, Player player, List<String> reasons) {

        if (req.biome() == null || req.biome().isBlank()) {
            return;
        }

        String biome = player.getLocation().getBlock().getBiome().getKey().getKey();

        if (!biome.equalsIgnoreCase(req.biome())) {
            reasons.add("Tenés que estar en el bioma: " + req.biome());
        }
    }

    private void checkWeather(QuestRequirements req, Player player, List<String> reasons) {

        if (req.weather() == null || req.weather().isBlank()) {
            return;
        }

        String weather = player.getWorld().isThundering() ? "STORM"
                : player.getWorld().hasStorm() ? "RAIN" : "CLEAR";

        if (!weather.equalsIgnoreCase(req.weather())) {
            reasons.add("Necesitás clima: " + req.weather());
        }
    }

    private void checkTimeRange(QuestRequirements req, Player player, List<String> reasons) {

        if (!req.hasTimeRange()) {
            return;
        }

        long time = player.getWorld().getTime();

        if (time < req.hourMin() || time > req.hourMax()) {
            reasons.add("Solo disponible entre las " + req.hourMin() + " y " + req.hourMax() + " (hora del mundo).");
        }
    }

}
