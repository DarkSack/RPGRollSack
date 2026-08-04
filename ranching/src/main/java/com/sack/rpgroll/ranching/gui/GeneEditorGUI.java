package com.sack.rpgroll.ranching.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.ranching.core.genetics.Gene;
import com.sack.rpgroll.ranching.core.genetics.GeneDominance;
import com.sack.rpgroll.ranching.core.genetics.GeneManager;
import com.sack.rpgroll.ranching.core.genetics.GeneMutation;
import com.sack.rpgroll.ranching.core.genetics.MutationEffectType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class GeneEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int NAME_SLOT = 9;
    private static final int ATTRIBUTE_KEY_SLOT = 10;
    private static final int DESCRIPTION_SLOT = 11;
    private static final int DOMINANCE_SLOT = 12;
    private static final int MIN_VALUE_SLOT = 13;
    private static final int MAX_VALUE_SLOT = 14;
    private static final int SPECIES_SLOT = 15;
    private static final int MUTATIONS_SLOT = 16;
    private static final int CLEAR_MUTATIONS_SLOT = 17;
    private static final int BACK_SLOT = 40;

    private final GeneManager geneManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private Gene current;

    public GeneEditorGUI(Player player, Gene gene, GeneManager geneManager, ChatPromptManager chatPromptManager,
            Runnable onBack) {
        super(player, Component.text("Gen: " + gene.id(), NamedTextColor.GOLD), SIZE);
        this.current = gene;
        this.geneManager = geneManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(Gene updated) {
        current = updated;
        geneManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(Component.text("Nombre: " + current.displayName(), NamedTextColor.YELLOW)).build());

        setItem(ATTRIBUTE_KEY_SLOT, new ItemBuilder(Material.COMPASS)
                .setName(Component.text("attribute-key: " + current.attributeKey(), NamedTextColor.AQUA))
                .setLore(Component.text("Ej. milk_production, growth_speed, fertility", NamedTextColor.GRAY)).build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(Component.text("Descripción", NamedTextColor.YELLOW))
                .setLore(ItemBuilder.toLoreLines(current.description().isBlank() ? "(sin descripción)" : current.description()))
                .build());

        setItem(DOMINANCE_SLOT, new ItemBuilder(Material.REPEATER)
                .setName(Component.text("Dominancia: " + current.dominance(), NamedTextColor.LIGHT_PURPLE))
                .setLore(Component.text("Click para rotar: DOMINANT → RECESSIVE → MIXED", NamedTextColor.GRAY)).build());

        setItem(MIN_VALUE_SLOT, new ItemBuilder(Material.RED_DYE)
                .setName(Component.text("Mínimo: " + current.minValue(), NamedTextColor.WHITE))
                .setLore(Component.text("Click: +5 · Click derecho: -5", NamedTextColor.GRAY)).build());

        setItem(MAX_VALUE_SLOT, new ItemBuilder(Material.LIME_DYE)
                .setName(Component.text("Máximo: " + current.maxValue(), NamedTextColor.WHITE))
                .setLore(Component.text("Click: +5 · Click derecho: -5", NamedTextColor.GRAY)).build());

        setItem(SPECIES_SLOT, new ItemBuilder(Material.COW_SPAWN_EGG)
                .setName(Component.text("Especies: "
                        + (current.applicableSpecies().isEmpty() ? "cualquiera" : String.join(", ", current.applicableSpecies())),
                        NamedTextColor.GOLD))
                .setLore(Component.text("Escribí ids separados por comas — vacío = cualquiera", NamedTextColor.GRAY))
                .build());

        setItem(MUTATIONS_SLOT, new ItemBuilder(Material.DRAGON_BREATH)
                .setName(Component.text("Mutaciones: " + current.mutations().size(), NamedTextColor.LIGHT_PURPLE))
                .setLore(ItemBuilder.toLoreLines(
                        "Formato: id;nombre;TIPO;valor;chance\nTIPO: MULTIPLY, OVERRIDE, COSMETIC_TAG\nEscribí para AGREGAR una."))
                .build());

        setItem(CLEAR_MUTATIONS_SLOT, new ItemBuilder(Material.BARRIER)
                .setName(Component.text("Borrar todas las mutaciones", NamedTextColor.RED)).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        double sign = event.getClick() == ClickType.RIGHT ? -5 : 5;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo nombre:", value -> replace(withDisplayName(value)));
        } else if (slot == ATTRIBUTE_KEY_SLOT) {
            chatPromptManager.prompt(player, "Escribí el attribute-key:", value -> replace(withAttributeKey(value)));
        } else if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, "Escribí la nueva descripción:", value -> replace(withDescription(value)));
        } else if (slot == DOMINANCE_SLOT) {
            GeneDominance[] values = GeneDominance.values();
            replace(withDominance(values[(current.dominance().ordinal() + 1) % values.length]));
        } else if (slot == MIN_VALUE_SLOT) {
            replace(withMinValue(current.minValue() + sign));
        } else if (slot == MAX_VALUE_SLOT) {
            replace(withMaxValue(current.maxValue() + sign));
        } else if (slot == SPECIES_SLOT) {
            chatPromptManager.prompt(player, "Escribí ids de especie separados por comas (vacío = cualquiera):",
                    value -> replace(withApplicableSpecies(parseSet(value))));
        } else if (slot == MUTATIONS_SLOT) {
            chatPromptManager.prompt(player, "Escribí 'id;nombre;TIPO;valor;chance' para agregar una mutación:",
                    this::addMutation);
        } else if (slot == CLEAR_MUTATIONS_SLOT) {
            replace(withMutations(List.of()));
        } else if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void addMutation(String raw) {

        String[] parts = raw.split(";");

        if (parts.length < 5) {
            player.sendMessage(Component.text("Formato inválido — necesita 5 partes separadas por ';'.", NamedTextColor.RED));
            build();
            return;
        }

        MutationEffectType effectType;

        try {
            effectType = MutationEffectType.valueOf(parts[2].trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            effectType = MutationEffectType.COSMETIC_TAG;
        }

        double effectValue;
        double chance;

        try {
            effectValue = Double.parseDouble(parts[3].trim());
            chance = Double.parseDouble(parts[4].trim());
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Valor o chance inválidos.", NamedTextColor.RED));
            build();
            return;
        }

        List<GeneMutation> mutations = new ArrayList<>(current.mutations());
        mutations.add(new GeneMutation(parts[0].trim(), parts[1].trim(), effectType, effectValue, chance));

        replace(withMutations(mutations));
    }

    private Set<String> parseSet(String raw) {

        Set<String> result = new HashSet<>();

        for (String entry : raw.split(",")) {
            if (!entry.isBlank()) {
                result.add(entry.trim().toLowerCase(Locale.ROOT));
            }
        }

        return result;
    }

    private Gene withDisplayName(String value) {
        return new Gene(current.id(), value, current.description(), current.attributeKey(), current.dominance(),
                current.minValue(), current.maxValue(), current.applicableSpecies(), current.mutations());
    }

    private Gene withAttributeKey(String value) {
        return new Gene(current.id(), current.displayName(), current.description(), value, current.dominance(),
                current.minValue(), current.maxValue(), current.applicableSpecies(), current.mutations());
    }

    private Gene withDescription(String value) {
        return new Gene(current.id(), current.displayName(), value, current.attributeKey(), current.dominance(),
                current.minValue(), current.maxValue(), current.applicableSpecies(), current.mutations());
    }

    private Gene withDominance(GeneDominance value) {
        return new Gene(current.id(), current.displayName(), current.description(), current.attributeKey(), value,
                current.minValue(), current.maxValue(), current.applicableSpecies(), current.mutations());
    }

    private Gene withMinValue(double value) {
        return new Gene(current.id(), current.displayName(), current.description(), current.attributeKey(),
                current.dominance(), value, current.maxValue(), current.applicableSpecies(), current.mutations());
    }

    private Gene withMaxValue(double value) {
        return new Gene(current.id(), current.displayName(), current.description(), current.attributeKey(),
                current.dominance(), current.minValue(), value, current.applicableSpecies(), current.mutations());
    }

    private Gene withApplicableSpecies(Set<String> value) {
        return new Gene(current.id(), current.displayName(), current.description(), current.attributeKey(),
                current.dominance(), current.minValue(), current.maxValue(), value, current.mutations());
    }

    private Gene withMutations(List<GeneMutation> value) {
        return new Gene(current.id(), current.displayName(), current.description(), current.attributeKey(),
                current.dominance(), current.minValue(), current.maxValue(), current.applicableSpecies(), value);
    }

}
