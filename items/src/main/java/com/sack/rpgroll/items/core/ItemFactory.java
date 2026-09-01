package com.sack.rpgroll.items.core;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.items.instance.ItemInstanceService;
import com.sack.rpgroll.items.integration.EnchantmentsIntegration;
import com.sack.rpgroll.items.rarity.Rarity;
import com.sack.rpgroll.items.rarity.RarityManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Construye el ItemStack real de una {@link ItemDefinition}: apariencia
 * (material/nombre/lore/rareza/glow/custom-model-data), encantamientos
 * (vanilla + RPGRoll-Enchantments), flags, y los tags de instancia
 * iniciales (id, durabilidad al máximo, nivel de mejora 0, skin base,
 * sockets vacíos). También reconstruye el lore tras cualquier cambio de
 * estado (durabilidad, mejora, skin, sockets) sin tocar los tags.
 */
public class ItemFactory {


    private final ItemInstanceService instanceService;
    private final RarityManager rarityManager;
    private final LangManager langManager;

    public ItemFactory(Plugin plugin, ItemInstanceService instanceService, RarityManager rarityManager,
            LangManager langManager) {
        this.instanceService = instanceService;
        this.rarityManager = rarityManager;
        this.langManager = langManager;
    }

    public ItemStack create(ItemDefinition definition) {

        ItemStack item = new ItemStack(definition.material());
        ItemMeta meta = item.getItemMeta();

        instanceService.setId(meta, definition.id());
        instanceService.setDurability(meta, definition.durability().enabled() ? definition.durability().maxDurability() : -1);
        instanceService.setUpgradeLevel(meta, 0);
        instanceService.setSkinIndex(meta, 0);

        Map<String, String> emptySockets = new LinkedHashMap<>();
        for (var socket : definition.sockets()) {
            emptySockets.put(socket.id(), "");
        }
        instanceService.setSockets(meta, emptySockets);
        instanceService.setCustomData(meta, definition.customData());

        applyEnchantments(meta, definition);
        applyFlags(meta, definition);
        applyAttributeModifiers(meta, definition);
        applyMaterialSpecificMeta(meta, definition);

        item.setItemMeta(meta);
        applyAppearance(item, definition, 0, 0, emptySockets);

        EnchantmentsIntegration.applyCustomEnchantments(item, definition.customEnchantments());

        return item;
    }

    /** Reconstruye nombre/lore a partir del estado actual del PDC — llamar tras cualquier mutación. */
    public void rebuildDisplay(ItemStack item, ItemDefinition definition) {

        int upgradeLevel = instanceService.getUpgradeLevel(item);
        int skinIndex = instanceService.getSkinIndex(item);
        Map<String, String> sockets = instanceService.getSockets(item);

        applyAppearance(item, definition, upgradeLevel, skinIndex, sockets);
    }

    private void applyAppearance(ItemStack item, ItemDefinition definition, int upgradeLevel, int skinIndex,
            Map<String, String> sockets) {

        ItemSkin currentSkin = skinIndex > 0 && skinIndex <= definition.skins().size()
                ? definition.skins().get(skinIndex - 1)
                : null;

        if (currentSkin != null && currentSkin.material() != null) {
            Material skinMaterial = parseMaterial(currentSkin.material());
            if (skinMaterial != null && item.getType() != skinMaterial) {
                item.setType(skinMaterial);
            }
        } else if (item.getType() != definition.material()) {
            item.setType(definition.material());
        }

        ItemMeta meta = item.getItemMeta();

        UpgradeLevel currentUpgrade = upgradeLevel > 0 ? findUpgrade(definition, upgradeLevel) : null;

        String rarityId = currentUpgrade != null && currentUpgrade.rarityOverride() != null
                ? currentUpgrade.rarityOverride()
                : definition.rarityId();
        Rarity rarity = rarityManager.getOrFallback(rarityId);

        String displayName = currentUpgrade != null && currentUpgrade.displayNameOverride() != null
                ? currentUpgrade.displayNameOverride()
                : currentSkin != null ? currentSkin.displayName() : definition.displayName();

        String suffix = upgradeLevel > 0 ? " &7+" + upgradeLevel : "";
        int currentDurabilityForName = instanceService.getDurability(item, definition.durability().maxDurability());

        Component nameComponent = Component.text().color(rarity.color())
                .append(ComponentUtils.parse(applyPlaceholders(displayName, upgradeLevel, currentDurabilityForName,
                        definition.durability().maxDurability()) + suffix))
                .build()
                .decoration(TextDecoration.ITALIC, false);

        meta.displayName(nameComponent);

        boolean glow = definition.glowOverride() != null ? definition.glowOverride() : rarity.glow();
        Enchantment glowEnchant = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("luck_of_the_sea"));

