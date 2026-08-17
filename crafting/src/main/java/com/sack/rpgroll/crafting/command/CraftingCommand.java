package com.sack.rpgroll.crafting.command;

import com.sack.rpgroll.crafting.discovery.DiscoveryService;
import com.sack.rpgroll.crafting.discovery.ExperimentationOutcome;
import com.sack.rpgroll.crafting.discovery.ExperimentationService;
import com.sack.rpgroll.crafting.gui.RecipeBookGUI;
import com.sack.rpgroll.crafting.proficiency.ProficiencyLevelCurve;
import com.sack.rpgroll.crafting.proficiency.ProficiencyService;
import com.sack.rpgroll.crafting.recipe.CustomRecipeManager;
import com.sack.rpgroll.crafting.station.CustomStation;
import com.sack.rpgroll.crafting.station.CustomStationManager;
import com.sack.rpgroll.crafting.station.runtime.StationRuntime;
import com.sack.rpgroll.crafting.station.runtime.StationRuntimeRegistry;
import com.sack.rpgroll.crafting.station.tier.StationUpgradeService;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.util.TabCompleteUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class CraftingCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("book", "discover", "upgrade", "experiment", "proficiency");

    private final CustomRecipeManager recipeManager;
    private final CustomStationManager stationManager;
    private final DiscoveryService discoveryService;
    private final StationRuntimeRegistry runtimeRegistry;
    private final StationUpgradeService upgradeService;
    private final ExperimentationService experimentationService;
    private final ProficiencyService proficiencyService;
    private final LangManager lang;

    public CraftingCommand(CustomRecipeManager recipeManager, CustomStationManager stationManager,
            DiscoveryService discoveryService, StationRuntimeRegistry runtimeRegistry,
            StationUpgradeService upgradeService, ExperimentationService experimentationService,
            ProficiencyService proficiencyService, LangManager lang) {
        this.recipeManager = recipeManager;
        this.stationManager = stationManager;
        this.discoveryService = discoveryService;
        this.runtimeRegistry = runtimeRegistry;
        this.upgradeService = upgradeService;
        this.experimentationService = experimentationService;
        this.proficiencyService = proficiencyService;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text(lang.raw("common.players_only"), NamedTextColor.RED));
            return true;
        }

        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "book";

        switch (sub) {
            case "book" -> new RecipeBookGUI(player, recipeManager, stationManager, discoveryService, lang).open();
            case "discover" -> sendDiscoveredCount(player);
            case "upgrade" -> handleUpgrade(player);
            case "experiment" -> handleExperiment(player);
            case "proficiency" -> sendProficiency(player, args);
            default -> player.sendMessage(Component.text(lang.raw("common.unknown_subcommand"), NamedTextColor.RED));
        }

        return true;
    }

    private void sendDiscoveredCount(Player player) {

        int discovered = discoveryService.get(player.getUniqueId()).discoveredRecipeIds().size();
        int total = recipeManager.count();

        player.sendMessage(Component.text(
                lang.raw("command.crafting.discovered_count", "discovered", discovered, "total", total),
                NamedTextColor.AQUA));
    }

    private void handleUpgrade(Player player) {

        StationUpgradeService.Result result = upgradeService.attemptUpgrade(player);

        switch (result) {
            case OK -> player.sendMessage(Component.text(lang.raw("command.crafting.upgrade_ok"), NamedTextColor.GREEN));
            case NOT_A_STATION -> player.sendMessage(
                    Component.text(lang.raw("command.crafting.upgrade_not_a_station"), NamedTextColor.RED));
            case ALREADY_MAX -> player.sendMessage(Component.text(lang.raw("command.crafting.upgrade_already_max"), NamedTextColor.YELLOW));
            case NO_UPGRADE_DEFINED -> player.sendMessage(
                    Component.text(lang.raw("command.crafting.upgrade_no_upgrade_defined"), NamedTextColor.RED));
            case MISSING_COST -> player.sendMessage(Component.text(lang.raw("command.crafting.upgrade_missing_cost"), NamedTextColor.RED));
        }
    }

    private void handleExperiment(Player player) {

        Inventory top = player.getOpenInventory().getTopInventory();
        Optional<StationRuntime> runtimeOpt = runtimeRegistry.findByInventory(top);

        if (runtimeOpt.isEmpty()) {
            player.sendMessage(Component.text(lang.raw("command.crafting.experiment_not_a_station"), NamedTextColor.RED));
            return;
        }

        Optional<CustomStation> stationOpt = stationManager.get(runtimeOpt.get().stationDefId());
        if (stationOpt.isEmpty()) {
            return;
        }

        ExperimentationOutcome outcome = experimentationService.attempt(player, stationOpt.get(), top);

        switch (outcome.result()) {
            case DISCOVERED -> {
                lang.send(player, "command.crafting.experiment_discovered", "recipe", outcome.recipe().displayName());
                player.sendMessage(Component.text(lang.raw("command.crafting.experiment_discovered_hint"), NamedTextColor.GRAY));
            }
            case NOT_ALLOWED -> player.sendMessage(Component.text(lang.raw("command.crafting.experiment_not_allowed"), NamedTextColor.RED));
            case ON_COOLDOWN -> player.sendMessage(Component.text(lang.raw("command.crafting.experiment_on_cooldown"), NamedTextColor.YELLOW));
            case ALREADY_DISCOVERED_ALL -> player.sendMessage(
                    Component.text(lang.raw("command.crafting.experiment_already_all"), NamedTextColor.YELLOW));
            case NO_MATCH -> player.sendMessage(Component.text(lang.raw("command.crafting.experiment_no_match"), NamedTextColor.GRAY));
        }
    }

    private void sendProficiency(Player player, String[] args) {

        if (args.length < 2) {
            player.sendMessage(Component.text(lang.raw("command.crafting.proficiency_usage"), NamedTextColor.YELLOW));
            return;
        }

        String category = args[1];
        double xp = proficiencyService.get(player.getUniqueId()).xp(category);
        int level = ProficiencyLevelCurve.levelFor(xp);
        double progress = Math.round(ProficiencyLevelCurve.progressWithinLevel(xp) * 1000) / 10.0;

        player.sendMessage(Component.text(lang.raw("command.crafting.proficiency_value", "category", category,
                "level", level, "max", ProficiencyLevelCurve.MAX_LEVEL, "progress", progress),
                NamedTextColor.LIGHT_PURPLE));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        return List.of();
    }

}
