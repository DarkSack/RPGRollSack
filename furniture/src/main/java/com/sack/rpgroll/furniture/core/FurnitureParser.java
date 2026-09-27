package com.sack.rpgroll.furniture.core;

import com.sack.rpgroll.furniture.function.FurnitureFunctions;
import com.sack.rpgroll.furniture.function.FurnitureFunctions.Action;
import com.sack.rpgroll.furniture.function.FurnitureFunctions.Ambient;
import com.sack.rpgroll.furniture.function.FurnitureFunctions.Light;
import com.sack.rpgroll.furniture.function.FurnitureFunctions.Seat;
import com.sack.rpgroll.furniture.function.FurnitureFunctions.Shelf;
import com.sack.rpgroll.furniture.function.FurnitureFunctions.ShelfSlot;
import com.sack.rpgroll.furniture.function.FurnitureFunctions.State;
import com.sack.rpgroll.furniture.function.FurnitureFunctions.States;
import com.sack.rpgroll.furniture.function.FurnitureFunctions.Storage;
import com.sack.rpgroll.furniture.function.FurnitureFunctions.Trash;
import com.sack.rpgroll.furniture.function.FurnitureFunctions.Trigger;
import com.sack.rpgroll.furniture.function.FurnitureFunctions.Workstation;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Lee un mueble de su sección YAML. Un error de forma (modelo que falta, número que no lo
 * es) descarta el mueble entero con un aviso; un detalle opcional mal escrito se ignora con
 * otro aviso y el mueble se carga igual.
 */
public final class FurnitureParser {

    private static final Pattern ID = Pattern.compile("[a-z0-9_]+");

    private FurnitureParser() {
    }

