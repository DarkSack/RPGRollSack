package com.sack.rpgroll.extras.admin;

import com.sack.rpgroll.extras.stat.StatDefinition;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Menú de admin de las necesidades (sed, fatiga, estrés, oxígeno...): ajustes globales que se
 * guardan en los YAML y recargan, y control de cada jugador conectado.
 */
public final class NeedsAdminMenu extends InventoryGUI {

    private enum View { HOME, STAT, AFK, WORLDS, PLAYERS, PLAYER }

    private static final int BACK = 45;
    private static final int CLOSE = 53;
    // shortcut: una página; con más de 36 mundos o jugadores conectados no se ven todos, paginar entonces.
    private static final int LIST_FIRST = 9;
    private static final int LIST_LAST = 44;
    // shortcut: 5 necesidades por jugador (una por fila); con más, paginar.
    private static final int PLAYER_ROWS = 5;
    private static final int[] HOME_STATS = {19, 20, 21, 22, 23, 24, 25};

    private final NeedsAdmin admin;
    private View view = View.HOME;
    private String statId;
    private UUID target;

    public NeedsAdminMenu(Player player, NeedsAdmin admin) {
        super(player, admin.lang().component("admin_menu.title"), 54);
        this.admin = admin;
    }

    // ------------------------------------------------------------------ dibujo

    private Component text(String key, Object... pairs) {
        return admin.lang().component("admin_menu." + key, pairs).decoration(TextDecoration.ITALIC, false);
    }

    private ItemStack item(Material material, Component name, Component... lore) {
        return new ItemBuilder(material).setName(name)
                .setLore(java.util.Arrays.stream(lore).map(c -> c.decoration(TextDecoration.ITALIC, false)).toList())
                .build();
    }

    private String statName(String id) {
        String key = "admin_menu.names." + id;
        String found = admin.lang().raw(key);
        return found.equals(key) ? id : found;
    }

    private static Material statIcon(String id) {
        return switch (id) {
            case "thirst" -> Material.POTION;
            case "fatigue" -> Material.RED_BED;
            case "stress" -> Material.WITHER_ROSE;
            case "oxygen" -> Material.HEART_OF_THE_SEA;
            case "stamina" -> Material.FEATHER;
            default -> Material.PAPER;
        };
    }

    private Component onOff(boolean on) {
        return text(on ? "state_on" : "state_off");
    }

