package com.sack.rpgroll.magic.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.magic.core.MagicSchool;
import com.sack.rpgroll.magic.core.SchoolManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SchoolEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int NAME_SLOT = 10;
    private static final int COLOR_SLOT = 11;
    private static final int ICON_SLOT = 12;
    private static final int DESCRIPTION_SLOT = 13;
    private static final int CAST_SOUND_SLOT = 19;
    private static final int CAST_EFFECT_SLOT = 20;
    private static final int RACE_AFFINITIES_SLOT = 22;
    private static final int CLASS_AFFINITIES_SLOT = 23;
    private static final int BACK_SLOT = 40;

    private final SchoolManager schoolManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private MagicSchool current;

    public SchoolEditorGUI(Player player, MagicSchool school, SchoolManager schoolManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Escuela: " + school.id(), NamedTextColor.GOLD), SIZE);
        this.current = school;
        this.schoolManager = schoolManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(MagicSchool updated) {
        current = updated;
        schoolManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(Component.text("Nombre: " + current.displayName(), NamedTextColor.YELLOW))
                .build());

        setItem(COLOR_SLOT, new ItemBuilder(Material.PAPER)
                .setName(Component.text("Color: " + current.color(), SchoolBrowserGUI.parseColor(current.color())))
                .setLore(Component.text("Click para escribir uno nuevo (ej. RED, AQUA...)", NamedTextColor.GRAY))
                .build());

        setItem(ICON_SLOT, new ItemBuilder(SchoolBrowserGUI.parseMaterial(current.icon()))
                .setName(Component.text("Ícono: " + current.icon(), NamedTextColor.YELLOW))
                .build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(Component.text("Descripción", NamedTextColor.YELLOW))
                .setLore(ItemBuilder.toLoreLines(
                        current.description().isBlank() ? "(sin descripción)" : current.description()))
                .build());

        setItem(CAST_SOUND_SLOT, new ItemBuilder(Material.NOTE_BLOCK)
                .setName(Component.text(
                        "Sonido al lanzar: " + (current.castSoundOnCast() == null ? "(ninguno)" : current.castSoundOnCast()),
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Escribí 'ninguno' para quitarlo", NamedTextColor.GRAY))
                .build());

        setItem(CAST_EFFECT_SLOT, new ItemBuilder(Material.BLAZE_POWDER)
                .setName(Component.text(
                        "Efecto SackEffects: " + (current.castEffectId() == null ? "(ninguno)" : current.castEffectId()),
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Escribí 'ninguno' para quitarlo", NamedTextColor.GRAY))
                .build());

        setItem(RACE_AFFINITIES_SLOT, new ItemBuilder(Material.ZOMBIE_HEAD)
                .setName(Component.text("Afinidades de raza: " + current.raceAffinities().size(), NamedTextColor.YELLOW))
                .setLore(affinityLore(current.raceAffinities()))
                .build());

        setItem(CLASS_AFFINITIES_SLOT, new ItemBuilder(Material.IRON_SWORD)
                .setName(Component.text("Afinidades de clase: " + current.classAffinities().size(), NamedTextColor.YELLOW))
                .setLore(affinityLore(current.classAffinities()))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    private List<Component> affinityLore(Map<String, Double> affinities) {

        List<Component> lore = new java.util.ArrayList<>();

        for (var entry : affinities.entrySet()) {
            lore.add(Component.text(entry.getKey() + ": " + (entry.getValue() >= 0 ? "+" : "")
                    + Math.round(entry.getValue() * 100) + "%", NamedTextColor.GRAY));
        }

        lore.add(Component.text("Click para reescribir (id=fraccion,id2=fraccion2)", NamedTextColor.DARK_GRAY));

        return lore;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo nombre:", value -> replace(new MagicSchool(
                    current.id(), value, current.color(), current.icon(), current.description(),
                    current.castSoundOnCast(), current.castEffectId(), current.raceAffinities(),
                    current.classAffinities())));
            return;
        }

        if (slot == COLOR_SLOT) {
            chatPromptManager.prompt(player, "Escribí el color (ej. RED, AQUA, LIGHT_PURPLE):", value -> replace(
                    new MagicSchool(current.id(), current.displayName(), value, current.icon(),
                            current.description(), current.castSoundOnCast(), current.castEffectId(),
                            current.raceAffinities(), current.classAffinities())));
            return;
        }

        if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, "Escribí el Material del ícono:", value -> replace(new MagicSchool(
                    current.id(), current.displayName(), current.color(), value, current.description(),
                    current.castSoundOnCast(), current.castEffectId(), current.raceAffinities(),
                    current.classAffinities())));
            return;
        }

        if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, "Escribí la nueva descripción:", value -> replace(new MagicSchool(
                    current.id(), current.displayName(), current.color(), current.icon(), value,
                    current.castSoundOnCast(), current.castEffectId(), current.raceAffinities(),
                    current.classAffinities())));
            return;
        }

        if (slot == CAST_SOUND_SLOT) {
            chatPromptManager.prompt(player, "Escribí el Sound (o 'ninguno' para quitarlo):", value -> replace(
                    new MagicSchool(current.id(), current.displayName(), current.color(), current.icon(),
                            current.description(), value.equalsIgnoreCase("ninguno") ? null : value,
                            current.castEffectId(), current.raceAffinities(), current.classAffinities())));
            return;
        }

        if (slot == CAST_EFFECT_SLOT) {
            chatPromptManager.prompt(player, "Escribí el id del efecto de SackEffects (o 'ninguno'):", value -> replace(
                    new MagicSchool(current.id(), current.displayName(), current.color(), current.icon(),
                            current.description(), current.castSoundOnCast(),
                            value.equalsIgnoreCase("ninguno") ? null : value, current.raceAffinities(),
                            current.classAffinities())));
            return;
        }

        if (slot == RACE_AFFINITIES_SLOT) {
            chatPromptManager.prompt(player, "Escribí: raza=fraccion,raza2=fraccion2 (ej. elf=0.25,demon=-0.2):",
                    value -> replace(new MagicSchool(current.id(), current.displayName(), current.color(),
                            current.icon(), current.description(), current.castSoundOnCast(),
                            current.castEffectId(), parseAffinities(value), current.classAffinities())));
            return;
        }

        if (slot == CLASS_AFFINITIES_SLOT) {
            chatPromptManager.prompt(player, "Escribí: clase=fraccion,clase2=fraccion2 (ej. paladin=0.3):",
                    value -> replace(new MagicSchool(current.id(), current.displayName(), current.color(),
                            current.icon(), current.description(), current.castSoundOnCast(),
                            current.castEffectId(), current.raceAffinities(), parseAffinities(value))));
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private Map<String, Double> parseAffinities(String raw) {

        Map<String, Double> result = new LinkedHashMap<>();

        for (String pair : raw.split(",")) {

            String[] kv = pair.split("=", 2);

            if (kv.length == 2) {
                try {
                    result.put(kv[0].trim().toLowerCase(java.util.Locale.ROOT), Double.parseDouble(kv[1].trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return result;
    }

}
