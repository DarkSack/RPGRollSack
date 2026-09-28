package com.sack.rpgroll.recipes.read;

import com.sack.rpgroll.common.recipe.RecipeEntry;
import com.sack.rpgroll.common.recipe.RecipeSlot;
import com.sack.rpgroll.common.recipe.RecipeStation;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * La fermentación vanilla. Bukkit no tiene una lista de mezclas, así que se leen del propio
 * servidor ({@code PotionBrewing} de Minecraft, por reflexión: si una versión nueva añade una
 * poción, sale sola). Si esa lectura falla se usa una tabla fija de las mezclas de siempre.
 * <p>
 * Las mezclas que añaden otros plugins con {@code PotionBrewer#addPotionMix} no se pueden
 * listar: Paper las guarda como predicados, sin los ítems. Esos plugins pueden publicar un
 * {@code RecipeSource} o declararlas en {@code extra/}.
 */
public final class BrewingReader {

    /** Una mezcla: claves de registro ({@code minecraft:swiftness}, {@code minecraft:sugar}). */
    record Mix(String from, List<String> ingredients, String to) {
    }

    public record Result(List<RecipeEntry> entries, boolean fromServer) {
    }

    private static final Pattern RESOURCE_KEY = Pattern.compile("/ ([a-z0-9_.\\-]+:[a-z0-9_./\\-]+)]");

    private final Logger logger;
    private final Notes notes;

    public BrewingReader(Logger logger, Notes notes) {
        this.logger = logger;
        this.notes = notes;
    }

    public Result read() {

        List<Mix> potionMixes = List.of();
        List<Mix> containerMixes = List.of();
        boolean fromServer = false;

        try {
            Object brewing = serverBrewing();
            potionMixes = mixes(brewing, "potionMixes");
            containerMixes = mixes(brewing, "containerMixes");
            fromServer = !potionMixes.isEmpty();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            logger.info("Fermentación: no se pudo leer del servidor (" + e.getClass().getSimpleName()
                    + "), se usa la tabla de siempre.");
        }

        if (!fromServer) {
            potionMixes = FallbackMixes.POTIONS;
            containerMixes = FallbackMixes.CONTAINERS;
        } else {
            reportTableDrift(potionMixes);
        }

        return new Result(entries(potionMixes, containerMixes), fromServer);
    }

    /**
     * Avisa si la tabla fija se quedó atrás respecto a la versión del servidor (una poción nueva,
     * un ingrediente cambiado): así se nota al actualizar Minecraft, no el día que falle la lectura.
     */
    private void reportTableDrift(List<Mix> serverMixes) {
        Set<String> server = new LinkedHashSet<>();
        for (Mix mix : serverMixes) {
            for (String ingredient : mix.ingredients()) {
                server.add(path(mix.from()) + " " + path(ingredient) + " " + path(mix.to()));
            }
        }
        Set<String> table = new LinkedHashSet<>();
        for (Mix mix : FallbackMixes.POTIONS) {
            table.add(path(mix.from()) + " " + path(mix.ingredients().getFirst()) + " " + path(mix.to()));
        }
        Set<String> missing = new LinkedHashSet<>(server);
        missing.removeAll(table);
        Set<String> stale = new LinkedHashSet<>(table);
        stale.removeAll(server);
        if (!missing.isEmpty() || !stale.isEmpty()) {
            logger.info("Fermentación: la tabla fija de respaldo no coincide con este servidor. Faltan "
                    + missing + "; sobran " + stale + ".");
        }
    }

    // --- Lectura del servidor ---

    private static Object serverBrewing() throws ReflectiveOperationException {
        Object craftServer = Bukkit.getServer();
        Object minecraft = craftServer.getClass().getMethod("getServer").invoke(craftServer);
        return minecraft.getClass().getMethod("potionBrewing").invoke(minecraft);
    }

    private static List<Mix> mixes(Object brewing, String fieldName) throws ReflectiveOperationException {

        Field field = findField(brewing.getClass(), fieldName);
        field.setAccessible(true);
        List<Mix> result = new ArrayList<>();

        for (Object mix : (Collection<?>) field.get(brewing)) {
            String from = keyOf(call(mix, "from"));
            String to = keyOf(call(mix, "to"));
            List<String> ingredients = itemKeys(call(mix, "ingredient"));
            if (from != null && to != null && !ingredients.isEmpty()) {
                result.add(new Mix(from, ingredients, to));
            }
        }

        return result;
    }

    private static List<String> itemKeys(Object ingredient) throws ReflectiveOperationException {

        Object items;
        try {
            items = call(ingredient, "items");
        } catch (NoSuchMethodException e) {
            Field values = findField(ingredient.getClass(), "values");
            values.setAccessible(true);
            items = values.get(ingredient);
        }

        List<String> keys = new ArrayList<>();
        Iterable<?> iterable = items instanceof Stream<?> stream ? stream.toList() : (Iterable<?>) items;
        for (Object holder : iterable) {
            String key = keyOf(holder);
            if (key != null) {
                keys.add(key);
            }
        }
        return keys;
    }

    /** {@code Reference{ResourceKey[minecraft:potion / minecraft:swiftness]=...}} → {@code minecraft:swiftness}. */
    static String keyOf(Object holder) {
        if (holder == null) {
            return null;
        }
        Matcher matcher = RESOURCE_KEY.matcher(holder.toString());
        return matcher.find() ? matcher.group(1) : null;
    }

    private static Object call(Object target, String method) throws ReflectiveOperationException {
        Method m = findMethod(target.getClass(), method);
        m.setAccessible(true);
        return m.invoke(target);
    }

    private static Method findMethod(Class<?> type, String name) throws NoSuchMethodException {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == 0) {
                    return m;
                }
            }
        }
        throw new NoSuchMethodException(type.getName() + "#" + name);
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getName().equals(name)) {
                    return f;
                }
            }
        }
        throw new NoSuchFieldException(type.getName() + "#" + name);
    }

    // --- Recetas ---

    private List<RecipeEntry> entries(List<Mix> potionMixes, List<Mix> containerMixes) {

        RecipeStation station = RecipeStation.vanilla(RecipeStation.BREWING, Material.BREWING_STAND);
        List<RecipeEntry> entries = new ArrayList<>();
        Set<PotionType> brewable = new LinkedHashSet<>();

        for (Mix mix : potionMixes) {
            PotionType from = potionType(mix.from());
            PotionType to = potionType(mix.to());
            if (from == null || to == null) {
                continue;
            }
            brewable.add(from);
            brewable.add(to);
            entries.add(RecipeEntry.builder(id(mix), station).source("Minecraft")
                    .input(ingredient(mix.ingredients()))
                    .input(potion(Material.POTION, from))
                    .output(potion(Material.POTION, to))
                    .note(notes.brewing())
                    .build());
        }

        // Arrojadizas y persistentes: una por cada poción que se puede fermentar.
        for (Mix mix : containerMixes) {
            Material from = Material.matchMaterial(mix.from());
            Material to = Material.matchMaterial(mix.to());
            if (from == null || to == null) {
                continue;
            }
            for (PotionType type : brewable) {
                entries.add(RecipeEntry.builder(id(mix) + "/" + type.getKey().getKey(), station).source("Minecraft")
                        .input(ingredient(mix.ingredients()))
                        .input(potion(from, type))
                        .output(potion(to, type))
                        .note(notes.brewing())
                        .build());
            }
        }

        return entries;
    }

    private static String id(Mix mix) {
        return "minecraft:brewing/" + path(mix.from()) + "+" + path(mix.ingredients().getFirst()) + "=" + path(mix.to());
    }

    private static String path(String key) {
        int colon = key.indexOf(':');
        return colon < 0 ? key : key.substring(colon + 1);
    }

    private static RecipeSlot ingredient(List<String> keys) {
        List<ItemStack> options = new ArrayList<>();
        for (String key : keys) {
            Material material = Material.matchMaterial(key);
            if (material != null && material.isItem()) {
                options.add(ItemStack.of(material));
            }
        }
        return RecipeSlot.of(options);
    }

    private static PotionType potionType(String key) {
        NamespacedKey namespaced = NamespacedKey.fromString(key);
        return namespaced == null ? null : Registry.POTION.get(namespaced);
    }

    static ItemStack potion(Material container, PotionType type) {
        ItemStack stack = ItemStack.of(container);
        if (stack.getItemMeta() instanceof PotionMeta meta) {
            meta.setBasePotionType(type);
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
