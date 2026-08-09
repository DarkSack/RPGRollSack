package com.sack.rpgroll.items.command;

import com.sack.rpgroll.items.core.ItemDefinition;
import com.sack.rpgroll.items.core.ItemFactory;
import com.sack.rpgroll.items.core.ItemManager;
import com.sack.rpgroll.items.gui.ChatPromptManager;
import com.sack.rpgroll.items.gui.ItemBrowserGUI;
import com.sack.rpgroll.items.gui.editor.EditorSession;
import com.sack.rpgroll.items.gui.editor.ItemEditorHubGUI;
import com.sack.rpgroll.items.rarity.RarityManager;
import com.sack.rpgroll.items.registry.StatRegistry;
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

    private final ItemManager itemManager;
    private final ItemFactory itemFactory;
    private final RarityManager rarityManager;
    private final StatRegistry statRegistry;
    private final ChatPromptManager chatPromptManager;
    private final Plugin plugin;

    public ItemAdminCommand(ItemManager itemManager, ItemFactory itemFactory, RarityManager rarityManager,
            StatRegistry statRegistry, ChatPromptManager chatPromptManager, Plugin plugin) {
        this.itemManager = itemManager;
        this.itemFactory = itemFactory;
        this.rarityManager = rarityManager;
        this.statRegistry = statRegistry;
        this.chatPromptManager = chatPromptManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(Component.text("No tenés permiso para usar este comando.", NamedTextColor.RED));
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
        sender.sendMessage(Component.text(
                "Uso: /itemadmin <give|list|reload|create|browser|editor> [args]", NamedTextColor.RED));
    }

    private void handleCreate(CommandSender sender, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo un jugador puede crear un ítem.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Uso: /itemadmin create <id> [categoria]", NamedTextColor.RED));
            return;
        }

        String id = args[1].toLowerCase();

        if (itemManager.exists(id)) {
            sender.sendMessage(Component.text("Ya existe un ítem con ese id.", NamedTextColor.RED));
            return;
        }

        String category = args.length >= 3 ? args[2].toLowerCase() : "misc";

        ItemDefinition definition = new ItemDefinition(id, category, org.bukkit.Material.PAPER, id, null, null,
                null, null, null, false, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, 0, 0, null);

        EditorSession session = new EditorSession(definition, itemFactory, itemManager, rarityManager, statRegistry,
                chatPromptManager, plugin);

        sender.sendMessage(Component.text("✔ Ítem '" + id + "' creado — configuralo en el editor.",
                NamedTextColor.GREEN));

        new ItemEditorHubGUI(player, session).open();
    }

    private void handleGive(CommandSender sender, String[] args) {

        if (args.length < 3) {
            sender.sendMessage(Component.text("Uso: /itemadmin give <jugador> <id> [cantidad]", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Jugador no encontrado: " + args[1], NamedTextColor.RED));
            return;
        }

        Optional<ItemDefinition> definitionOpt = itemManager.get(args[2]);
        if (definitionOpt.isEmpty()) {
            sender.sendMessage(Component.text("No existe un ítem con id: " + args[2], NamedTextColor.RED));
            return;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException ignored) {
            }
        }

        ItemStack item = itemFactory.create(definitionOpt.get());
        item.setAmount(Math.max(1, amount));

        var leftover = target.getInventory().addItem(item);
        leftover.values().forEach(remaining -> target.getWorld().dropItemNaturally(target.getLocation(), remaining));

        sender.sendMessage(Component.text(
                "✔ " + amount + "x '" + args[2] + "' entregado a " + target.getName(), NamedTextColor.GREEN));
    }

    private void handleList(CommandSender sender) {

        if (itemManager.count() == 0) {
            sender.sendMessage(Component.text("No hay ítems definidos.", NamedTextColor.GRAY));
            return;
        }

        sender.sendMessage(Component.text("Ítems disponibles:", NamedTextColor.GOLD));

        for (ItemDefinition definition : itemManager.getAll()) {
            sender.sendMessage(Component.text(
                    "• " + definition.id() + " (" + definition.category() + ", " + definition.rarityId() + ")",
                    NamedTextColor.WHITE));
        }
    }

    private void handleReload(CommandSender sender) {
        itemManager.reload();
        sender.sendMessage(Component.text("✔ Recargado: " + itemManager.count() + " ítem(s).", NamedTextColor.GREEN));
    }

    private void handleBrowser(CommandSender sender) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo un jugador puede usar esto.", NamedTextColor.RED));
            return;
        }

        new ItemBrowserGUI(player, itemManager, itemFactory, rarityManager, statRegistry, chatPromptManager, plugin)
                .open();
    }

    private void handleEditor(CommandSender sender, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo un jugador puede usar esto.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Uso: /itemadmin editor <id>", NamedTextColor.RED));
            return;
        }

        Optional<ItemDefinition> definitionOpt = itemManager.get(args[1]);
        if (definitionOpt.isEmpty()) {
            sender.sendMessage(Component.text("No existe un ítem con id: " + args[1], NamedTextColor.RED));
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