    private static String number(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    private List<StatDefinition> stats() {
        return admin.stats().getAll().stream().sorted(java.util.Comparator.comparing(StatDefinition::id)).toList();
    }

    @Override
    public void build() {
        clear();
        if (view != View.HOME) {
            setItem(BACK, item(Material.ARROW, text("back")));
        }
        setItem(CLOSE, item(Material.BARRIER, text("close")));
        switch (view) {
            case HOME -> buildHome();
            case STAT -> buildStat();
            case AFK -> buildAfk();
            case WORLDS -> buildWorlds();
            case PLAYERS -> buildPlayers();
            case PLAYER -> buildPlayer();
        }
    }

    private void buildHome() {
        setItem(4, item(Material.BOOK, text("title"), text("home_hint")));
        List<StatDefinition> stats = stats();
        for (int i = 0; i < stats.size() && i < HOME_STATS.length; i++) {
            StatDefinition stat = stats.get(i);
            setItem(HOME_STATS[i], item(statIcon(stat.id()), text("stat_name", "name", statName(stat.id())),
                    text("state", "state", admin.lang().raw(stat.enabled() ? "admin_menu.state_on" : "admin_menu.state_off")),
                    text("speed_line", "speed", number(admin.engine().speed(stat.id()))),
                    text("max_line", "max", number(stat.max())),
                    text("click_adjust")));
        }
        boolean temperature = admin.temperatureEnabled();
        setItem(29, item(temperature ? Material.MAGMA_CREAM : Material.SNOWBALL, text("temperature"),
                text("state", "state", admin.lang().raw(temperature ? "admin_menu.state_on" : "admin_menu.state_off")),
                text("click_toggle")));
        setItem(31, item(Material.CLOCK, text("afk"), text("click_adjust")));
        setItem(33, item(Material.GRASS_BLOCK, text("worlds"), text("worlds_hint")));
        setItem(40, item(Material.PLAYER_HEAD, text("players"),
                text("players_hint", "count", Bukkit.getOnlinePlayers().size())));
    }

    private StatDefinition stat() {
        return statId == null ? null : admin.stats().get(statId).orElse(null);
    }

    private void buildStat() {
        StatDefinition stat = stat();
        if (stat == null) {
            view = View.HOME;
            buildHome();
            return;
        }
        setItem(4, item(statIcon(stat.id()), text("stat_name", "name", statName(stat.id())),
                text("max_line", "max", number(stat.max())), text("start_line", "start", number(stat.start()))));
        setItem(20, item(stat.enabled() ? Material.LIME_DYE : Material.GRAY_DYE, text("enabled"),
                onOff(stat.enabled()), text("click_toggle")));
        setItem(22, item(Material.SUGAR, text("speed"),
                text("speed_line", "speed", number(admin.engine().speed(stat.id()))), text("speed_hint")));
        setItem(24, item(stat.resetOnDeath() ? Material.TOTEM_OF_UNDYING : Material.SKELETON_SKULL,
                text("reset_on_death"), onOff(stat.resetOnDeath()), text("click_toggle")));
    }

    private void buildAfk() {
        var config = admin.plugin().getConfig();
        boolean pause = config.getBoolean("afk.pause-stats", true);
        int seconds = config.getInt("afk.idle-seconds", 300);
        setItem(4, item(Material.CLOCK, text("afk"), text("afk_hint")));
        setItem(20, item(pause ? Material.LIME_DYE : Material.GRAY_DYE, text("afk_pause"), onOff(pause),
                text("click_toggle")));
        setItem(23, item(Material.RED_STAINED_GLASS_PANE, text("minus_minute")));
        ItemStack time = item(Material.CLOCK, text("afk_time", "minutes", number(seconds / 60.0)));
        time.setAmount(Math.max(1, Math.min(64, seconds / 60)));
        setItem(24, time);
        setItem(25, item(Material.LIME_STAINED_GLASS_PANE, text("plus_minute")));
    }

    private List<String> offWorlds() {
        return admin.plugin().getConfig().getStringList("stats-disabled-worlds");
    }

    private void buildWorlds() {
        setItem(4, item(Material.GRASS_BLOCK, text("worlds"), text("worlds_hint")));
        List<String> off = offWorlds();
        int slot = LIST_FIRST;
        for (World world : Bukkit.getWorlds()) {
            if (slot > LIST_LAST) {
                break;
            }
            Material icon = switch (world.getEnvironment()) {
                case NETHER -> Material.NETHERRACK;
                case THE_END -> Material.END_STONE;
                default -> Material.GRASS_BLOCK;
            };
            boolean needs = !off.contains(world.getName());
            setItem(slot++, item(needs ? icon : Material.BARRIER, text("world_name", "world", world.getName()),
                    text(needs ? "world_on" : "world_off"), text("click_toggle")));
        }
    }

    private ItemStack head(Player who, Component name, Component... lore) {
        ItemStack head = item(Material.PLAYER_HEAD, name, lore);
        if (head.getItemMeta() instanceof SkullMeta meta) {
            meta.setOwningPlayer(who);
            head.setItemMeta(meta);
        }
        return head;
    }

    private List<Player> online() {
        return Bukkit.getOnlinePlayers().stream().map(p -> (Player) p)
                .sorted(java.util.Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    private void buildPlayers() {
        setItem(4, item(Material.PLAYER_HEAD, text("players"),
                text("players_hint", "count", Bukkit.getOnlinePlayers().size())));
        int slot = LIST_FIRST;
        for (Player online : online()) {
            if (slot > LIST_LAST) {
                break;
            }
            setItem(slot++, head(online, text("player_name", "player", online.getName()), text("click_adjust")));
        }
    }

    private Player targetPlayer() {
        return target == null ? null : Bukkit.getPlayer(target);
    }

    private void buildPlayer() {
        Player who = targetPlayer();
        if (who == null) {
            view = View.PLAYERS;
            buildPlayers();
            return;
        }
        List<StatDefinition> stats = stats();
        for (int r = 0; r < stats.size() && r < PLAYER_ROWS; r++) {
            StatDefinition stat = stats.get(r);
            int row = r * 9;
            double value = admin.engine().get(who, stat.id());
            setItem(row, item(statIcon(stat.id()), text("stat_name", "name", statName(stat.id())),
                    text("value_line", "value", number(Math.round(value * 10) / 10.0), "max", number(stat.max()))));
            setItem(row + 2, item(Material.RED_STAINED_GLASS_PANE, text("minus_ten")));
            setItem(row + 3, item(Material.LIME_STAINED_GLASS_PANE, text("plus_ten")));
            setItem(row + 5, item(Material.HONEY_BOTTLE, text("fill")));
            setItem(row + 6, item(Material.GLASS_BOTTLE, text("empty")));
        }
        List<String> conditions = admin.runtime().activeConditions(who);
        for (int r = 0; r < conditions.size() && r < PLAYER_ROWS; r++) {
            setItem(r * 9 + 8, item(Material.FERMENTED_SPIDER_EYE,
                    text("condition_name", "condition", conditions.get(r)), text("condition_hint")));
        }
        setItem(47, head(who, text("player_name", "player", who.getName()),
                text("temperature_line", "value", number(Math.round(admin.temperature().bodyTemperature(who) * 10) / 10.0))));
        setItem(49, item(Material.GOLDEN_APPLE, text("cure_all"), text("cure_all_hint")));
        boolean immune = admin.engine().isImmune(who);
        setItem(51, item(immune ? Material.TOTEM_OF_UNDYING : Material.SHIELD, text("immune"), onOff(immune),
                text("immune_hint")));
    }

    // ------------------------------------------------------------------ clics

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot == CLOSE) {
            close();
            return;
        }
        if (slot == BACK && view != View.HOME) {
            view = view == View.PLAYER ? View.PLAYERS : View.HOME;
            click();
            build();
            return;
        }
        boolean changed = switch (view) {
            case HOME -> clickHome(slot);
            case STAT -> clickStat(slot);
            case AFK -> clickAfk(slot);
            case WORLDS -> clickWorld(slot);
            case PLAYERS -> clickPlayers(slot);
            case PLAYER -> clickPlayer(slot);
        };
        if (changed) {
            click();
            build();
        }
    }

    private void click() {
        player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5f, 1.4f);
    }

