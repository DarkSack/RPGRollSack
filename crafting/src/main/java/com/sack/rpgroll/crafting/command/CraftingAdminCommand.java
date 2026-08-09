package com.sack.rpgroll.crafting.command;

import com.sack.rpgroll.crafting.anvil.AnvilRecipeManager;
import com.sack.rpgroll.crafting.brewing.BrewRecipeManager;
import com.sack.rpgroll.crafting.fuel.FuelManager;
import com.sack.rpgroll.crafting.gui.ChatPromptManager;
import com.sack.rpgroll.crafting.gui.CraftingStudioHubGUI;
import com.sack.rpgroll.crafting.integration.ItemsBridge;
import com.sack.rpgroll.crafting.recipe.CustomRecipeManager;
import com.sack.rpgroll.crafting.station.CustomStationManager;
import com.sack.rpgroll.crafting.vanilla.VanillaRecipeManager;
import com.sack.rpgroll.util.TabCompleteUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;

public class CraftingAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("browser", "reload", "give");

    private final CustomStationManager stationManager;
    private final CustomRecipeManager recipeManager;
    private final FuelManager fuelManager;
    private final VanillaRecipeManager vanillaRecipeManager;
    private final AnvilRecipeManager anvilRecipeManager;
    private final BrewRecipeManager brewRecipeManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onReload;

    public CraftingAdminCommand(CustomStationManager stationManager, CustomRecipeManager recipeManager,
            FuelManager fuelManager, VanillaRecipeManager vanillaRecipeManager, AnvilRecipeManager anvilRecipeManager,
            BrewRecipeManager brewRecipeManager, ChatPromptManager chatPromptManager, Runnable onReload) {
        this.stationManager = stationManager;
        this.recipeManager = recipeManager;
        this.fuelManager = fuelManager;
        this.vanillaRecipeManager = vanillaRecipeManager;
        this.anvilRecipeManager = anvilRecipeManager;
        this.brewRecipeManager = brewRecipeManager;
        this.chatPromptManager = chatPromptManager;
        this.onReload = onReload;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("rpgrollcrafting.admin.*")) {
            sender.sendMessage(Component.text("No tenés permiso.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("Uso: /craftingadmin <browser|reload|give>", NamedTextColor.YELLOW));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "browser" -> {
                if (sender instanceof Player player) {
                    new CraftingStudioHubGUI(player, stationManager, recipeManager, fuelManager, vanillaRecipeManager,
                            anvilRecipeManager, brewRecipeManager, chatPromptManager).open();
                } else {
                    sender.sendMessage(Component.text("Solo un jugador puede abrir el navegador.", NamedTextColor.RED));
                }
            }
            case "reload" -> {
                onReload.run();
                sender.sendMessage(Component.text("✔ RPGRoll-Crafting recargado.", NamedTextColor.GREEN));
            }
            case "give" -> handleGive(sender, args);
            default -> sender.sendMessage(Component.text("Subcomando desconocido.", NamedTextColor.RED));
        }

        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {

        if (args.length < 3) {
            sender.sendMessage(Component.text("Uso: /craftingadmin give <jugador> <item-id-de-RPGRoll-Items>",
                    NamedTextColor.YELLOW));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Jugador no encontrado (debe estar online).", NamedTextColor.RED));
            return;
        }

        ItemStack item = ItemsBridge.createItem(args[2]).orElse(null);
        if (item == null) {
            sender.sendMessage(Component.text("No se pudo crear ese ítem (¿RPGRoll-Items instalado? ¿id correcto?)",
                    NamedTextColor.RED));
            return;
        }

        target.getInventory().addItem(item);
        sender.sendMessage(Component.text("✔ Entregado.", NamedTextColor.GREEN));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return TabCompleteUtil.onlinePlayerNames(args[1]);
        }

        return List.of();
    }

}