    public static Optional<FurnitureDefinition> parse(String id, ConfigurationSection s, Consumer<String> warn) {

        String where = "Mueble '" + id + "'";

        if (!ID.matcher(id).matches()) {
            warn.accept("✘ " + where + ": el id solo admite minúsculas, números y _");
            return Optional.empty();
        }

        try {
            String model = s.getString("model", "");
            if (model.isBlank() || NamespacedKey.fromString(model) == null) {
                throw new IllegalArgumentException("falta 'model' (el item_model, p. ej. rpgroll_furniture:chair)");
            }

            Material material = material(s.getString("material", "PAPER"));
            Map<String, FurnitureVariant> variants = variants(s.getConfigurationSection("variants"), where, warn);

            FurnitureDefinition definition = new FurnitureDefinition(
                    id,
                    s.getString("category", "misc").toLowerCase(Locale.ROOT),
                    s.getString("name", id),
                    s.getStringList("lore"),
                    material,
                    model,
                    variants,
                    placement(s.getConfigurationSection("placement")),
                    hitbox(s.getConfigurationSection("hitbox")),
                    display(s.getConfigurationSection("display")),
                    functions(s, where, warn),
                    recipe(s.getConfigurationSection("recipe"), where, warn),
                    blankToNull(s.getString("permission")),
                    new FurnitureSounds(
                            s.getString("sounds.place", FurnitureSounds.WOOD.place()),
                            s.getString("sounds.break", FurnitureSounds.WOOD.breakSound())));

            return Optional.of(definition);

        } catch (IllegalArgumentException e) {
            warn.accept("✘ " + where + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    // ---------------------------------------------------------------- partes

    private static Map<String, FurnitureVariant> variants(ConfigurationSection s, String where,
            Consumer<String> warn) {

        Map<String, FurnitureVariant> out = new LinkedHashMap<>();
        if (s == null) {
            return out;
        }

        for (String key : s.getKeys(false)) {

            ConfigurationSection v = s.getConfigurationSection(key);
            String variantId = key.toLowerCase(Locale.ROOT);

            if (v == null || !ID.matcher(variantId).matches()) {
                warn.accept("✘ " + where + ": variante '" + key + "' inválida, se ignora");
                continue;
            }

            String model = v.getString("model", "");
            if (model.isBlank() || NamespacedKey.fromString(model) == null) {
                warn.accept("✘ " + where + ": la variante '" + key + "' no tiene 'model', se ignora");
                continue;
            }

            DyeColor dye = null;
            String dyeName = v.getString("dye");
            if (dyeName != null) {
                try {
                    dye = DyeColor.valueOf(dyeName.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    warn.accept("⚠ " + where + ": tinte '" + dyeName + "' desconocido en la variante '" + key + "'");
                }
            }

            out.put(variantId, new FurnitureVariant(variantId, blankToNull(v.getString("name")), model, dye,
                    recipe(v.getConfigurationSection("recipe"), where, warn)));
        }

        return out;
    }

    static Placement placement(ConfigurationSection s) {

        if (s == null) {
            return new Placement(Set.of(Surface.FLOOR), 4, 0);
        }

        Set<Surface> surfaces = EnumSet.noneOf(Surface.class);
        for (String name : s.getStringList("surfaces")) {
            surfaces.add(Surface.valueOf(name.toUpperCase(Locale.ROOT)));
        }
        if (surfaces.isEmpty()) {
            surfaces.add(Surface.FLOOR);
        }

        return new Placement(surfaces, s.getInt("rotations", 4), s.getInt("limit", 0));
    }

    static Hitbox hitbox(ConfigurationSection s) {

        if (s == null) {
            return Hitbox.barrier(List.of(Offset.ZERO));
        }

        Hitbox.Type type = Hitbox.Type.valueOf(s.getString("type", "barrier").toUpperCase(Locale.ROOT));

        if (type == Hitbox.Type.INTERACTION) {
            return Hitbox.interaction((float) s.getDouble("width", 1.0), (float) s.getDouble("height", 1.0));
        }

        List<Offset> blocks = new ArrayList<>();
        for (String text : s.getStringList("blocks")) {
            blocks.add(Offset.parse(text));
        }
        return Hitbox.barrier(blocks);
    }

    static DisplaySettings display(ConfigurationSection s) {

        if (s == null) {
            return DisplaySettings.DEFAULT;
        }

        return new DisplaySettings(
                (float) s.getDouble("scale", 1.0),
                Offset.parse(s.getString("translation", "0,0,0")),
                s.getInt("brightness", -1),
                (float) s.getDouble("view-range", 1.0),
                (float) s.getDouble("shadow", 0.0),
                ItemDisplayTransform.valueOf(s.getString("transform", "FIXED").toUpperCase(Locale.ROOT)));
    }

    static CarpenterRecipe recipe(ConfigurationSection s, String where, Consumer<String> warn) {

        if (s == null) {
            return null;
        }

        Map<Material, Integer> materials = new LinkedHashMap<>();
        ConfigurationSection m = s.getConfigurationSection("materials");

        if (m != null) {
            for (String key : m.getKeys(false)) {
                Material material = Material.matchMaterial(key);
                int amount = m.getInt(key, 0);
                if (material == null || air(material) || material.isLegacy() || amount <= 0) {
                    warn.accept("⚠ " + where + ": material de receta inválido '" + key + "', se ignora");
                    continue;
                }
                materials.merge(material, amount, Integer::sum);
            }
        }

        return new CarpenterRecipe(s.getString("station", CarpenterRecipe.DEFAULT_STATION).toLowerCase(Locale.ROOT),
                materials, s.getDouble("money", 0), s.getInt("amount", 1));
    }

    static FurnitureFunctions functions(ConfigurationSection s, String where, Consumer<String> warn) {

        return new FurnitureFunctions(
                seat(s.getConfigurationSection("seat")),
                storage(s.getConfigurationSection("storage")),
                light(s.getConfigurationSection("light")),
                states(s.getConfigurationSection("states")),
                workstation(s),
                shelf(s),
                trash(s.getConfigurationSection("trash")),
                ambient(s.getConfigurationSection("ambient"), s.getConfigurationSection("states"), where, warn),
                actions(s, where, warn));
    }

    private static Seat seat(ConfigurationSection s) {

        if (s == null) {
            return null;
        }

        List<Offset> positions = new ArrayList<>();
        for (String text : s.getStringList("positions")) {
            positions.add(Offset.parse(text));
        }
        return new Seat(positions, s.getDouble("height", 0.4));
    }

    private static Storage storage(ConfigurationSection s) {
        return s == null ? null : new Storage(s.getInt("rows", 3), s.getString("title", ""), s.getBoolean("owner-only", true));
    }

    private static Trash trash(ConfigurationSection s) {
        return s == null ? null : new Trash(s.getInt("rows", 3), s.getString("title", ""));
    }

    private static Light light(ConfigurationSection s) {
        return s == null ? null : new Light(s.getInt("level", 15), Offset.parse(s.getString("at", "0,0,0")));
    }

    private static States states(ConfigurationSection s) {

        if (s == null) {
            return null;
        }

        List<State> states = new ArrayList<>();
        for (Map<?, ?> raw : s.getMapList("list")) {
            ConfigurationSection st = section(raw);
            states.add(new State(
                    st.getString("id", "state" + states.size()),
                    st.getString("suffix", ""),
                    st.getInt("light", -1),
                    blankToNull(st.getString("sound"))));
        }

        if (states.size() < 2) {
            throw new IllegalArgumentException("'states' necesita al menos dos estados en 'list'");
        }

        return new States(states, trigger(s.getString("trigger", "click")));
    }

    private static Workstation workstation(ConfigurationSection s) {

        if (s.isConfigurationSection("workstation")) {
            ConfigurationSection w = s.getConfigurationSection("workstation");
            return new Workstation(w.getString("type", "CRAFTING").toUpperCase(Locale.ROOT),
                    w.getString("station", CarpenterRecipe.DEFAULT_STATION).toLowerCase(Locale.ROOT));
        }

        String type = s.getString("workstation");
        return type == null ? null : new Workstation(type.toUpperCase(Locale.ROOT), CarpenterRecipe.DEFAULT_STATION);
    }

    private static Shelf shelf(ConfigurationSection s) {

        List<Map<?, ?>> list = s.getMapList("shelf");
        if (list.isEmpty()) {
            return null;
        }

        List<ShelfSlot> slots = new ArrayList<>();
        for (Map<?, ?> raw : list) {
            ConfigurationSection slot = section(raw);
            slots.add(new ShelfSlot(Offset.parse(slot.getString("at", "0,0.5,0")),
                    (float) slot.getDouble("scale", 0.4), slot.getBoolean("flat", false)));
        }
        return new Shelf(slots);
    }

    private static Ambient ambient(ConfigurationSection s, ConfigurationSection states, String where,
            Consumer<String> warn) {

        if (s == null) {
            return null;
        }

        int state = -1;
        String stateId = s.getString("state");

        if (stateId != null && states != null) {
            List<Map<?, ?>> list = states.getMapList("list");
            for (int i = 0; i < list.size(); i++) {
                if (stateId.equals(String.valueOf(list.get(i).get("id")))) {
                    state = i;
                }
            }
            if (state < 0) {
                warn.accept("⚠ " + where + ": 'ambient.state' nombra un estado que no existe: " + stateId);
            }
        }

        return new Ambient(s.getString("particle", "SMOKE").toUpperCase(Locale.ROOT),
                Offset.parse(s.getString("at", "0,0.5,0")), s.getInt("count", 1), s.getDouble("spread", 0.05),
                s.getInt("every", 10), state);
    }

    private static List<Action> actions(ConfigurationSection s, String where, Consumer<String> warn) {

        List<Action> actions = new ArrayList<>();

        for (Map<?, ?> raw : s.getMapList("actions")) {

            ConfigurationSection a = section(raw);
            String command = blankToNull(a.getString("command"));
            String sound = blankToNull(a.getString("sound"));
            String message = blankToNull(a.getString("message"));

            if (command == null && sound == null && message == null) {
                warn.accept("⚠ " + where + ": una acción sin command, sound ni message, se ignora");
                continue;
            }

            if (command != null && command.startsWith("/")) {
                command = command.substring(1);
            }

            actions.add(new Action(trigger(a.getString("trigger", "click")), command, a.getBoolean("console", false),
                    sound, message));
        }

        return actions;
    }

    // ---------------------------------------------------------------- ayudas

    private static Trigger trigger(String text) {
        String t = text.toUpperCase(Locale.ROOT).replace('-', '_');
        return t.startsWith("SNEAK") ? Trigger.SNEAK_CLICK : Trigger.CLICK;
    }

    private static Material material(String name) {

        Material material = Material.matchMaterial(name);
        // Que además sea un ítem lo comprueba FurnitureManager.checkItems() con el servidor
        // arrancado: Material.isItem() necesita el registro de ítems, que en un test no existe.
        if (material == null || air(material) || material.isLegacy()) {
            throw new IllegalArgumentException("material '" + name + "' no es un ítem");
        }
        return material;
    }

    /** Sin Material.isAir(): también necesita el registro. */
    private static boolean air(Material material) {
        return material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR;
    }

    private static ConfigurationSection section(Map<?, ?> raw) {

        MemoryConfiguration section = new MemoryConfiguration();
        raw.forEach((k, v) -> section.set(String.valueOf(k), v));
        return section;
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text;
    }
}
