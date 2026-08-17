package com.sack.rpgroll.crafting.command;

import com.sack.rpgroll.crafting.anvil.AnvilRecipeManager;
import com.sack.rpgroll.crafting.brewing.BrewRecipeManager;
import com.sack.rpgroll.crafting.cartography.CartographyRecipeManager;
import com.sack.rpgroll.crafting.fuel.FuelManager;
import com.sack.rpgroll.crafting.grindstone.GrindstoneRecipeManager;
import com.sack.rpgroll.crafting.gui.ChatPromptManager;
import com.sack.rpgroll.crafting.gui.CraftingStudioHubGUI;
import com.sack.rpgroll.crafting.integration.ItemsBridge;
import com.sack.rpgroll.crafting.loom.LoomRecipeManager;
import com.sack.rpgroll.crafting.recipe.CustomRecipeManager;
import com.sack.rpgroll.crafting.station.CustomStationManager;
import com.sack.rpgroll.crafting.vanilla.VanillaRecipeBridge;
import com.sack.rpgroll.crafting.vanilla.VanillaRecipeManager;
import com.sack.rpgroll.crafting.villager.VillagerBinding;
import com.sack.rpgroll.crafting.villager.VillagerTradeManager;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.util.TabCompleteUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CraftingAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("browser", "reload", "give", "villager");
    private static final List<String> VILLAGER_SUBCOMMANDS = List.of("bind", "unbind");

    private final Plugin plugin;
    private final CustomStationManager stationManager;
    private final CustomRecipeManager recipeManager;
    private final FuelManager fuelManager;
    private final VanillaRecipeManager vanillaRecipeManager;
    private final AnvilRecipeManager anvilRecipeManager;
    private final BrewRecipeManager brewRecipeManager;
    private final GrindstoneRecipeManager grindstoneRecipeManager;
    private final CartographyRecipeManager cartographyRecipeManager;
    private final LoomRecipeManager loomRecipeManager;
    private final VanillaRecipeBridge vanillaRecipeBridge;
    private final VillagerTradeManager villagerTradeManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onReload;
    private final LangManager lang;

    public CraftingAdminCommand(Plugin plugin, CustomStationManager stationManager, CustomRecipeManager recipeManager,
            FuelManager fuelManager, VanillaRecipeManager vanillaRecipeManager, AnvilRecipeManager anvilRecipeManager,
            BrewRecipeManager brewRecipeManager, GrindstoneRecipeManager grindstoneRecipeManager,
            CartographyRecipeManager cartographyRecipeManager, LoomRecipeManager loomRecipeManager,
            VanillaRecipeBridge vanillaRecipeBridge, VillagerTradeManager villagerTradeManager,
            ChatPromptManager chatPromptManager, Runnable onReload, LangManager lang) {
        this.plugin = plugin;
        this.stationManager = stationManager;
        this.recipeManager = recipeManager;
        this.fuelManager = fuelManager;
        this.vanillaRecipeManager = vanillaRecipeManager;
        this.anvilRecipeManager = anvilRecipeManager;
        this.brewRecipeManager = brewRecipeManager;
        this.grindstoneRecipeManager = grindstoneRecipeManager;
        this.cartographyRecipeManager = cartographyRecipeManager;
        this.loomRecipeManager = loomRecipeManager;
        this.vanillaRecipeBridge = vanillaRecipeBridge;
        this.villagerTradeManager = villagerTradeManager;
        this.chatPromptManager = chatPromptManager;
        this.onReload = onReload;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("rpgrollcrafting.admin.*")) {
            sender.sendMessage(Component.text(lang.raw("common.no_permission"), NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text(lang.raw("command.crafting_admin.usage"), NamedTextColor.YELLOW));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "browser" -> {
                if (sender instanceof Player player) {
                    new CraftingStudioHubGUI(player, stationManager, recipeManager, fuelManager, vanillaRecipeManager,
                            anvilRecipeManager, brewRecipeManager, grindstoneRecipeManager, cartographyRecipeManager,
                            loomRecipeManager, villagerTradeManager, vanillaRecipeBridge, chatPromptManager).open();
                } else {
                    sender.sendMessage(Component.text(lang.raw("common.players_only_gui"), NamedTextColor.RED));
                }
            }
            case "reload" -> {
                onReload.run();
                sender.sendMessage(Component.text(lang.raw("command.crafting_admin.reloaded"), NamedTextColor.GREEN));
            }
            case "give" -> handleGive(sender, args);
            case "villager" -> handleVillager(sender, args);
            default -> sender.sendMessage(Component.text(lang.raw("common.unknown_subcommand"), NamedTextColor.RED));
        }

        return true;
    }

    private void handleVillager(CommandSender sender, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text(lang.raw("common.players_only"), NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text(lang.raw("command.crafting_admin.villager_usage"), NamedTextColor.YELLOW));
            return;
        }

        Entity target = player.getTargetEntity(6);
        if (!(target instanceof AbstractVillager villager)) {
            player.sendMessage(Component.text(lang.raw("command.crafting_admin.villager_target_required"), NamedTextColor.RED));
            return;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "bind" -> {
                if (args.length < 3) {
                    player.sendMessage(Component.text(lang.raw("command.crafting_admin.villager_bind_usage"), NamedTextColor.YELLOW));
                    return;
                }
                Set<String> ids = new LinkedHashSet<>(Arrays.asList(args[2].split(",")));
                ids.removeIf(id -> villagerTradeManager.get(id).isEmpty());
                if (ids.isEmpty()) {
                    player.sendMessage(Component.text(lang.raw("command.crafting_admin.villager_bind_no_valid_ids"), NamedTextColor.RED));
                    return;
                }
                VillagerBinding.bind(plugin, villager, ids);
                player.sendMessage(Component.text(
                        lang.raw("command.crafting_admin.villager_bind_success", "ids", String.join(", ", ids)),
                        NamedTextColor.GREEN));
            }
            case "unbind" -> {
                VillagerBinding.unbind(plugin, villager);
                player.sendMessage(Component.text(lang.raw("command.crafting_admin.villager_unbind_success"),
                        NamedTextColor.GREEN));
            }
            default -> player.sendMessage(Component.text(lang.raw("common.unknown_subcommand"), NamedTextColor.RED));
        }
    }

    private void handleGive(CommandSender sender, String[] args) {

        if (args.length < 3) {
            sender.sendMessage(Component.text(lang.raw("command.crafting_admin.give_usage"),
                    NamedTextColor.YELLOW));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text(lang.raw("common.player_not_found"), NamedTextColor.RED));
            return;
        }

        ItemStack item = ItemsBridge.createItem(args[2]).orElse(null);
        if (item == null) {
            sender.sendMessage(Component.text(lang.raw("command.crafting_admin.give_item_failed"),
                    NamedTextColor.RED));
            return;
        }

        target.getInventory().addItem(item);
        sender.sendMessage(Component.text(lang.raw("command.crafting_admin.give_success"), NamedTextColor.GREEN));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return TabCompleteUtil.onlinePlayerNames(args[1]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("villager")) {
            return TabCompleteUtil.filter(args[1], VILLAGER_SUBCOMMANDS);
        }

        return List.of();
    }

}
