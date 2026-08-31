package com.sack.rpgroll.ranching.gui;

import com.sack.rpgroll.util.ComponentUtils;

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
        super(player, ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.gene.editor.title", "id", gene.id()), NamedTextColor.GOLD), SIZE);
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

        var lang = chatPromptManager.lang();

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(lang.component("gui.editor.name_line", "name", current.displayName())).build());

        setItem(ATTRIBUTE_KEY_SLOT, new ItemBuilder(Material.COMPASS)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.gene.editor.attribute_key_line", "key", current.attributeKey()), NamedTextColor.AQUA))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.gene.editor.attribute_key_hint"), NamedTextColor.GRAY)).build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.editor.description_title"), NamedTextColor.YELLOW))
                .setLore(ItemBuilder.toLoreLines(current.description().isBlank() ? lang.raw("gui.editor.no_description") : current.description()))
                .build());

        setItem(DOMINANCE_SLOT, new ItemBuilder(Material.REPEATER)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.gene.editor.dominance_line", "dominance", current.dominance()), NamedTextColor.LIGHT_PURPLE))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.gene.editor.dominance_hint"), NamedTextColor.GRAY)).build());

        setItem(MIN_VALUE_SLOT, new ItemBuilder(Material.RED_DYE)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.gene.editor.min_value_line", "value", current.minValue()), NamedTextColor.WHITE))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.editor.step5_hint"), NamedTextColor.GRAY)).build());

        setItem(MAX_VALUE_SLOT, new ItemBuilder(Material.LIME_DYE)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.gene.editor.max_value_line", "value", current.maxValue()), NamedTextColor.WHITE))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.editor.step5_hint"), NamedTextColor.GRAY)).build());

        setItem(SPECIES_SLOT, new ItemBuilder(Material.COW_SPAWN_EGG)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.gene.editor.species_line", "species",
                        current.applicableSpecies().isEmpty() ? lang.raw("gui.gene.editor.any_species") : String.join(", ", current.applicableSpecies())), NamedTextColor.GOLD))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.gene.editor.species_hint"), NamedTextColor.GRAY))
                .build());

        setItem(MUTATIONS_SLOT, new ItemBuilder(Material.DRAGON_BREATH)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.gene.editor.mutations_line", "count", current.mutations().size()), NamedTextColor.LIGHT_PURPLE))
                .setLore(ItemBuilder.toLoreLines(lang.raw("gui.gene.editor.mutations_hint")))
                .build());

        setItem(CLEAR_MUTATIONS_SLOT, new ItemBuilder(Material.BARRIER)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.gene.editor.clear_mutations"), NamedTextColor.RED)).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        double sign = event.getClick() == ClickType.RIGHT ? -5 : 5;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.editor.prompt_name"), value -> replace(withDisplayName(value)));
        } else if (slot == ATTRIBUTE_KEY_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.gene.editor.prompt_attribute_key"), value -> replace(withAttributeKey(value)));
        } else if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.editor.prompt_description"), value -> replace(withDescription(value)));
        } else if (slot == DOMINANCE_SLOT) {
            GeneDominance[] values = GeneDominance.values();
            replace(withDominance(values[(current.dominance().ordinal() + 1) % values.length]));
        } else if (slot == MIN_VALUE_SLOT) {
            replace(withMinValue(current.minValue() + sign));
        } else if (slot == MAX_VALUE_SLOT) {
            replace(withMaxValue(current.maxValue() + sign));
        } else if (slot == SPECIES_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.gene.editor.prompt_species"),
                    value -> replace(withApplicableSpecies(parseSet(value))));
        } else if (slot == MUTATIONS_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.gene.editor.prompt_add_mutation"),
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
            player.sendMessage(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.gene.editor.invalid_format"), NamedTextColor.RED));
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
            player.sendMessage(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.gene.editor.invalid_value"), NamedTextColor.RED));
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
