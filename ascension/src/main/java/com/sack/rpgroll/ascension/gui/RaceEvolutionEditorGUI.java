package com.sack.rpgroll.ascension.gui;

import com.sack.rpgroll.ascension.core.RaceEvolution;
import com.sack.rpgroll.ascension.core.RaceEvolutionManager;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

/**
 * Editor de una evolución de raza. Los mapas (stats/afinidades/resistencias/
 * debilidades) y {@code requirements} se editan con una línea de chat en
 * formato compacto — ver {@link NumberMapPrompt} y {@link RequirementsPrompt}.
 */
public class RaceEvolutionEditorGUI extends InventoryGUI {

    private static final int SIZE = 36;
    private static final int BASE_RACE_SLOT = 9;
    private static final int NAME_SLOT = 10;
    private static final int REQUIREMENTS_SLOT = 11;
    private static final int STATS_SLOT = 12;
    private static final int TRAITS_SLOT = 13;
    private static final int SKILLS_SLOT = 14;
    private static final int AFFINITIES_SLOT = 15;
    private static final int RESISTANCES_SLOT = 16;
    private static final int WEAKNESSES_SLOT = 17;
    private static final int PROFESSIONS_SLOT = 18;
    private static final int BACK_SLOT = 35;

    private final RaceEvolutionManager manager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private final LangManager lang;
    private RaceEvolution current;

    public RaceEvolutionEditorGUI(Player player, RaceEvolution evolution, RaceEvolutionManager manager,
            ChatPromptManager chatPromptManager, Runnable onBack, LangManager lang) {
        super(player, lang.component("gui.race_evolution.editor_title", "id", evolution.id()), SIZE);
        this.current = evolution;
        this.manager = manager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.lang = lang;
    }

    private void replace(RaceEvolution updated) {
        current = updated;
        manager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(BASE_RACE_SLOT, new ItemBuilder(Material.PLAYER_HEAD)
                .setName(lang.component("gui.race_evolution.base_race_label", "race", current.baseRace()))
                .setLore(lang.component("gui.race_evolution.click_to_change"))
                .build());

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(lang.component("gui.common.name_label", "name", current.displayName()))
                .setLore(lang.component("gui.common.click_new_value"))
                .build());

        setItem(REQUIREMENTS_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(lang.component("gui.race_evolution.requirements_label"))
                .setLore(Component.text(RequirementsPrompt.format(current.requirements()), NamedTextColor.GRAY),
                        lang.component("gui.race_evolution.requirements_hint"))
                .build());

        setItem(STATS_SLOT, new ItemBuilder(Material.IRON_SWORD)
                .setName(lang.component("gui.race_evolution.stats_label", "count", current.statBonus().size()))
                .setLore(Component.text(NumberMapPrompt.format(current.statBonus()), NamedTextColor.GRAY),
                        lang.component("gui.race_evolution.stats_hint"))
                .build());

        setItem(TRAITS_SLOT, new ItemBuilder(Material.NETHER_STAR)
                .setName(lang.component("gui.race_evolution.traits_label", "count",
                        current.grantedTraits().size()))
                .setLore(Component.text(String.join(", ", current.grantedTraits()), NamedTextColor.GRAY),
                        lang.component("gui.common.comma_hint"))
                .build());

        setItem(SKILLS_SLOT, new ItemBuilder(Material.BOOK)
                .setName(lang.component("gui.race_evolution.skills_label", "count",
                        current.grantedSkills().size()))
                .setLore(Component.text(String.join(", ", current.grantedSkills()), NamedTextColor.GRAY),
                        lang.component("gui.common.comma_hint"))
                .build());

        setItem(AFFINITIES_SLOT, new ItemBuilder(Material.BLAZE_POWDER)
                .setName(lang.component("gui.race_evolution.affinities_label", "count",
                        current.affinityBonus().size()))
                .setLore(Component.text(NumberMapPrompt.format(current.affinityBonus()), NamedTextColor.GRAY),
                        lang.component("gui.race_evolution.affinities_hint"))
                .build());

        setItem(RESISTANCES_SLOT, new ItemBuilder(Material.SHIELD)
                .setName(lang.component("gui.race_evolution.resistances_label", "count",
                        current.resistances().size()))
                .setLore(Component.text(NumberMapPrompt.format(current.resistances()), NamedTextColor.GRAY),
                        lang.component("gui.race_evolution.causes_hint"))
                .build());

        setItem(WEAKNESSES_SLOT, new ItemBuilder(Material.TNT)
                .setName(lang.component("gui.race_evolution.weaknesses_label", "count",
                        current.weaknesses().size()))
                .setLore(Component.text(NumberMapPrompt.format(current.weaknesses()), NamedTextColor.GRAY),
                        lang.component("gui.race_evolution.causes_hint"))
                .build());