    private boolean clickHome(int slot) {
        List<StatDefinition> stats = stats();
        for (int i = 0; i < HOME_STATS.length; i++) {
            if (HOME_STATS[i] == slot && i < stats.size()) {
                statId = stats.get(i).id();
                view = View.STAT;
                return true;
            }
        }
        return switch (slot) {
            case 29 -> admin.save(player, admin.temperatureFile(), "enabled", !admin.temperatureEnabled());
            case 31 -> open(View.AFK);
            case 33 -> open(View.WORLDS);
            case 40 -> open(View.PLAYERS);
            default -> false;
        };
    }

    private boolean open(View next) {
        view = next;
        return true;
    }

    private boolean clickStat(int slot) {
        StatDefinition stat = stat();
        if (stat == null) {
            return open(View.HOME);
        }
        return switch (slot) {
            case 20 -> admin.save(player, admin.statFile(stat.id()), "enabled", !stat.enabled());
            case 22 -> admin.save(player, admin.configFile(), "needs-speed." + stat.id(),
                    NeedsSettings.nextSpeed(admin.engine().speed(stat.id())));
            case 24 -> admin.save(player, admin.statFile(stat.id()), "reset-on-death", !stat.resetOnDeath());
            default -> false;
        };
    }

    private boolean clickAfk(int slot) {
        var config = admin.plugin().getConfig();
        int seconds = config.getInt("afk.idle-seconds", 300);
        return switch (slot) {
            case 20 -> admin.save(player, admin.configFile(), "afk.pause-stats", !config.getBoolean("afk.pause-stats", true));
            case 23 -> admin.save(player, admin.configFile(), "afk.idle-seconds", NeedsSettings.idleStep(seconds, -60));
            case 25 -> admin.save(player, admin.configFile(), "afk.idle-seconds", NeedsSettings.idleStep(seconds, 60));
            default -> false;
        };
    }

    private boolean clickWorld(int slot) {
        List<World> worlds = Bukkit.getWorlds();
        int index = slot - LIST_FIRST;
        if (slot < LIST_FIRST || slot > LIST_LAST || index >= worlds.size()) {
            return false;
        }
        return admin.save(player, admin.configFile(), "stats-disabled-worlds",
                NeedsSettings.toggle(offWorlds(), worlds.get(index).getName()));
    }

    private boolean clickPlayers(int slot) {
        List<Player> online = online();
        int index = slot - LIST_FIRST;
        if (slot < LIST_FIRST || slot > LIST_LAST || index >= online.size()) {
            return false;
        }
        target = online.get(index).getUniqueId();
        return open(View.PLAYER);
    }

    private boolean clickPlayer(int slot) {
        Player who = targetPlayer();
        if (who == null) {
            return open(View.PLAYERS);
        }
        switch (slot) {
            case 49 -> {
                admin.cure(who);
                return true;
            }
            case 51 -> {
                admin.engine().setImmune(who, !admin.engine().isImmune(who));
                admin.engine().save(who);
                return true;
            }
            default -> {
            }
        }
        int row = slot / 9;
        int col = slot % 9;
        if (row >= PLAYER_ROWS) {
            return false;
        }
        if (col == 8) {
            List<String> conditions = new ArrayList<>(admin.runtime().activeConditions(who));
            if (row < conditions.size()) {
                admin.runtime().remove(who, conditions.get(row));
                return true;
            }
            return false;
        }
        List<StatDefinition> stats = stats();
        if (row >= stats.size()) {
            return false;
        }
        StatDefinition stat = stats.get(row);
        switch (col) {
            case 2 -> admin.engine().adjust(who, stat.id(), -10);
            case 3 -> admin.engine().adjust(who, stat.id(), 10);
            case 5 -> admin.engine().set(who, stat.id(), stat.max());
            case 6 -> admin.engine().set(who, stat.id(), 0);
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public void handlePlayerInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
