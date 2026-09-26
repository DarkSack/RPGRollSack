package com.sack.rpgroll.extras.backpack;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Lee backpacks.yml. Lo que está mal se avisa en consola y se salta (o toma
 * su valor por defecto); nunca tumba el resto de la configuración.
 */
public class BackpackConfigParser {

    private final Consumer<String> warn;

    public BackpackConfigParser(Consumer<String> warn) {
        this.warn = warn;
    }

    public BackpackSettings parse(ConfigurationSection root) {

        if (root == null) {
            return BackpackSettings.DISABLED;
        }

        ConfigurationSection settings = section(root, "settings");
        ConfigurationSection sounds = section(root, "sounds");
        ConfigurationSection guiSection = section(root, "gui");

        BackpackSettings.Gui gui = parseGui(guiSection);
        List<BackpackTier> tiers = parseTiers(root.getConfigurationSection("tiers"), gui.tabSlots().size());

        return new BackpackSettings(
                settings.getBoolean("enabled", true) && !tiers.isEmpty(),
                settings.getBoolean("open-in-air", true),
                settings.getBoolean("open-from-inventory", true),
                settings.getBoolean("allow-place", true),
                !"anyone".equalsIgnoreCase(settings.getString("placed-access", "placer")),
                settings.getBoolean("discover-recipes", true),
                materials(settings.getStringList("forbidden-items"), "settings.forbidden-items"),
                guiSection.getString("title", "{name}"),
                settings.getString("owner-lore.unbound", ""),
                settings.getString("owner-lore.bound", "&7Ligada a &e{owner}"),
                new BackpackSettings.Sounds(
                        sounds.getString("open", ""),
                        sounds.getString("close", ""),
                        sounds.getString("page", ""),
                        sounds.getString("bind", "")),
                gui,
                tiers);
    }

    // ---------------------------------------------------------------- gui

    private BackpackSettings.Gui parseGui(ConfigurationSection gui) {

        List<Integer> tabSlots = new ArrayList<>();
        for (int slot : gui.getIntegerList("tabs.slots")) {
            if (slot < 0 || slot > 8) {
                warn.accept("gui.tabs.slots: " + slot + " no es una columna de la fila de botones (0-8), se ignora.");
            } else if (!tabSlots.contains(slot)) {
                tabSlots.add(slot);
            }
        }
        if (tabSlots.isEmpty()) {
            tabSlots.add(4);
        }

        int infoSlot = column(gui, "info.slot", 0);
        int bindSlot = column(gui, "bind.slot", 8);

        if (tabSlots.contains(infoSlot)) {
            warn.accept("gui.info.slot choca con una pestaña; se quita el botón de información.");
            infoSlot = -1;
        }
        if (bindSlot >= 0 && (tabSlots.contains(bindSlot) || bindSlot == infoSlot)) {
            warn.accept("gui.bind.slot choca con otro botón; se quita el botón de ligar.");
            bindSlot = -1;
        }

        return new BackpackSettings.Gui(
                button(gui, "filler", Material.BLACK_STAINED_GLASS_PANE, " "),
                button(gui, "locked-slot", Material.GRAY_STAINED_GLASS_PANE, "&8Espacio bloqueado"),
                infoSlot,
                button(gui, "info", Material.BOOK, "{name}"),
                List.copyOf(tabSlots),
                button(gui, "tabs.normal", Material.WHITE_STAINED_GLASS_PANE, "&ePestaña {page}"),
                button(gui, "tabs.selected", Material.LIME_STAINED_GLASS_PANE, "&aPestaña {page}"),
                bindSlot,
                button(gui, "bind.unbound", Material.TRIPWIRE_HOOK, "&eLigar a mí"),
                button(gui, "bind.bound", Material.NAME_TAG, "&aLigada a {owner}"));
    }

    private int column(ConfigurationSection gui, String path, int fallback) {

        int slot = gui.getInt(path, fallback);

        if (slot < -1 || slot > 8) {
            warn.accept("gui." + path + ": " + slot + " no es una columna de la fila de botones (0-8, o -1 para "
                    + "quitarlo); se usa " + fallback + ".");
            return fallback;
        }

        return slot;
    }

    private BackpackButton button(ConfigurationSection gui, String path, Material fallbackMaterial, String fallbackName) {

        ConfigurationSection section = gui.getConfigurationSection(path);

        if (section == null) {
            return new BackpackButton(fallbackMaterial, fallbackName, List.of());
        }

        Material material = material(section.getString("material"), "gui." + path + ".material");

        return new BackpackButton(material != null ? material : fallbackMaterial,
                section.getString("name", fallbackName), section.getStringList("lore"));
    }