        setItem(PROFESSIONS_SLOT, new ItemBuilder(Material.IRON_PICKAXE)
                .setName(lang.component("gui.race_evolution.professions_label", "count",
                        current.unlockedProfessions().size()))
                .setLore(Component.text(String.join(", ", current.unlockedProfessions()), NamedTextColor.GRAY),
                        lang.component("gui.common.comma_hint"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back_button")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == BASE_RACE_SLOT) {
            chatPromptManager.prompt(player, "gui.race_evolution.prompt_base_race",
                    value -> replace(new RaceEvolution(current.id(), value.trim().toLowerCase(Locale.ROOT),
                            current.displayName(), current.requirements(), current.statBonus(),
                            current.grantedTraits(), current.grantedSkills(), current.affinityBonus(),
                            current.resistances(), current.weaknesses(), current.unlockedProfessions())));
            return;
        }

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "gui.race_evolution.prompt_new_name",
                    value -> replace(new RaceEvolution(current.id(), current.baseRace(), value,
                            current.requirements(), current.statBonus(), current.grantedTraits(),
                            current.grantedSkills(), current.affinityBonus(), current.resistances(),
                            current.weaknesses(), current.unlockedProfessions())));
            return;
        }

        if (slot == REQUIREMENTS_SLOT) {
            chatPromptManager.prompt(player, "gui.race_evolution.prompt_requirements", value -> {
                        try {
                            replace(new RaceEvolution(current.id(), current.baseRace(), current.displayName(),
                                    RequirementsPrompt.parse(value), current.statBonus(), current.grantedTraits(),
                                    current.grantedSkills(), current.affinityBonus(), current.resistances(),
                                    current.weaknesses(), current.unlockedProfessions()));
                        } catch (NumberFormatException e) {
                            lang.send(player, "gui.common.invalid_format");
                        }
                    });
            return;
        }

        if (slot == STATS_SLOT) {
            chatPromptManager.prompt(player, "gui.race_evolution.prompt_stats", value -> {
                try {
                    replace(new RaceEvolution(current.id(), current.baseRace(), current.displayName(),
                            current.requirements(), NumberMapPrompt.parse(value), current.grantedTraits(),
                            current.grantedSkills(), current.affinityBonus(), current.resistances(),
                            current.weaknesses(), current.unlockedProfessions()));
                } catch (NumberFormatException e) {
                    lang.send(player, "gui.common.invalid_format");
                }
            });
            return;
        }

        if (slot == TRAITS_SLOT) {
            chatPromptManager.prompt(player, "gui.race_evolution.prompt_traits",
                    value -> replace(new RaceEvolution(current.id(), current.baseRace(), current.displayName(),
                            current.requirements(), current.statBonus(), List.of(value.trim().split(",")),
                            current.grantedSkills(), current.affinityBonus(), current.resistances(),
                            current.weaknesses(), current.unlockedProfessions())));
            return;
        }

        if (slot == SKILLS_SLOT) {
            chatPromptManager.prompt(player, "gui.race_evolution.prompt_skills",
                    value -> replace(new RaceEvolution(current.id(), current.baseRace(), current.displayName(),
                            current.requirements(), current.statBonus(), current.grantedTraits(),
                            List.of(value.trim().split(",")), current.affinityBonus(), current.resistances(),
                            current.weaknesses(), current.unlockedProfessions())));
            return;
        }

        if (slot == AFFINITIES_SLOT) {
            chatPromptManager.prompt(player, "gui.race_evolution.prompt_affinities", value -> {
                try {
                    replace(new RaceEvolution(current.id(), current.baseRace(), current.displayName(),
                            current.requirements(), current.statBonus(), current.grantedTraits(),
                            current.grantedSkills(), NumberMapPrompt.parse(value), current.resistances(),
                            current.weaknesses(), current.unlockedProfessions()));
                } catch (NumberFormatException e) {
                    lang.send(player, "gui.common.invalid_format");
                }
            });
            return;
        }

        if (slot == RESISTANCES_SLOT) {
            chatPromptManager.prompt(player, "gui.race_evolution.prompt_resistances", value -> {
                try {
                    replace(new RaceEvolution(current.id(), current.baseRace(), current.displayName(),
                            current.requirements(), current.statBonus(), current.grantedTraits(),
                            current.grantedSkills(), current.affinityBonus(), NumberMapPrompt.parse(value),
                            current.weaknesses(), current.unlockedProfessions()));
                } catch (NumberFormatException e) {
                    lang.send(player, "gui.common.invalid_format");
                }
            });
            return;
        }

        if (slot == WEAKNESSES_SLOT) {
            chatPromptManager.prompt(player, "gui.race_evolution.prompt_weaknesses", value -> {
                try {
                    replace(new RaceEvolution(current.id(), current.baseRace(), current.displayName(),
                            current.requirements(), current.statBonus(), current.grantedTraits(),
                            current.grantedSkills(), current.affinityBonus(), current.resistances(),
                            NumberMapPrompt.parse(value), current.unlockedProfessions()));
                } catch (NumberFormatException e) {
                    lang.send(player, "gui.common.invalid_format");
                }
            });
            return;
        }

        if (slot == PROFESSIONS_SLOT) {
            chatPromptManager.prompt(player, "gui.race_evolution.prompt_professions",
                    value -> replace(new RaceEvolution(current.id(), current.baseRace(), current.displayName(),
                            current.requirements(), current.statBonus(), current.grantedTraits(),
                            current.grantedSkills(), current.affinityBonus(), current.resistances(),
                            current.weaknesses(), List.of(value.trim().split(",")))));
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
