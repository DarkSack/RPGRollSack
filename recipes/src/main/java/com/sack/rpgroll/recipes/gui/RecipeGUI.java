package com.sack.rpgroll.recipes.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.recipe.RecipeEntry;
import com.sack.rpgroll.common.recipe.RecipeSlot;
import com.sack.rpgroll.gui.InventoryGUI;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Una receta a la vez, con su estación, sus notas y flechas para pasar a la siguiente. Los
 * huecos con varias opciones (cualquier tabla, cualquier lana) van rotando cada segundo. Clic
 * en cualquier ingrediente o resultado = sus recetas (derecho: para qué sirve).
 */
final class RecipeGUI extends InventoryGUI {

    private static final int[] GRID = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final int[] OUTPUTS = {25, 16, 34};
    private static final int FOCUS = 4;
    private static final int STATION = 14;
    private static final int ARROW = 23;
    private static final int BACK = 45;
    private static final int SWITCH = 47;
    private static final int PREV = 48;
    private static final int COUNTER = 49;
    private static final int NEXT = 50;
    private static final int CLOSE = 53;

    private final Viewer viewer;
    private final LangManager lang;
    private final ItemStack focus;
    private final Viewer.Mode mode;
    private final List<RecipeEntry> recipes;
    private int position;

    /** Lo que hay en cada hueco de ingredientes/resultados (sin retocar) y lo que rota. */
    private final Map<Integer, ItemStack> shown = new HashMap<>();
    private final Map<Integer, List<ItemStack>> cycling = new HashMap<>();
    private BukkitTask task;
    private int tick;

    RecipeGUI(Player player, Viewer viewer, ItemStack focus, Viewer.Mode mode,
            List<RecipeEntry> recipes) {
        super(player, title(viewer.lang(), focus, mode), 54);
        this.viewer = viewer;
        this.lang = viewer.lang();
        this.focus = focus.asOne();
        this.mode = mode;
        this.recipes = recipes;
    }

