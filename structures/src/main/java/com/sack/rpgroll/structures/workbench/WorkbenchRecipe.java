package com.sack.rpgroll.structures.workbench;

import com.sack.rpgroll.structures.StructuresPlugin;
import com.sack.rpgroll.structures.config.WorkbenchSettings;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Receta de la mesa Constructor. El resultado registrado y el que se ve al preparar es una MUESTRA con firma
 * inválida: nunca pasa {@link WorkbenchItems#isGenuine}, así que aunque saliera de la rejilla por otro camino
 * no se podría colocar. Solo {@link CraftItemEvent} la cambia por una mesa recién emitida (id y serie propios).
 */
public final class WorkbenchRecipe implements Listener {

    private static final WorkbenchStamp SAMPLE =
            new WorkbenchStamp(new UUID(0, 0), WorkbenchTier.CONSTRUCTOR, 0, "muestra");

    private final StructuresPlugin plugin;
    private final NamespacedKey key;

    public WorkbenchRecipe(StructuresPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "workbench");
    }

    /** {@link #problem(List, Map, Predicate)} sin mirar remanentes. */
    public static Optional<String> problem(List<String> shape, Map<Character, String> keys) {
        return problem(shape, keys, id -> false);
    }

    /**
     * Vacío si la forma y las claves casan; si no, el motivo. Cada letra de la forma necesita clave, cada clave se
     * usa y ningún ingrediente deja remanente ({@code leavesRemainder}: cubos, botellas...). Mayús+clic fabrica a
     * mano y no devolvería el cubo vacío.
     */
    public static Optional<String> problem(List<String> shape, Map<Character, String> keys,
                                           Predicate<String> leavesRemainder) {
        if (shape.isEmpty() || shape.size() > 3) {
            return Optional.of("la forma debe tener de 1 a 3 filas");
        }
        Set<Character> used = new HashSet<>();
        for (String row : shape) {
            if (row.isEmpty() || row.length() > 3 || row.length() != shape.get(0).length()) {
                return Optional.of("las filas deben medir lo mismo, de 1 a 3 caracteres");
            }
            for (char c : row.toCharArray()) {
                if (c == ' ') {
                    continue;
                }
                if (!keys.containsKey(c)) {
                    return Optional.of("la letra '" + c + "' de la forma no tiene clave");
                }
                used.add(c);
            }
        }
        for (Map.Entry<Character, String> e : keys.entrySet()) {
            if (!used.contains(e.getKey())) {
                return Optional.of("la clave '" + e.getKey() + "' no se usa en la forma");
            }
            if (leavesRemainder.test(e.getValue())) {
                return Optional.of("la clave '" + e.getKey() + "' (" + e.getValue()
                        + ") deja un recipiente al fabricar; usa otro ingrediente");
            }
        }
        return Optional.empty();
    }

    /** Quita la receta anterior y registra la de los ajustes actuales; si no es válida, avisa y no registra. */
    public void reload() {
        plugin.getServer().removeRecipe(key);
        WorkbenchSettings ws = plugin.workbench();
        Optional<String> bad = problem(ws.recipeShape(), ws.recipeKeys(), id -> {
            Material m = Material.matchMaterial(id);
            return m != null && m.isItem() && m.getCraftingRemainingItem() != null;
        });
        if (bad.isPresent()) {
            warn(bad.get());
            return;
        }
        ShapedRecipe recipe = new ShapedRecipe(key, sample());
        try {
            recipe.shape(ws.recipeShape().toArray(String[]::new));
            for (Map.Entry<Character, String> e : ws.recipeKeys().entrySet()) {
                Material m = Material.matchMaterial(e.getValue());
                if (m == null || !m.isItem()) {
                    warn("la clave '" + e.getKey() + "' apunta a '" + e.getValue() + "', que no es un ítem");
                    return;
                }
                recipe.setIngredient(e.getKey(), m);
            }
        } catch (IllegalArgumentException ex) {
            warn(ex.getMessage());
            return;
        }
        plugin.getServer().addRecipe(recipe);
    }

    public void unregister() {
        plugin.getServer().removeRecipe(key);
    }

    private void warn(String reason) {
        plugin.getLogger().warning(plugin.lang().raw("workbench.warning.recipe-invalid", "reason", reason));
    }

    private ItemStack sample() {
        return plugin.items().sample(SAMPLE, plugin.lang(), plugin.workbench().tiers().get(SAMPLE.tier()));
    }

    private boolean ours(Recipe r) {
        return r instanceof Keyed k && k.getKey().equals(key);
    }

    @EventHandler
    public void onPrepare(PrepareItemCraftEvent e) {
        if (ours(e.getRecipe())) {
            e.getInventory().setResult(sample());
        }
    }

    /** Qué hacer con un clic sobre nuestro resultado. */
    public enum Action {
        /** Clic izquierdo/derecho con el cursor vacío: vanilla entrega el ítem que se pone en el evento. */
        NORMAL,
        /** Mayús+clic: se fabrica una sola, a mano. */
        ONE_BY_ONE,
        /** Cualquier otro (tecla numérica, soltar, doble clic, central, cursor ocupado...): no se fabrica. */
        CANCEL
    }

    /**
     * Solo estos clics garantizan una unidad por evento. Los demás (Ctrl+Q repite la toma, doble clic recoge
     * varias, tecla numérica/soltar/creativo se comportan distinto) darían una mesa firmada y el resto muestras
     * que gastan ingredientes; se cancelan sin gastar ingredientes ni serie.
     */
    public static Action classify(ClickType click, boolean cursorEmpty) {
        return switch (click) {
            case SHIFT_LEFT, SHIFT_RIGHT -> Action.ONE_BY_ONE;
            case LEFT, RIGHT -> cursorEmpty ? Action.NORMAL : Action.CANCEL;
            default -> Action.CANCEL;
        };
    }

    /** El Crafter automático fabricaría muestras en bucle. */
    @EventHandler
    public void onCrafter(CrafterCraftEvent e) {
        if (ours(e.getRecipe())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent e) {
        if (!ours(e.getRecipe()) || !(e.getWhoClicked() instanceof Player p)) {
            return;
        }
        Action action = classify(e.getClick(), e.getCursor().getType().isAir());
        // Antes de gastar una serie: un clic que no se atiende no cuesta nada.
        if (action == Action.CANCEL || (action == Action.ONE_BY_ONE && p.getInventory().firstEmpty() == -1)) {
            e.setCancelled(true);
            return;
        }
        Optional<ItemStack> bench = plugin.newBench(WorkbenchTier.CONSTRUCTOR);
        if (bench.isEmpty()) {
            e.setCancelled(true); // ingredientes intactos
            plugin.lang().send(p, "recipe.busy");
            return;
        }
        if (action == Action.NORMAL) {
            e.setCurrentItem(bench.get());
            return;
        }
        e.setCancelled(true);
        CraftingInventory grid = e.getInventory();
        ItemStack[] matrix = grid.getMatrix();
        // Sin recipientes que devolver: reload() rechaza las recetas con ingredientes que dejan remanente.
        for (int i = 0; i < matrix.length; i++) {
            if (matrix[i] != null) {
                if (matrix[i].getAmount() <= 1) {
                    matrix[i] = null;
                } else {
                    matrix[i].setAmount(matrix[i].getAmount() - 1);
                }
            }
        }
        grid.setMatrix(matrix);
        p.getInventory().addItem(bench.get());
        p.updateInventory();
    }
}
