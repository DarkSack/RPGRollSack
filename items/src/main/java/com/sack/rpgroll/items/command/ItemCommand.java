package com.sack.rpgroll.items.command;

import com.sack.rpgroll.items.core.ItemDefinition;
import com.sack.rpgroll.items.core.ItemManager;
import com.sack.rpgroll.items.instance.ItemInstanceService;
import com.sack.rpgroll.items.skin.SkinService;
import com.sack.rpgroll.items.socket.SocketService;
import com.sack.rpgroll.items.upgrade.UpgradeService;
import com.sack.rpgroll.util.TabCompleteUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * /item info        — detalle del ítem RPGRoll en tu mano principal
 * /item upgrade      — sube de nivel el ítem en mano (cobra su costo)
 * /item skin         — cicla su próxima skin
 * /item socket &lt;id&gt; — inserta la gema de tu mano secundaria en ese socket
 */
public class ItemCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("info", "upgrade", "skin", "socket");

    private final ItemManager itemManager;
    private final ItemInstanceService instanceService;
    private final UpgradeService upgradeService;
    private final SkinService skinService;
    private final SocketService socketService;

    public ItemCommand(ItemManager itemManager, ItemInstanceService instanceService, UpgradeService upgradeService,
            SkinService skinService, SocketService socketService) {
        this.itemManager = itemManager;
        this.instanceService = instanceService;
        this.upgradeService = upgradeService;
        this.skinService = skinService;
        this.socketService = socketService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo un jugador puede usar este comando.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(Component.text("Uso: /item <info|upgrade|skin|socket> [args]", NamedTextColor.RED));
            return true;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        Optional<ItemDefinition> definitionOpt = instanceService.getId(held).flatMap(itemManager::get);

        if (definitionOpt.isEmpty()) {
            player.sendMessage(Component.text("No tenés un ítem de RPGRoll-Items en la mano.", NamedTextColor.RED));
            return true;
        }

        ItemDefinition definition = definitionOpt.get();

        switch (args[0].toLowerCase()) {
            case "info" -> handleInfo(player, held, definition);
            case "upgrade" -> handleUpgrade(player, held, definition);
            case "skin" -> handleSkin(player, held, definition);
            case "socket" -> handleSocket(player, held, definition, args);
            default -> player.sendMessage(Component.text(
                    "Uso: /item <info|upgrade|skin|socket> [args]", NamedTextColor.RED));
        }

        return true;
    }

    private void handleInfo(Player player, ItemStack held, ItemDefinition definition) {

        player.sendMessage(Component.text(definition.displayName(), NamedTextColor.GOLD));
        player.sendMessage(Component.text("Categoría: " + definition.category(), NamedTextColor.GRAY));
        player.sendMessage(Component.text("Rareza: " + definition.rarityId(), NamedTextColor.GRAY));
        player.sendMessage(Component.text("Nivel de mejora: " + instanceService.getUpgradeLevel(held),
                NamedTextColor.GRAY));

        if (definition.durability().enabled()) {
            player.sendMessage(Component.text(
                    "Durabilidad: " + instanceService.getDurability(held, definition.durability().maxDurability())
                            + "/" + definition.durability().maxDurability(),
                    NamedTextColor.GRAY));
        }
    }

    private void handleUpgrade(Player player, ItemStack held, ItemDefinition definition) {

        UpgradeService.Result result = upgradeService.upgrade(player, held, definition);

        switch (result) {
            case OK -> player.sendMessage(Component.text("✔ Ítem mejorado.", NamedTextColor.GREEN));
            case MAX_LEVEL -> player.sendMessage(Component.text(
                    "Este ítem ya está en su nivel máximo.", NamedTextColor.RED));
            case NO_UPGRADE_DEFINED -> player.sendMessage(Component.text(
                    "El próximo nivel de mejora no está definido todavía.", NamedTextColor.RED));
            case CANT_AFFORD_MONEY -> player.sendMessage(Component.text(
                    "No tenés suficiente dinero para esta mejora.", NamedTextColor.RED));
            case MISSING_MATERIAL -> player.sendMessage(Component.text(
                    "Te falta el material requerido para esta mejora.", NamedTextColor.RED));
        }
    }

    private void handleSkin(Player player, ItemStack held, ItemDefinition definition) {

        if (definition.skins().isEmpty()) {
            player.sendMessage(Component.text("Este ítem no tiene skins alternativas.", NamedTextColor.RED));
            return;
        }

        int newIndex = skinService.cycleSkin(held, definition);
        String skinName = newIndex == 0 ? "apariencia base" : definition.skins().get(newIndex - 1).displayName();

        player.sendMessage(Component.text("✔ Skin cambiada a: " + skinName, NamedTextColor.GREEN));
    }

    private void handleSocket(Player player, ItemStack held, ItemDefinition definition, String[] args) {

        if (definition.sockets().isEmpty()) {
            player.sendMessage(Component.text("Este ítem no tiene sockets.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Uso: /item socket <id> (con la gema en tu mano secundaria)",
                    NamedTextColor.RED));
            return;
        }

        ItemStack gem = player.getInventory().getItemInOffHand();

        SocketService.Result result = socketService.insertGem(held, definition, args[1], gem);

        switch (result) {
            case OK -> {
                consumeOne(player, gem);
                player.sendMessage(Component.text("✔ Gema insertada.", NamedTextColor.GREEN));
            }
            case SOCKET_NOT_FOUND -> player.sendMessage(Component.text(
                    "Ese ítem no tiene un socket con id: " + args[1], NamedTextColor.RED));
            case ALREADY_FILLED -> player.sendMessage(Component.text(
                    "Ese socket ya tiene una gema.", NamedTextColor.RED));
            case TYPE_NOT_ACCEPTED -> player.sendMessage(Component.text(
                    "Ese socket no acepta este tipo de gema.", NamedTextColor.RED));
            case GEM_NOT_FOUND -> player.sendMessage(Component.text(
                    "Necesitás tener una gema válida en tu mano secundaria.", NamedTextColor.RED));
        }
    }

    private void consumeOne(Player player, ItemStack gemStack) {

        int newAmount = gemStack.getAmount() - 1;

        if (newAmount <= 0) {
            player.getInventory().setItemInOffHand(null);
        } else {
            gemStack.setAmount(newAmount);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        if (args.length == 2 && "socket".equalsIgnoreCase(args[0]) && sender instanceof Player player) {

            ItemStack held = player.getInventory().getItemInMainHand();
            Optional<ItemDefinition> definitionOpt = instanceService.getId(held).flatMap(itemManager::get);

            if (definitionOpt.isEmpty()) {
                return List.of();
            }

            return TabCompleteUtil.filter(args[1],
                    definitionOpt.get().sockets().stream().map(socket -> socket.id()).toList());
        }

        return List.of();
    }

}
