package com.sack.rpgroll.magic.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.magic.core.Spell;
import com.sack.rpgroll.magic.core.SpellComponent;
import com.sack.rpgroll.magic.core.SpellComponentType;
import com.sack.rpgroll.magic.core.SpellManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * El pipeline de un hechizo — sin excepciones, TODOS los hechizos se arman
 * encadenando estos mismos componentes en orden, agregados con la misma
 * sintaxis compacta que el resto del proyecto: {@code TIPO clave=valor}.
 * PROJECTILE/DELAY pausan el pipeline (colisión sobre varios ticks / espera
 * agendada) — el resto corre sincrónico dentro del mismo tick.
 */
public class SpellComponentsEditorGUI extends InventoryGUI {

    private static final int SIZE = 54;
    private static final int COMPONENTS_START = 0;
    private static final int COMPONENTS_MAX = 45;
    private static final int ADD_SLOT = 49;
    private static final int BACK_SLOT = 53;

    private final SpellManager spellManager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private final Runnable onBack;
    private Spell current;

    public SpellComponentsEditorGUI(Player player, Spell spell, SpellManager spellManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, chatPromptManager.lang().component("gui.spell_components.title", "id", spell.id()), SIZE);
        this.current = spell;
        this.spellManager = spellManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = chatPromptManager.lang();
        this.onBack = onBack;
    }

    private void replace(List<SpellComponent> components) {
        current = new Spell(current.id(), current.displayName(), current.icon(), current.color(),
                current.schoolId(), current.rarity(), current.level(), current.cost(), current.castTimeTicks(),
                current.cooldownTicks(), current.trigger(), current.treeParentId(), current.treeTier(),
                current.tags(), current.description(), components);
        spellManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 45; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        List<SpellComponent> components = current.components();

        for (int i = 0; i < components.size() && i < COMPONENTS_MAX; i++) {
            setItem(COMPONENTS_START + i, componentItem(components.get(i), i));
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.spell_components.add"))
                .setLore(lang.component("gui.spell_components.add_lore_1"),
                        lang.component("gui.spell_components.add_lore_2"),
                        lang.component("gui.spell_components.add_lore_3"),
                        lang.component("gui.spell_components.add_lore_4"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    private org.bukkit.inventory.ItemStack componentItem(SpellComponent component, int index) {

        List<Component> lore = new ArrayList<>();

        for (var entry : component.params().entrySet()) {
            lore.add(Component.text(entry.getKey() + "=" + entry.getValue(), NamedTextColor.DARK_GRAY));
        }

        lore.add(lang.component("gui.common.shift_remove"));

        return new ItemBuilder(iconFor(component.type()))
                .setName(lang.component("gui.spell_components.component_label", "index", index + 1,
                        "type", component.type()))
                .setLore(lore)
                .build();
    }

    private Material iconFor(SpellComponentType type) {
        return switch (type) {
            case PROJECTILE, DASH, LEAP, TELEPORT, ORBIT -> Material.FEATHER;
            case DAMAGE_DIRECT, DAMAGE_AREA, DAMAGE_LINE, DAMAGE_CHAIN, DAMAGE_CONE -> Material.IRON_SWORD;
            case PARTICLE, SOUND, VISUAL -> Material.BLAZE_POWDER;
            case BREAK_BLOCK, PLACE_BLOCK, IGNITE, FREEZE_WATER -> Material.STONE;
            case SUMMON -> Material.ZOMBIE_HEAD;
            case HEAL -> Material.GOLDEN_APPLE;
            case APPLY_EFFECT, REMOVE_EFFECT -> Material.POTION;
            case PUSH, PULL -> Material.SLIME_BALL;
            case DELAY -> Material.CLOCK;
            case MESSAGE -> Material.PAPER;
            case COMMAND -> Material.COMMAND_BLOCK;
        };
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < current.components().size() && slot < COMPONENTS_MAX) {
            if (event.isShiftClick()) {
                removeComponent(slot);
            }
            return;
        }

        if (slot == ADD_SLOT) {
            promptAddComponent();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void removeComponent(int index) {
        List<SpellComponent> components = new ArrayList<>(current.components());
        components.remove(index);
        replace(components);
    }

    private void promptAddComponent() {
        chatPromptManager.prompt(player, lang.raw("gui.spell_components.prompt_add"), value -> {

            String[] parts = value.trim().split("\\s+", 2);
            SpellComponentType type;

            try {
                type = SpellComponentType.valueOf(parts[0].trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                lang.send(player, "gui.spell_components.invalid_type", "type", parts[0]);
                return;
            }

            Map<String, String> params = new LinkedHashMap<>();

            if (parts.length == 2) {
                for (String pair : parts[1].split(",")) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length == 2) {
                        params.put(kv[0].trim(), kv[1].trim());
                    }
                }
            }

            List<SpellComponent> components = new ArrayList<>(current.components());
            components.add(new SpellComponent(type, params));
            replace(components);
        });
    }

}
