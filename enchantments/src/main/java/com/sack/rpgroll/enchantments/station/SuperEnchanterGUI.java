package com.sack.rpgroll.enchantments.station;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.enchantments.station.EnchantPlan.Entry;
import com.sack.rpgroll.enchantments.station.EnchantPlan.Kind;
import com.sack.rpgroll.enchantments.util.RomanNumerals;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.MenuBackground;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Súper encantador al estilo de una lista con barras: arriba el ítem de la mano; por fila un
 * encantamiento (libro) y sus niveles I–VIII; abajo páginas, coste, Encantar y Deshacer. Nada se cobra
 * hasta Encantar.
 */
final class SuperEnchanterGUI extends InventoryGUI {

    /** Cómo se pinta una casilla de nivel. */
    enum Cell { HAVE, ADD, REMOVE, FREE, BLOCKED, NONE }

    private static final int ITEM = 4;
    private static final int CLOSE = 8;
    private static final int ROWS = 4;
    // shortcut: 8 casillas de nivel por fila (el tope más alto de hoy es VIII); por encima se sube con el libro.
    private static final int LEVELS = 8;
    private static final int PREV = 45;
    private static final int XP = 47;
    private static final int LAPIS = 48;
    private static final int ENCHANT = 49;
    private static final int RESET = 51;
    private static final int NEXT = 53;

    private final SuperEnchanter enchanter;
    private final LangManager lang;
    private final String pack;
    private ItemStack held;
    private EnchantPlan plan;
    private int page;

    SuperEnchanterGUI(Player player, SuperEnchanter enchanter, LangManager lang) {
        super(player, MenuBackground.title(enchanter.menuBackground(), lang.component("station.super.title")), 54);
        this.enchanter = enchanter;
        this.lang = lang;
        String bg = enchanter.menuBackground().trim();
        // Los modelos de las casillas van junto al fondo: "sackito:menus/super_encantador" → "sackito:menus".
        this.pack = MenuBackground.active(bg) && bg.contains("/") ? bg.substring(0, bg.lastIndexOf('/')) : "";
        reload();
    }

    /** Estado de la casilla del nivel {@code i} para un encantamiento que tiene {@code current} y va a {@code target}. */
    static Cell cell(int i, int current, int target, int cap, boolean blocked) {
        if (i > cap) {
            return Cell.NONE;
        }
        if (blocked) {
            return Cell.BLOCKED;
        }
        if (i <= Math.min(current, target)) {
            return Cell.HAVE;
        }
        if (i <= target) {
            return Cell.ADD;
        }
        return i <= current ? Cell.REMOVE : Cell.FREE;
    }

    /** Clic en el nivel {@code level}: lo elige; si ya era el elegido, baja uno (así se llega a quitarlo). */
    static int clicked(int level, int target) {
        return level == target ? level - 1 : level;
    }

    private void reload() {
        held = player.getInventory().getItemInMainHand().clone();
        plan = enchanter.plan(held);
        page = Math.min(page, Math.max(0, pages() - 1));
    }

    private int pages() {
        return Math.max(1, (plan.editable().size() + ROWS - 1) / ROWS);
    }

    private static Component plain(Component c) {
        return c.decoration(TextDecoration.ITALIC, false);
    }

    private ItemStack named(Material material, String model, Component name, List<Component> lore) {
        NamespacedKey key = pack.isEmpty() || model == null ? null : NamespacedKey.fromString(pack + "/" + model);
        ItemStack item = ItemStack.of(key == null ? material : Material.PAPER);
        var meta = item.getItemMeta();
        if (key != null) {
            meta.setItemModel(key);
        }
        meta.displayName(plain(name));
        meta.lore(lore.stream().map(SuperEnchanterGUI::plain).toList());
        item.setItemMeta(meta);
        return item;
    }

    /** Botón cuyo dibujo ya está en el fondo: invisible con fondo, el ítem de siempre sin él. */
    private ItemStack button(Material plainMaterial, Component name, List<Component> lore) {
        return named(plainMaterial, "invisible", name, lore);
    }

    private static String roman(int n) {
        return n <= 0 ? "—" : RomanNumerals.of(n);
    }

    private Component names(List<String> ids) {
        Component out = Component.empty();
        for (int i = 0; i < ids.size(); i++) {
            Entry e = plan.entry(ids.get(i));
            out = out.append(i == 0 ? Component.empty() : Component.text(", ", NamedTextColor.GRAY))
                    .append(e == null ? Component.text(ids.get(i)) : enchanter.name(e));
        }
        return out;
    }

