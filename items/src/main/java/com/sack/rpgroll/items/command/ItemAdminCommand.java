package com.sack.rpgroll.items.command;

import com.sack.rpgroll.common.command.Senders;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.items.core.ItemDefinition;
import com.sack.rpgroll.items.core.ItemFactory;
import com.sack.rpgroll.items.core.ItemManager;
import com.sack.rpgroll.items.gui.ChatPromptManager;
import com.sack.rpgroll.items.gui.ItemBrowserGUI;
import com.sack.rpgroll.items.gui.editor.EditorSession;
import com.sack.rpgroll.items.gui.editor.ItemEditorHubGUI;
import com.sack.rpgroll.items.pack.PackAssetSync;
import com.sack.rpgroll.items.pack.PackManager;
import com.sack.rpgroll.items.pack.PackSelectorGUI;
import com.sack.rpgroll.items.rarity.RarityManager;
import com.sack.rpgroll.items.registry.StatRegistry;
import com.sack.rpgroll.util.TabCompleteUtil;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Optional;

/**
 * /itemadmin give &lt;jugador&gt; &lt;id&gt; [cantidad]
 * /itemadmin list
 * /itemadmin reload
 * /itemadmin browser        — abre el navegador de ítems (GUI)
 * /itemadmin editor &lt;id&gt; — abre el editor directo de un ítem (GUI)
 */
public class ItemAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("give", "list", "reload", "create", "browser", "editor");

    private static final String PERMISSION = "rpgrollitems.admin.*";
    private static final int MAX_GIVE_AMOUNT = 6400;

    private final ItemManager itemManager;
    private final ItemFactory itemFactory;
    private final RarityManager rarityManager;
    private final StatRegistry statRegistry;
    private final ChatPromptManager chatPromptManager;
    private final PackManager packManager;
    private final Plugin plugin;

    public ItemAdminCommand(ItemManager itemManager, ItemFactory itemFactory, RarityManager rarityManager,
            StatRegistry statRegistry, ChatPromptManager chatPromptManager, PackManager packManager, Plugin plugin) {
        this.itemManager = itemManager;
        this.itemFactory = itemFactory;
        this.rarityManager = rarityManager;
        this.statRegistry = statRegistry;
        this.chatPromptManager = chatPromptManager;
        this.packManager = packManager;
        this.plugin = plugin;
    }

    private LangManager lang() {
        return chatPromptManager.lang();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission(PERMISSION)) {
            lang().send(sender, "general.no_permission");
            return true;
        }

        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give" -> handleGive(sender, args);
            case "list" -> handleList(sender);
            case "reload" -> handleReload(sender);
            case "create" -> handleCreate(sender, args);
            case "browser" -> handleBrowser(sender);
            case "editor" -> handleEditor(sender, args);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        lang().send(sender, "command.itemadmin.usage");
    }

    private void handleCreate(CommandSender sender, String[] args) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            lang().send(sender, "command.itemadmin.create.player_only");
            return;
        }

        if (args.length < 2) {
            lang().send(sender, "command.itemadmin.create.usage");
            return;
        }

        String id = args[1].toLowerCase();

        if (itemManager.exists(id)) {
            lang().send(sender, "command.itemadmin.create.already_exists");
            return;
        }

        if (args.length >= 3) {
            String pack = args[2].toLowerCase();
            packManager.create(pack);
            createItemInPack(player, id, pack);
            return;
        }

        new PackSelectorGUI(player, packManager, chatPromptManager, pack -> createItemInPack(player, id, pack))
                .open();
    }

    private void createItemInPack(Player player, String id, String pack) {

        ItemDefinition definition = new ItemDefinition(id, pack, org.bukkit.Material.PAPER, id, null, null,
                null, null, null, false, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, 0, 0, null);

        EditorSession session = new EditorSession(definition, itemFactory, itemManager, rarityManager, statRegistry,
                chatPromptManager, plugin);

        lang().send(player, "command.itemadmin.create.success", "id", id);

        new ItemEditorHubGUI(player, session).open();
    }

    private void handleGive(CommandSender sender, String[] args) {

        if (args.length < 3) {
            lang().send(sender, "command.itemadmin.give.usage");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            lang().send(sender, "command.itemadmin.give.player_not_found", "player", args[1]);
            return;
        }

        Optional<ItemDefinition> definitionOpt = itemManager.get(args[2]);
        if (definitionOpt.isEmpty()) {
            lang().send(sender, "command.itemadmin.give.item_not_found", "id", args[2]);
            return;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                lang().send(sender, "command.itemadmin.give.invalid_amount");
                return;
            }
        }

        if (amount < 1 || amount > MAX_GIVE_AMOUNT) {
            lang().send(sender, "command.itemadmin.give.amount_range", "max", MAX_GIVE_AMOUNT);
            return;
        }

        ItemStack item = itemFactory.create(definitionOpt.get());
        item.setAmount(amount);

        boolean fullyDelivered = com.sack.rpgroll.util.ItemDeliveryUtil.deliver(target, item);

        String overflow = fullyDelivered ? "" : lang().raw("command.itemadmin.give.overflow_suffix");

        lang().send(sender, "command.itemadmin.give.success", "amount", amount, "id", args[2],
                "player", target.getName(), "overflow", overflow);
    }

    private void handleList(CommandSender sender) {

        if (itemManager.count() == 0) {
            lang().send(sender, "command.itemadmin.list.empty");
            return;
        }

        lang().send(sender, "command.itemadmin.list.header");

        for (ItemDefinition definition : itemManager.getAll()) {
            lang().send(sender, "command.itemadmin.list.entry", "id", definition.id(),
                    "pack", definition.pack(), "rarity", definition.rarityId());
        }
    }

    private void handleReload(CommandSender sender) {
        itemManager.reload();
        new PackAssetSync(plugin, packManager).syncAll();
        lang().reload(plugin.getConfig().getString("language", "es"));
        lang().send(sender, "command.itemadmin.reload.success", "count", itemManager.count());
    }

    private void handleBrowser(CommandSender sender) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            lang().send(sender, "command.itemadmin.player_only");
            return;
        }

        new ItemBrowserGUI(player, itemManager, itemFactory, rarityManager, statRegistry, chatPromptManager,
                packManager, plugin).open();
    }

    private void handleEditor(CommandSender sender, String[] args) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            lang().send(sender, "command.itemadmin.player_only");
            return;
        }

        if (args.length < 2) {
            lang().send(sender, "command.itemadmin.editor.usage");
            return;
        }

        Optional<ItemDefinition> definitionOpt = itemManager.get(args[1]);
        if (definitionOpt.isEmpty()) {
            lang().send(sender, "command.itemadmin.editor.not_found", "id", args[1]);
            return;
        }

        EditorSession session = new EditorSession(definitionOpt.get(), itemFactory, itemManager, rarityManager,
                statRegistry, chatPromptManager, plugin);

        new ItemEditorHubGUI(player, session).open();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2) {
            return switch (sub) {
                case "give" -> TabCompleteUtil.onlinePlayerNames(args[1]);
                case "editor" -> TabCompleteUtil.filter(args[1], itemIds());
                default -> List.of();
            };
        }

        if (args.length == 3 && "give".equals(sub)) {
            return TabCompleteUtil.filter(args[2], itemIds());
        }

        return List.of();
    }

    private List<String> itemIds() {
        return itemManager.getAll().stream().map(ItemDefinition::id).toList();
    }

}
