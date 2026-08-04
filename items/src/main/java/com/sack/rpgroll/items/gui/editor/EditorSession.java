package com.sack.rpgroll.items.gui.editor;

import com.sack.rpgroll.items.core.ArmorTrimDef;
import com.sack.rpgroll.items.core.ItemAbility;
import com.sack.rpgroll.items.core.ItemAction;
import com.sack.rpgroll.items.core.ItemDefinition;
import com.sack.rpgroll.items.core.ItemDefinitionWriter;
import com.sack.rpgroll.items.core.ItemDurabilityDef;
import com.sack.rpgroll.items.core.ItemEffectDef;
import com.sack.rpgroll.items.core.ItemFactory;
import com.sack.rpgroll.items.core.ItemManager;
import com.sack.rpgroll.items.core.ItemRecipeDef;
import com.sack.rpgroll.items.core.ItemRequirements;
import com.sack.rpgroll.items.core.ItemSkin;
import com.sack.rpgroll.items.core.ItemTrigger;
import com.sack.rpgroll.items.core.SocketDefinition;
import com.sack.rpgroll.items.core.UpgradeLevel;
import com.sack.rpgroll.items.gui.ChatPromptManager;
import com.sack.rpgroll.items.rarity.RarityManager;
import com.sack.rpgroll.items.registry.StatRegistry;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Copia de trabajo mutable de un {@link ItemDefinition} en edición.
 * Compartida por referencia entre el hub y todos los sub-editores — cada
 * uno muta directamente los campos que le corresponden, y cualquiera
 * puede pedir una vista previa ({@link #preview()}) o persistir
 * ({@link #save()}) en cualquier momento sin perder lo que tocaron los
 * demás.
 */
public class EditorSession {

    public final ItemDefinition original;
    public final ItemFactory itemFactory;
    public final ItemManager itemManager;
    public final RarityManager rarityManager;
    public final StatRegistry statRegistry;
    public final ChatPromptManager chatPromptManager;
    public final Plugin plugin;

    public Material material;
    public String displayName;
    public List<String> lore;
    public Integer customModelData;
    public String rarityId;
    public Boolean glowOverride;
    public List<String> flags;
    public boolean unbreakable;
    public String dyeColor;
    public String skullTexture;
    public ArmorTrimDef trim;
    public Map<String, Double> stats;
    public Map<String, Double> attributeModifiers;
    public ItemRequirements requirements;
    public ItemDurabilityDef durability;
    public Map<String, Integer> vanillaEnchantments;
    public Map<String, Integer> customEnchantments;
    public List<ItemEffectDef> effects;
    public Map<ItemTrigger, List<ItemAction>> triggers;
    public List<ItemAbility> abilities;
    public List<SocketDefinition> sockets;
    public List<ItemSkin> skins;
    public List<UpgradeLevel> upgrades;
    public List<ItemRecipeDef> recipes;
    public double sellPrice;
    public double buyPrice;
    public Map<String, String> customData;

    public EditorSession(ItemDefinition original, ItemFactory itemFactory, ItemManager itemManager,
            RarityManager rarityManager, StatRegistry statRegistry, ChatPromptManager chatPromptManager,
            Plugin plugin) {

        this.original = original;
        this.itemFactory = itemFactory;
        this.itemManager = itemManager;
        this.rarityManager = rarityManager;
        this.statRegistry = statRegistry;
        this.chatPromptManager = chatPromptManager;
        this.plugin = plugin;

        this.material = original.material();
        this.displayName = original.displayName();
        this.lore = new ArrayList<>(original.lore());
        this.customModelData = original.customModelData();
        this.rarityId = original.rarityId();
        this.glowOverride = original.glowOverride();
        this.flags = new ArrayList<>(original.flags());
        this.unbreakable = original.unbreakable();
        this.dyeColor = original.dyeColor();
        this.skullTexture = original.skullTexture();
        this.trim = original.trim();
        this.stats = new LinkedHashMap<>(original.stats());
        this.attributeModifiers = new LinkedHashMap<>(original.attributeModifiers());
        this.requirements = original.requirements();
        this.durability = original.durability();
        this.vanillaEnchantments = new LinkedHashMap<>(original.vanillaEnchantments());
        this.customEnchantments = new LinkedHashMap<>(original.customEnchantments());
        this.effects = new ArrayList<>(original.effects());
        this.triggers = new EnumMap<>(ItemTrigger.class);
        this.triggers.putAll(original.triggers());
        this.abilities = new ArrayList<>(original.abilities());
        this.sockets = new ArrayList<>(original.sockets());
        this.skins = new ArrayList<>(original.skins());
        this.upgrades = new ArrayList<>(original.upgrades());
        this.recipes = new ArrayList<>(original.recipes());
        this.sellPrice = original.sellPrice();
        this.buyPrice = original.buyPrice();
        this.customData = new LinkedHashMap<>(original.customData());
    }

    public ItemDefinition buildDefinition() {
        return new ItemDefinition(
                original.id(), original.category(), material, displayName, lore, customModelData, rarityId,
                glowOverride, flags, unbreakable, dyeColor, skullTexture, trim, stats, attributeModifiers,
                requirements, durability, vanillaEnchantments, customEnchantments, effects, triggers, abilities,
                sockets, skins, upgrades, recipes, sellPrice, buyPrice, customData);
    }

    public ItemStack preview() {
        return itemFactory.create(buildDefinition());
    }

    public void save() throws IOException {

        ItemDefinition updated = buildDefinition();
        itemManager.register(updated);

        File file = new File(plugin.getDataFolder(), "items/" + updated.category() + "/" + updated.id() + ".yml");
        new ItemDefinitionWriter().save(updated, file);
    }

}