        if (glow && glowEnchant != null && meta.getEnchants().isEmpty()) {
            meta.addEnchant(glowEnchant, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        if (currentSkin != null && currentSkin.customModelData() != null) {
            meta.setCustomModelData(currentSkin.customModelData());
        } else if (definition.customModelData() != null) {
            meta.setCustomModelData(definition.customModelData());
        }

        int currentDurability = instanceService.getDurability(item, definition.durability().maxDurability());

        meta.lore(buildLore(definition, rarity, upgradeLevel, sockets, currentDurability));

        item.setItemMeta(meta);
    }

    private List<Component> buildLore(ItemDefinition definition, Rarity rarity, int upgradeLevel,
            Map<String, String> sockets, int currentDurability) {

        List<Component> lore = new ArrayList<>();

        for (String line : definition.lore()) {
            String rendered = applyPlaceholders(line, upgradeLevel, currentDurability,
                    definition.durability().maxDurability());
            lore.add(ComponentUtils.parse(rendered).decoration(TextDecoration.ITALIC, false));
        }

        Map<String, Double> stats = combinedStats(definition, upgradeLevel);

        if (!stats.isEmpty()) {
            lore.add(Component.empty());
            for (var entry : stats.entrySet()) {
                lore.add(Component.text("+ " + formatStatName(entry.getKey()) + ": ", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(formatNumber(entry.getValue()), NamedTextColor.WHITE)));
            }
        }

        if (definition.durability().enabled()) {
            lore.add(Component.empty());
            lore.add(langManager.component("item.durability_lore", "current", currentDurability, "max",
                            definition.durability().maxDurability())
                    .decoration(TextDecoration.ITALIC, false));
        }

        if (!definition.sockets().isEmpty()) {
            lore.add(Component.empty());
            for (var socket : definition.sockets()) {

                String gemId = sockets.getOrDefault(socket.id(), "");

                Component line = gemId.isBlank()
                        ? langManager.component("item.socket_empty")
                        : langManager.component("item.socket_filled", "gem", gemId);

                lore.add(line.decoration(TextDecoration.ITALIC, false));
            }
        }

        lore.add(Component.empty());
        lore.add(ComponentUtils.parseWithDefault(rarity.displayName(), rarity.color()).decoration(TextDecoration.ITALIC, false));

        return lore;
    }

    /** Stats base + bonos de todos los niveles de mejora alcanzados (sin contar gemas). */
    public Map<String, Double> combinedStats(ItemDefinition definition, int upgradeLevel) {

        Map<String, Double> combined = new LinkedHashMap<>(definition.stats());

        for (UpgradeLevel level : definition.upgrades()) {
            if (level.level() > upgradeLevel) {
                break;
            }
            for (var entry : level.statBonus().entrySet()) {
                combined.merge(entry.getKey(), entry.getValue(), Double::sum);
            }
        }

        return combined;
    }

    private UpgradeLevel findUpgrade(ItemDefinition definition, int level) {
        return definition.upgrades().stream().filter(u -> u.level() == level).findFirst().orElse(null);
    }

    private Material parseMaterial(String raw) {
        try {
            return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String formatStatName(String key) {

        String[] parts = key.split("_");
        StringBuilder result = new StringBuilder();

        for (String part : parts) {
            if (!result.isEmpty()) {
                result.append(" ");
            }
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }

        return result.toString();
    }

    /** Placeholders dinámicos disponibles en display-name/lore — se resuelven en cada rebuild. */
    private String applyPlaceholders(String text, int upgradeLevel, int durability, int maxDurability) {

        if (!text.contains("{")) {
            return text;
        }

        return text
                .replace("{upgrade_level}", String.valueOf(upgradeLevel))
                .replace("{durability}", String.valueOf(durability))
                .replace("{durability_max}", String.valueOf(maxDurability));
    }

    private String formatNumber(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    private void applyEnchantments(ItemMeta meta, ItemDefinition definition) {

        for (var entry : definition.vanillaEnchantments().entrySet()) {

            Enchantment enchantment = Registry.ENCHANTMENT.get(
                    NamespacedKey.minecraft(entry.getKey().toLowerCase(Locale.ROOT)));

            if (enchantment != null) {
                meta.addEnchant(enchantment, entry.getValue(), true);
            }
        }
    }

    private void applyFlags(ItemMeta meta, ItemDefinition definition) {

        for (String rawFlag : definition.flags()) {

            try {
                meta.addItemFlags(ItemFlag.valueOf(rawFlag.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
            }
        }

        meta.setUnbreakable(definition.unbreakable());

        if (definition.unbreakable()) {
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        }
    }

    /** Atributos vanilla reales (no los "stats" virtuales de RPGRoll) — se fijan una sola vez en create(). */
    private void applyAttributeModifiers(ItemMeta meta, ItemDefinition definition) {

        for (var entry : definition.attributeModifiers().entrySet()) {

            Attribute attribute = parseAttribute(entry.getKey());
            if (attribute == null) {
                continue;
            }

            NamespacedKey key = new NamespacedKey("rpgroll-items", "attr-" + entry.getKey().toLowerCase(Locale.ROOT));
            AttributeModifier modifier = new AttributeModifier(key, entry.getValue(), AttributeModifier.Operation.ADD_NUMBER);

            meta.addAttributeModifier(attribute, modifier);
        }
    }

    private Attribute parseAttribute(String raw) {
        try {
            return Attribute.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Color de cuero/poción, textura de cabeza y trim de armadura — todos estáticos, se fijan una sola vez. */
    private void applyMaterialSpecificMeta(ItemMeta meta, ItemDefinition definition) {

        if (definition.dyeColor() != null) {

            Color color = parseColor(definition.dyeColor());

            if (color != null) {
                if (meta instanceof LeatherArmorMeta leatherMeta) {
                    leatherMeta.setColor(color);
                } else if (meta instanceof PotionMeta potionMeta) {
                    potionMeta.setColor(color);
                }
            }
        }

        if (definition.skullTexture() != null && meta instanceof SkullMeta skullMeta) {
            applySkullTexture(skullMeta, definition.skullTexture());
        }

        if (definition.trim() != null && meta instanceof ArmorMeta armorMeta) {
            applyTrim(armorMeta, definition.trim());
        }
    }

    private Color parseColor(String raw) {

        try {
            String hex = raw.startsWith("#") ? raw.substring(1) : raw;
            return Color.fromRGB(Integer.parseInt(hex, 16));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void applySkullTexture(SkullMeta meta, String base64Texture) {

        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
        profile.setProperty(new ProfileProperty("textures", base64Texture));
        meta.setPlayerProfile(profile);
    }

    private void applyTrim(ArmorMeta meta, ArmorTrimDef trimDef) {

        TrimMaterial material = Registry.TRIM_MATERIAL.get(
                NamespacedKey.minecraft(trimDef.material().toLowerCase(Locale.ROOT)));
        TrimPattern pattern = Registry.TRIM_PATTERN.get(
                NamespacedKey.minecraft(trimDef.pattern().toLowerCase(Locale.ROOT)));

        if (material != null && pattern != null) {
            meta.setTrim(new ArmorTrim(material, pattern));
        }
    }

}