    @Override
    public void build() {
        clear();
        setItem(ITEM, held.clone());
        setItem(CLOSE, button(Material.BARRIER, lang.component("station.close"), List.of()));

        List<Entry> list = plan.editable();
        if (list.isEmpty()) {
            setItem(22, named(Material.BOOK, null, lang.component("station.super.nothing"),
                    List.of(lang.component("station.super.nothing_hint"))));
        }
        for (int r = 0; r < ROWS; r++) {
            int index = page * ROWS + r;
            if (index >= list.size()) {
                break;
            }
            drawRow(9 * (r + 1), list.get(index));
        }

        EnchantPlan.Cost cost = plan.cost();
        boolean changed = !plan.changes().isEmpty();
        boolean canPay = enchanter.canPay(player, cost);
        ItemStack xp = named(Material.EXPERIENCE_BOTTLE, null,
                lang.component("station.super.cost_xp_name", "levels", cost.xp()),
                List.of(lang.component("station.super.you_have_xp", "levels", player.getLevel())));
        xp.setAmount(Math.max(1, Math.min(64, cost.xp())));
        setItem(XP, xp);
        ItemStack lapis = named(Material.LAPIS_LAZULI, null,
                lang.component("station.super.cost_lapis_name", "amount", cost.lapis()), List.of());
        lapis.setAmount(Math.max(1, Math.min(64, cost.lapis())));
        setItem(LAPIS, lapis);
        setItem(ENCHANT, button(Material.ENCHANTING_TABLE, lang.component("station.super.enchant"),
                List.of(lang.component(!changed ? "station.super.enchant_nothing"
                        : canPay ? "station.click" : "station.super.cannot_pay"))));
        if (changed) {
            setItem(RESET, button(Material.WATER_BUCKET, lang.component("station.super.reset"), List.of()));
        }
        Component pageLine = lang.component("station.super.page", "page", page + 1, "pages", pages());
        if (page > 0) {
            setItem(PREV, button(Material.ARROW, lang.component("station.super.prev"), List.of(pageLine)));
        }
        if (page < pages() - 1) {
            setItem(NEXT, button(Material.ARROW, lang.component("station.super.next"), List.of(pageLine)));
        }
    }

    private void drawRow(int first, Entry e) {
        int target = plan.target(e.id());
        boolean blocked = plan.blocked(e.id());
        Component name = enchanter.name(e).color(e.kind() == Kind.CUSTOM ? e.rarity().color() : NamedTextColor.LIGHT_PURPLE);

        List<Component> lore = new ArrayList<>();
        lore.add(e.kind() == Kind.CUSTOM
                ? lang.component("station.super.tag_plugin", "rarity", e.rarity().name().toLowerCase())
                : lang.component("station.super.tag_vanilla"));
        lore.add(lang.component("station.super.level_line", "from", roman(e.current()), "to", roman(target),
                "cap", roman(e.cap())));
        if (blocked) {
            lore.add(lang.component("station.super.blocked_by").append(names(plan.blockers(e.id()))));
        }
        lore.add(lang.component("station.super.book_hint"));
        setItem(first, named(blocked ? Material.BOOK : Material.ENCHANTED_BOOK, null, name, lore));

        for (int i = 1; i <= LEVELS; i++) {
            Cell state = cell(i, e.current(), target, e.cap(), blocked);
            if (state == Cell.NONE) {
                continue;
            }
            String key = switch (state) {
                case HAVE -> "have";
                case ADD -> "add";
                case REMOVE -> "remove";
                case BLOCKED -> "blocked";
                default -> "free";
            };
            Material glass = switch (state) {
                case HAVE -> Material.LIME_STAINED_GLASS_PANE;
                case ADD -> Material.YELLOW_STAINED_GLASS_PANE;
                case REMOVE -> Material.RED_STAINED_GLASS_PANE;
                case BLOCKED -> Material.GRAY_STAINED_GLASS_PANE;
                default -> Material.LIGHT_GRAY_STAINED_GLASS_PANE;
            };
            String model = switch (state) {
                case HAVE -> "enc_tiene_" + i;
                case ADD -> "enc_poner_" + i;
                case REMOVE -> "enc_quitar_" + i;
                case BLOCKED -> "enc_bloqueado";
                default -> "enc_libre_" + i;
            };
            setItem(first + i, named(glass, model,
                    enchanter.name(e).color(NamedTextColor.WHITE).append(Component.text(" " + roman(i), NamedTextColor.WHITE)),
                    List.of(lang.component("station.super.cell_" + key))));
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        switch (slot) {
            case CLOSE -> {
                close();
                return;
            }
            case PREV -> page = Math.max(0, page - 1);
            case NEXT -> page = Math.min(pages() - 1, page + 1);
            case RESET -> plan.reset();
            case ENCHANT -> {
                String result = enchanter.apply(player, held, plan);
                player.sendActionBar(lang.component(result));
                if (result.endsWith("done")) {
                    reload();
                } else {
                    player.playSound(player, Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
                    if (result.equals("station.changed")) {
                        reload();
                    }
                }
            }
            default -> {
                int row = slot / 9 - 1;
                int col = slot % 9;
                int index = page * ROWS + row;
                List<Entry> list = plan.editable();
                if (row < 0 || row >= ROWS || index >= list.size()) {
                    return;
                }
                Entry e = list.get(index);
                int target = plan.target(e.id());
                int wanted = col == 0 ? target + (event.isRightClick() ? -1 : 1) : clicked(col, target);
                if (col > e.cap() || !plan.set(e.id(), wanted)) {
                    player.playSound(player, Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
                    return;
                }
                player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5f, 1.4f);
            }
        }
        build();
    }

    @Override
    public void handlePlayerInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