    // ---------------------------------------------------------------- niveles

    private List<BackpackTier> parseTiers(ConfigurationSection tiersSection, int maxPages) {

        List<BackpackTier> tiers = new ArrayList<>();

        if (tiersSection == null) {
            warn.accept("backpacks.yml no tiene sección tiers: las mochilas quedan desactivadas.");
            return tiers;
        }

        int maxSlots = maxPages * BackpackPages.PAGE_SIZE;

        for (String id : tiersSection.getKeys(false)) {

            ConfigurationSection section = tiersSection.getConfigurationSection(id);

            if (section == null) {
                warn.accept("tiers." + id + " no es una sección, se ignora.");
                continue;
            }

            int slots = section.getInt("slots", 27);

            if (slots < 1) {
                warn.accept("tiers." + id + ".slots debe ser al menos 1; se usa 9.");
                slots = 9;
            } else if (slots > maxSlots) {
                warn.accept("tiers." + id + ".slots (" + slots + ") necesita más pestañas de las que hay en "
                        + "gui.tabs.slots (" + maxPages + "); se recorta a " + maxSlots + ".");
                slots = maxSlots;
            }

            int index = tiers.size();
            BackpackRecipe recipe = parseRecipe(section.getConfigurationSection("recipe"), "tiers." + id, index);

            tiers.add(new BackpackTier(id.toLowerCase(Locale.ROOT), index, section.getString("name", id),
                    section.getStringList("lore"), section.getString("texture", ""), slots, recipe));
        }

        return tiers;
    }

    BackpackRecipe parseRecipe(ConfigurationSection recipe, String where, int tierIndex) {

        if (recipe == null || !recipe.getBoolean("enabled", true)) {
            return null;
        }

        List<String> shape = recipe.getStringList("shape");

        if (shape.isEmpty() || shape.size() > 3 || shape.stream().anyMatch(row -> row.isEmpty() || row.length() > 3)) {
            warn.accept(where + ".recipe.shape debe tener de 1 a 3 filas de 1 a 3 letras; el nivel queda sin receta.");
            return null;
        }

        ConfigurationSection ingredientSection = recipe.getConfigurationSection("ingredients");
        Map<Character, BackpackIngredient> ingredients = new LinkedHashMap<>();

        if (ingredientSection != null) {
            for (String key : ingredientSection.getKeys(false)) {

                if (key.length() != 1 || key.charAt(0) == ' ') {
                    warn.accept(where + ".recipe.ingredients: la clave «" + key + "» debe ser una sola letra; "
                            + "el nivel queda sin receta.");
                    return null;
                }

                try {
                    ingredients.put(key.charAt(0), BackpackIngredient.parse(ingredientSection.getString(key)));
                } catch (IllegalArgumentException e) {
                    warn.accept(where + ".recipe.ingredients." + key + ": " + e.getMessage()
                            + "; el nivel queda sin receta.");
                    return null;
                }
            }
        }

        int backpacks = 0;
        for (String row : shape) {
            for (char symbol : row.toCharArray()) {
                if (symbol == ' ') {
                    continue;
                }
                BackpackIngredient ingredient = ingredients.get(symbol);
                if (ingredient == null) {
                    warn.accept(where + ".recipe.shape usa «" + symbol + "», que no está en ingredients; "
                            + "el nivel queda sin receta.");
                    return null;
                }
                if (ingredient.kind() == BackpackIngredient.Kind.BACKPACK) {
                    backpacks++;
                }
            }
        }

        if (backpacks > 1) {
            warn.accept(where + ".recipe: solo puede llevar una mochila; el nivel queda sin receta.");
            return null;
        }

        if (backpacks == 1 && tierIndex == 0) {
            warn.accept(where + ".recipe pide la mochila anterior, pero es el primer nivel; el nivel queda sin receta.");
            return null;
        }

        return new BackpackRecipe(List.copyOf(shape), Map.copyOf(ingredients));
    }

    // ---------------------------------------------------------------- utilidades

    private Set<Material> materials(List<String> names, String where) {

        Set<Material> result = EnumSet.noneOf(Material.class);

        for (String name : names) {
            Material material = material(name, where);
            if (material != null) {
                result.add(material);
            }
        }

        return result;
    }

    private Material material(String name, String where) {

        if (name == null || name.isBlank()) {
            return null;
        }

        try {
            return Material.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            warn.accept(where + ": material desconocido «" + name + "».");
            return null;
        }
    }

    private static ConfigurationSection section(ConfigurationSection root, String path) {
        ConfigurationSection section = root.getConfigurationSection(path);
        return section != null ? section : root.createSection(path);
    }

}
