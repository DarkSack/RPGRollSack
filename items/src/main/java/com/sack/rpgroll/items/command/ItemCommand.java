package com.sack.rpgroll.items.command;

import com.sack.rpgroll.common.command.Senders;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.items.core.ItemDefinition;
import com.sack.rpgroll.items.core.ItemManager;
import com.sack.rpgroll.items.instance.ItemInstanceService;
import com.sack.rpgroll.items.skin.SkinService;
import com.sack.rpgroll.items.socket.SocketService;
import com.sack.rpgroll.items.upgrade.UpgradeService;
import com.sack.rpgroll.util.ComponentUtils;
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
    private final LangManager langManager;

    public ItemCommand(ItemManager itemManager, ItemInstanceService instanceService, UpgradeService upgradeService,
            SkinService skinService, SocketService socketService, LangManager langManager) {
        this.itemManager = itemManager;
        this.instanceService = instanceService;
        this.upgradeService = upgradeService;
        this.skinService = skinService;
        this.socketService = socketService;
        this.langManager = langManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            langManager.send(sender, "command.item.player_only");
            return true;
        }

        if (args.length < 1) {
            langManager.send(player, "command.item.usage");
            return true;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        Optional<ItemDefinition> definitionOpt = instanceService.getId(held).flatMap(itemManager::get);

        if (definitionOpt.isEmpty()) {
            langManager.send(player, "command.item.no_item");
            return true;
        }

        ItemDefinition definition = definitionOpt.get();

        switch (args[0].toLowerCase()) {
            case "info" -> handleInfo(player, held, definition);
            case "upgrade" -> handleUpgrade(player, held, definition);
            case "skin" -> handleSkin(player, held, definition);
            case "socket" -> handleSocket(player, held, definition, args);
            default -> langManager.send(player, "command.item.usage");
        }

        return true;
    }

    private void handleInfo(Player player, ItemStack held, ItemDefinition definition) {

        player.sendMessage(ComponentUtils.parse(definition.displayName()));
        langManager.send(player, "command.item.info.pack", "pack", definition.pack());
        langManager.send(player, "command.item.info.rarity", "rarity", definition.rarityId());
        langManager.send(player, "command.item.info.upgrade_level", "level", instanceService.getUpgradeLevel(held));

        if (definition.durability().enabled()) {
            langManager.send(player, "command.item.info.durability",
                    "current", instanceService.getDurability(held, definition.durability().maxDurability()),
                    "max", definition.durability().maxDurability());
        }
    }

    private void handleUpgrade(Player player, ItemStack held, ItemDefinition definition) {

        UpgradeService.Result result = upgradeService.upgrade(player, held, definition);

        switch (result) {
            case OK -> langManager.send(player, "command.item.upgrade.ok");
            case MAX_LEVEL -> langManager.send(player, "command.item.upgrade.max_level");
            case NO_UPGRADE_DEFINED -> langManager.send(player, "command.item.upgrade.no_upgrade_defined");
            case CANT_AFFORD_MONEY -> langManager.send(player, "command.item.upgrade.cant_afford_money");
            case MISSING_MATERIAL -> langManager.send(player, "command.item.upgrade.missing_material");
        }
    }

    private void handleSkin(Player player, ItemStack held, ItemDefinition definition) {

        if (definition.skins().isEmpty()) {
            langManager.send(player, "command.item.skin.none");
            return;
        }

        int newIndex = skinService.cycleSkin(held, definition);
        String skinName = newIndex == 0 ? langManager.raw("command.item.skin.base")
                : definition.skins().get(newIndex - 1).displayName();

        langManager.send(player, "command.item.skin.changed", "skin", skinName);
    }

    private void handleSocket(Player player, ItemStack held, ItemDefinition definition, String[] args) {

        if (definition.sockets().isEmpty()) {
            langManager.send(player, "command.item.socket.none");
            return;
        }

        if (args.length < 2) {
            langManager.send(player, "command.item.socket.usage");
            return;
        }

        ItemStack gem = player.getInventory().getItemInOffHand();

        SocketService.Result result = socketService.insertGem(held, definition, args[1], gem);

        switch (result) {
            case OK -> {
                consumeOne(player, gem);
                langManager.send(player, "command.item.socket.ok");
            }
            case SOCKET_NOT_FOUND -> langManager.send(player, "command.item.socket.not_found", "id", args[1]);
            case ALREADY_FILLED -> langManager.send(player, "command.item.socket.already_filled");
            case TYPE_NOT_ACCEPTED -> langManager.send(player, "command.item.socket.type_not_accepted");
            case GEM_NOT_FOUND -> langManager.send(player, "command.item.socket.gem_not_found");
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

        if (args.length == 2 && "socket".equalsIgnoreCase(args[0]) && Senders.asPlayer(sender) instanceof Player player) {

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
