package com.sack.rpgroll.recipes.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.recipe.RecipeEntry;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.recipes.index.IndexService;
import com.sack.rpgroll.recipes.index.RecipeIndex;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Abre las pantallas del recetario y recuerda por dónde vino cada jugador, para que "Volver"
 * deshaga el último clic como en JEI (catálogo → espada → lingote → ...).
 */
public final class Viewer implements Listener {

    public enum Mode { MAKE, USES }

    private static final int MAX_HISTORY = 30;

    private final Plugin plugin;
    private final LangManager lang;
    private final IndexService indexes;
    private final SearchPrompt prompt;
    private final Map<UUID, Deque<InventoryGUI>> history = new HashMap<>();

    public Viewer(Plugin plugin, LangManager lang, IndexService indexes, SearchPrompt prompt) {
        this.plugin = plugin;
        this.lang = lang;
        this.indexes = indexes;
        this.prompt = prompt;
    }

    public Plugin plugin() {
        return plugin;
    }

    public LangManager lang() {
        return lang;
    }

    public IndexService indexes() {
        return indexes;
    }

    public SearchPrompt prompt() {
        return prompt;
    }

    /** Catálogo desde cero (borra el historial). */
    public void openCatalog(Player player, String query) {
        RecipeIndex index = indexes.current();
        Deque<InventoryGUI> stack = stack(player);
        stack.clear();
        push(player, new CatalogGUI(player, this, index, query));
    }

    /**
     * Recetas de un ítem. Desde el comando empieza un historial nuevo; desde el menú se apila.
     *
     * @return false si no hay ninguna (ya se le avisó al jugador)
     */
    public boolean showRecipes(Player player, ItemStack item, Mode mode, boolean fromMenu) {

        if (item == null || item.getType().isAir()) {
            lang.send(player, "msg.empty_hand");
            return false;
        }

        RecipeIndex index = indexes.current();
        List<RecipeEntry> recipes = mode == Mode.MAKE ? index.howToMake(item, player) : index.usesOf(item, player);

        if (recipes.isEmpty()) {
            lang.send(player, mode == Mode.MAKE ? "msg.no_recipes" : "msg.no_uses");
            player.playSound(player, Sound.ENTITY_VILLAGER_NO, 0.4f, 1.2f);
            return false;
        }

        if (!fromMenu) {
            stack(player).clear();
        }
        push(player, new RecipeGUI(player, this, item, mode, recipes));
        return true;
    }

    public void push(Player player, InventoryGUI gui) {
        Deque<InventoryGUI> stack = stack(player);
        stack.push(gui);
        while (stack.size() > MAX_HISTORY) {
            stack.removeLast();
        }
        gui.open();
    }

    /** Vuelve a la pantalla anterior; sin anterior, abre el catálogo. */
    public void back(Player player) {
        Deque<InventoryGUI> stack = stack(player);
        stack.poll();
        InventoryGUI previous = stack.peek();
        if (previous != null) {
            previous.open();
        } else {
            openCatalog(player, "");
        }
    }

    public boolean hasBack(Player player) {
        return stack(player).size() > 1;
    }

    public void click(Player player) {
        player.playSound(player, Sound.UI_BUTTON_CLICK, 0.3f, 1.4f);
    }

    private Deque<InventoryGUI> stack(Player player) {
        return history.computeIfAbsent(player.getUniqueId(), id -> new ArrayDeque<>());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        history.remove(event.getPlayer().getUniqueId());
    }
}
