package com.sack.rpgroll.ascension.gui;

import com.sack.rpgroll.ascension.core.RaceEvolution;
import com.sack.rpgroll.ascension.core.RaceEvolutionManager;
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
    private RaceEvolution current;

    public RaceEvolutionEditorGUI(Player player, RaceEvolution evolution, RaceEvolutionManager manager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Evolución: " + evolution.id(), NamedTextColor.GOLD), SIZE);
        this.current = evolution;
        this.manager = manager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
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
                .setName(Component.text("Raza base: " + current.baseRace(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para cambiarla", NamedTextColor.GRAY))
                .build());

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(Component.text("Nombre: " + current.displayName(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir uno nuevo", NamedTextColor.GRAY))
                .build());

        setItem(REQUIREMENTS_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(Component.text("Requisitos", NamedTextColor.YELLOW))
                .setLore(Component.text(RequirementsPrompt.format(current.requirements()), NamedTextColor.GRAY),
                        Component.text("Click para escribir: nivel;prestigio;trait;quests;facción=rep",
                                NamedTextColor.DARK_GRAY))
                .build());

        setItem(STATS_SLOT, new ItemBuilder(Material.IRON_SWORD)
                .setName(Component.text("Bono de stats: " + current.statBonus().size(), NamedTextColor.YELLOW))
                .setLore(Component.text(NumberMapPrompt.format(current.statBonus()), NamedTextColor.GRAY),
                        Component.text("Click para escribir: stat=valor,stat2=valor2", NamedTextColor.DARK_GRAY))
                .build());

        setItem(TRAITS_SLOT, new ItemBuilder(Material.NETHER_STAR)
                .setName(Component.text("Traits otorgados: " + current.grantedTraits().size(), NamedTextColor.YELLOW))
                .setLore(Component.text(String.join(", ", current.grantedTraits()), NamedTextColor.GRAY),
                        Component.text("Click para escribir lista separada por comas", NamedTextColor.GRAY))
                .build());

        setItem(SKILLS_SLOT, new ItemBuilder(Material.BOOK)
                .setName(Component.text("Skills otorgadas: " + current.grantedSkills().size(), NamedTextColor.YELLOW))
                .setLore(Component.text(String.join(", ", current.grantedSkills()), NamedTextColor.GRAY),
                        Component.text("Click para escribir lista separada por comas", NamedTextColor.GRAY))
                .build());

        setItem(AFFINITIES_SLOT, new ItemBuilder(Material.BLAZE_POWDER)
                .setName(Component.text("Bono de afinidades: " + current.affinityBonus().size(),
                        NamedTextColor.YELLOW))
                .setLore(Component.text(NumberMapPrompt.format(current.affinityBonus()), NamedTextColor.GRAY),
                        Component.text("Click para escribir: afinidad=valor,afinidad2=valor2", NamedTextColor.DARK_GRAY))
                .build());

        setItem(RESISTANCES_SLOT, new ItemBuilder(Material.SHIELD)
                .setName(Component.text("Resistencias: " + current.resistances().size(), NamedTextColor.YELLOW))
                .setLore(Component.text(NumberMapPrompt.format(current.resistances()), NamedTextColor.GRAY),
                        Component.text("Click para escribir: causa=valor,causa2=valor2", NamedTextColor.DARK_GRAY))
                .build());

        setItem(WEAKNESSES_SLOT, new ItemBuilder(Material.TNT)
                .setName(Component.text("Debilidades: " + current.weaknesses().size(), NamedTextColor.YELLOW))
                .setLore(Component.text(NumberMapPrompt.format(current.weaknesses()), NamedTextColor.GRAY),
                        Component.text("Click para escribir: causa=valor,causa2=valor2", NamedTextColor.DARK_GRAY))
                .build());

        setItem(PROFESSIONS_SLOT, new ItemBuilder(Material.IRON_PICKAXE)
                .setName(Component.text("Profesiones desbloqueadas: " + current.unlockedProfessions().size(),
                        NamedTextColor.YELLOW))
                .setLore(Component.text(String.join(", ", current.unlockedProfessions()), NamedTextColor.GRAY),
                        Component.text("Click para escribir lista separada por comas", NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == BASE_RACE_SLOT) {
            chatPromptManager.prompt(player, "Escribí el id de la raza base:",
                    value -> replace(new RaceEvolution(current.id(), value.trim().toLowerCase(Locale.ROOT),
                            current.displayName(), current.requirements(), current.statBonus(),
                            current.grantedTraits(), current.grantedSkills(), current.affinityBonus(),
                            current.resistances(), current.weaknesses(), current.unlockedProfessions())));
            return;
        }

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo nombre:",
                    value -> replace(new RaceEvolution(current.id(), current.baseRace(), value,
                            current.requirements(), current.statBonus(), current.grantedTraits(),
                            current.grantedSkills(), current.affinityBonus(), current.resistances(),
                            current.weaknesses(), current.unlockedProfessions())));
            return;
        }

        if (slot == REQUIREMENTS_SLOT) {
            chatPromptManager.prompt(player,
                    "Escribí: nivel;prestigio;trait;quest1,quest2;facción=rep (ej. 20;0;-;;reino=50):", value -> {
                        try {
                            replace(new RaceEvolution(current.id(), current.baseRace(), current.displayName(),
                                    RequirementsPrompt.parse(value), current.statBonus(), current.grantedTraits(),
                                    current.grantedSkills(), current.affinityBonus(), current.resistances(),
                                    current.weaknesses(), current.unlockedProfessions()));
                        } catch (NumberFormatException e) {
                            player.sendMessage(Component.text("Formato inválido.", NamedTextColor.RED));
                        }
                    });
            return;
        }

        if (slot == STATS_SLOT) {
            chatPromptManager.prompt(player, "Escribí: stat=valor,stat2=valor2 (ej. health=4,speed=10):", value -> {
                try {
                    replace(new RaceEvolution(current.id(), current.baseRace(), current.displayName(),
                            current.requirements(), NumberMapPrompt.parse(value), current.grantedTraits(),
                            current.grantedSkills(), current.affinityBonus(), current.resistances(),
                            current.weaknesses(), current.unlockedProfessions()));
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Formato inválido.", NamedTextColor.RED));
                }
            });
            return;
        }

        if (slot == TRAITS_SLOT) {
            chatPromptManager.prompt(player, "Escribí los traits otorgados, separados por comas:",
                    value -> replace(new RaceEvolution(current.id(), current.baseRace(), current.displayName(),
                            current.requirements(), current.statBonus(), List.of(value.trim().split(",")),
                            current.grantedSkills(), current.affinityBonus(), current.resistances(),
                            current.weaknesses(), current.unlockedProfessions())));
            return;
        }

        if (slot == SKILLS_SLOT) {
            chatPromptManager.prompt(player, "Escribí las skills otorgadas, separadas por comas:",
                    value -> replace(new RaceEvolution(current.id(), current.baseRace(), current.displayName(),
                            current.requirements(), current.statBonus(), current.grantedTraits(),
                            List.of(value.trim().split(",")), current.affinityBonus(), current.resistances(),
                            current.weaknesses(), current.unlockedProfessions())));
            return;
        }

        if (slot == AFFINITIES_SLOT) {
            chatPromptManager.prompt(player, "Escribí: afinidad=valor,afinidad2=valor2:", value -> {
                try {
                    replace(new RaceEvolution(current.id(), current.baseRace(), current.displayName(),
                            current.requirements(), current.statBonus(), current.grantedTraits(),
                            current.grantedSkills(), NumberMapPrompt.parse(value), current.resistances(),
                            current.weaknesses(), current.unlockedProfessions()));
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Formato inválido.", NamedTextColor.RED));
                }
            });
            return;
        }

        if (slot == RESISTANCES_SLOT) {
            chatPromptManager.prompt(player, "Escribí: causa=valor,causa2=valor2:", value -> {
                try {
                    replace(new RaceEvolution(current.id(), current.baseRace(), current.displayName(),
                            current.requirements(), current.statBonus(), current.grantedTraits(),
                            current.grantedSkills(), current.affinityBonus(), NumberMapPrompt.parse(value),
                            current.weaknesses(), current.unlockedProfessions()));
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Formato inválido.", NamedTextColor.RED));
                }
            });
            return;
        }

        if (slot == WEAKNESSES_SLOT) {
            chatPromptManager.prompt(player, "Escribí: causa=valor,causa2=valor2:", value -> {
                try {
                    replace(new RaceEvolution(current.id(), current.baseRace(), current.displayName(),
                            current.requirements(), current.statBonus(), current.grantedTraits(),
                            current.grantedSkills(), current.affinityBonus(), current.resistances(),
                            NumberMapPrompt.parse(value), current.unlockedProfessions()));
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Formato inválido.", NamedTextColor.RED));
                }
            });
            return;
        }

        if (slot == PROFESSIONS_SLOT) {
            chatPromptManager.prompt(player, "Escribí las profesiones desbloqueadas, separadas por comas:",
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
