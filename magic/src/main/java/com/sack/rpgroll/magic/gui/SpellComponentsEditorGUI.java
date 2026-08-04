package com.sack.rpgroll.magic.gui;

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
    private final Runnable onBack;
    private Spell current;

    public SpellComponentsEditorGUI(Player player, Spell spell, SpellManager spellManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Componentes: " + spell.id(), NamedTextColor.GOLD), SIZE);
        this.current = spell;
        this.spellManager = spellManager;
        this.chatPromptManager = chatPromptManager;
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
                .setName(Component.text("Agregar componente", NamedTextColor.GREEN))
                .setLore(Component.text("TIPO clave=valor,clave2=valor2", NamedTextColor.GRAY),
                        Component.text("ej. PROJECTILE speed=1.2,max-distance=25", NamedTextColor.DARK_GRAY),
                        Component.text("ej. DAMAGE_DIRECT amount=8", NamedTextColor.DARK_GRAY),
                        Component.text("Orden importa — se ejecutan de arriba a abajo", NamedTextColor.DARK_GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    private org.bukkit.inventory.ItemStack componentItem(SpellComponent component, int index) {

        List<Component> lore = new ArrayList<>();

        for (var entry : component.params().entrySet()) {
            lore.add(Component.text(entry.getKey() + "=" + entry.getValue(), NamedTextColor.DARK_GRAY));
        }

        lore.add(Component.text("Shift-click para quitar", NamedTextColor.RED));

        return new ItemBuilder(iconFor(component.type()))
                .setName(Component.text("#" + (index + 1) + " " + component.type(), NamedTextColor.AQUA))
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
        chatPromptManager.prompt(player, "Escribí: TIPO clave=valor,clave2=valor2:", value -> {

            String[] parts = value.trim().split("\\s+", 2);
            SpellComponentType type;

            try {
                type = SpellComponentType.valueOf(parts[0].trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                player.sendMessage(Component.text("Tipo inválido: " + parts[0], NamedTextColor.RED));
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