    private static Component title(LangManager lang, ItemStack focus, Viewer.Mode mode) {
        String key = mode == Viewer.Mode.MAKE ? "gui.recipe.title_make" : "gui.recipe.title_uses";
        return Buttons.text(lang.raw(key)).append(focus.effectiveName().colorIfAbsent(NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
    }

    @Override
    public void open() {
        super.open();
        startCycling();
    }

    @Override
    public void build() {

        clear();
        shown.clear();
        cycling.clear();

        ItemStack filler = Buttons.filler();
        for (int slot = 0; slot < 54; slot++) {
            setItem(slot, filler);
        }

        RecipeEntry recipe = recipes.get(position);
        for (int slot : GRID) {
            setItem(slot, null);
        }
        for (int slot : OUTPUTS) {
            setItem(slot, null);
        }

        setItem(FOCUS, Buttons.withLore(focus, Buttons.lines("\n" + lang.raw(mode == Viewer.Mode.MAKE
                ? "gui.recipe.focus_make" : "gui.recipe.focus_uses"))));

        placeInputs(recipe);

        List<ItemStack> outputs = recipe.outputs();
        for (int i = 0; i < outputs.size() && i < OUTPUTS.length; i++) {
            place(OUTPUTS[i], List.of(outputs.get(i)));
        }

        List<Component> stationLore = new ArrayList<>();
        stationLore.addAll(Buttons.lines(lang.raw("gui.recipe.source", "value", recipe.source())));
        if (player.hasPermission("rpgrollrecipes.admin")) {
            stationLore.addAll(Buttons.lines(lang.raw("gui.recipe.id", "value", recipe.id())));
        }
        setItem(STATION, Buttons.of(recipe.station().icon().getType(),
                viewer.indexes().displayName(recipe.station()).colorIfAbsent(NamedTextColor.GOLD)
                        .decoration(TextDecoration.ITALIC, false), stationLore));

        List<Component> notes = new ArrayList<>();
        for (String note : recipe.notes()) {
            notes.addAll(Buttons.lines(note));
        }
        setItem(ARROW, Buttons.of(Material.SPECTRAL_ARROW, Buttons.text(lang.raw(recipe.shaped()
                ? "gui.recipe.arrow_shaped" : "gui.recipe.arrow_shapeless")), notes));

        setItem(BACK, Buttons.of(lang, Material.ARROW, "gui.recipe.back"));
        setItem(SWITCH, Buttons.of(lang, mode == Viewer.Mode.MAKE ? Material.HOPPER : Material.CRAFTING_TABLE,
                mode == Viewer.Mode.MAKE ? "gui.recipe.switch_uses" : "gui.recipe.switch_make"));
        if (position > 0) {
            setItem(PREV, Buttons.of(lang, Material.ARROW, "gui.recipe.prev"));
        }
        setItem(COUNTER, Buttons.of(lang, Material.PAPER, "gui.recipe.counter",
                "n", position + 1, "total", recipes.size()));
        if (position < recipes.size() - 1) {
            setItem(NEXT, Buttons.of(lang, Material.ARROW, "gui.recipe.next"));
        }
        setItem(CLOSE, Buttons.of(lang, Material.BARRIER, "gui.close"));
    }

    private void placeInputs(RecipeEntry recipe) {

        List<RecipeSlot> inputs = recipe.inputs();

        if (recipe.shaped()) {
            int width = recipe.width();
            for (int i = 0; i < inputs.size(); i++) {
                int row = i / width;
                int col = i % width;
                if (row < 3) {
                    place(GRID[row * 3 + col], inputs.get(i).options());
                }
            }
            return;
        }

        boolean overflow = inputs.size() > GRID.length;
        int visible = overflow ? GRID.length - 1 : inputs.size();
        for (int i = 0; i < visible; i++) {
            place(GRID[i], inputs.get(i).options());
        }

        if (overflow) {
            List<Component> rest = new ArrayList<>();
            for (int i = visible; i < inputs.size(); i++) {
                List<ItemStack> options = inputs.get(i).options();
                if (!options.isEmpty()) {
                    ItemStack first = options.getFirst();
                    rest.add(Component.text(first.getAmount() + "× ", NamedTextColor.GRAY)
                            .append(first.effectiveName().colorIfAbsent(NamedTextColor.WHITE))
                            .decoration(TextDecoration.ITALIC, false));
                }
            }
            setItem(GRID[GRID.length - 1], Buttons.of(Material.BUNDLE,
                    Buttons.text(lang.raw("gui.recipe.more", "count", inputs.size() - visible)), rest));
        }
    }

    private void place(int slot, List<ItemStack> options) {
        if (options.isEmpty()) {
            return;
        }
        ItemStack current = options.get(tick % options.size());
        shown.put(slot, current);
        setItem(slot, current);
        if (options.size() > 1) {
            cycling.put(slot, options);
        }
    }

    private void startCycling() {
        if (task != null) {
            task.cancel();
        }
        task = viewer.plugin().getServer().getScheduler().runTaskTimer(viewer.plugin(), () -> {
            if (!player.isOnline() || player.getOpenInventory().getTopInventory() != inventory) {
                task.cancel();
                task = null;
                return;
            }
            if (cycling.isEmpty()) {
                return;
            }
            tick++;
            cycling.forEach((slot, options) -> {
                ItemStack current = options.get(tick % options.size());
                shown.put(slot, current);
                setItem(slot, current);
            });
        }, 20L, 20L);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getRawSlot();
        boolean right = event.getClick().isRightClick();

        ItemStack item = shown.get(slot);
        if (item != null) {
            viewer.click(player);
            viewer.showRecipes(player, item, right ? Viewer.Mode.USES : Viewer.Mode.MAKE, true);
            return;
        }

        switch (slot) {
            case FOCUS, SWITCH -> viewer.showRecipes(player, focus,
                    mode == Viewer.Mode.MAKE ? Viewer.Mode.USES : Viewer.Mode.MAKE, true);
            case BACK -> {
                viewer.click(player);
                viewer.back(player);
            }
            case PREV -> move(-1);
            case NEXT -> move(1);
            case CLOSE -> close();
            default -> {
            }
        }
    }

    @Override
    public void handlePlayerInventoryClick(InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked != null && !clicked.getType().isAir()) {
            viewer.showRecipes(player, clicked, event.getClick().isRightClick() ? Viewer.Mode.USES : Viewer.Mode.MAKE,
                    true);
        }
    }

    private void move(int delta) {
        int next = position + delta;
        if (next >= 0 && next < recipes.size()) {
            position = next;
            viewer.click(player);
            build();
        }
    }
}
