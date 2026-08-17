package com.sack.rpgroll.ascension.gui;

import com.sack.rpgroll.ascension.core.ClassSpecialization;
import com.sack.rpgroll.ascension.core.ClassSpecializationManager;
import com.sack.rpgroll.ascension.core.TalentNode;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Editor de una especialización de clase. El árbol de talentos se edita con
 * una línea de chat en formato compacto (no hay grilla visual por nodo, dado
 * que pueden ser decenas y con prerequisitos cruzados):
 * id;nombre;costo;requiere1,requiere2;stat=val,stat2=val;skill;trait;encantamiento
 * (usar "-" para dejar un campo vacío/null).
 */
public class ClassSpecializationEditorGUI extends InventoryGUI {

    private static final int SIZE = 36;
    private static final int BASE_CLASS_SLOT = 9;
    private static final int NAME_SLOT = 10;
    private static final int REQUIREMENTS_SLOT = 11;
    private static final int STATS_SLOT = 12;
    private static final int RESTRICTIONS_SLOT = 13;
    private static final int EQUIPMENT_SLOT = 14;
    private static final int TALENT_ADD_SLOT = 15;
    private static final int TALENT_REMOVE_SLOT = 16;
    private static final int BACK_SLOT = 35;

    private final ClassSpecializationManager manager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private final LangManager lang;
    private ClassSpecialization current;

    public ClassSpecializationEditorGUI(Player player, ClassSpecialization specialization,
            ClassSpecializationManager manager, ChatPromptManager chatPromptManager, Runnable onBack,
            LangManager lang) {
        super(player, lang.component("gui.class_specialization.editor_title", "id", specialization.id()), SIZE);
        this.current = specialization;
        this.manager = manager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.lang = lang;
    }

    private void replace(ClassSpecialization updated) {
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

        setItem(BASE_CLASS_SLOT, new ItemBuilder(Material.DIAMOND_SWORD)
                .setName(lang.component("gui.class_specialization.base_class_label", "class", current.baseClass()))
                .setLore(lang.component("gui.class_specialization.click_to_change"))
                .build());

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(lang.component("gui.common.name_label", "name", current.displayName()))
                .setLore(lang.component("gui.common.click_new_value"))
                .build());

        setItem(REQUIREMENTS_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(lang.component("gui.class_specialization.requirements_label"))
                .setLore(Component.text(RequirementsPrompt.format(current.requirements()), NamedTextColor.GRAY),
                        lang.component("gui.class_specialization.requirements_hint"))
                .build());

        setItem(STATS_SLOT, new ItemBuilder(Material.IRON_SWORD)
                .setName(lang.component("gui.class_specialization.stats_label", "count",
                        current.statBonus().size()))
                .setLore(Component.text(NumberMapPrompt.format(current.statBonus()), NamedTextColor.GRAY),
                        lang.component("gui.class_specialization.stats_hint"))
                .build());

        setItem(RESTRICTIONS_SLOT, new ItemBuilder(Material.BARRIER)
                .setName(lang.component("gui.class_specialization.restrictions_label", "count",
                        current.restrictions().size()))
                .setLore(Component.text(String.join(", ", current.restrictions()), NamedTextColor.GRAY),
                        lang.component("gui.common.comma_hint"))
                .build());

        setItem(EQUIPMENT_SLOT, new ItemBuilder(Material.DIAMOND_CHESTPLATE)
                .setName(lang.component("gui.class_specialization.equipment_label", "count",
                        current.exclusiveEquipment().size()))
                .setLore(Component.text(String.join(", ", current.exclusiveEquipment()), NamedTextColor.GRAY),
                        lang.component("gui.common.comma_hint"))
                .build());

        setItem(TALENT_ADD_SLOT, new ItemBuilder(Material.KNOWLEDGE_BOOK)
                .setName(lang.component("gui.class_specialization.talents_label", "count",
                        current.talentTree().size()))
                .setLore(joinTalentLore())
                .build());

        setItem(TALENT_REMOVE_SLOT, new ItemBuilder(Material.LAVA_BUCKET)
                .setName(lang.component("gui.class_specialization.talents_remove"))
                .setLore(lang.component("gui.class_specialization.talents_remove_hint"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back_button")));
    }

    private List<Component> joinTalentLore() {

        List<Component> lore = new ArrayList<>();
        lore.add(lang.component("gui.class_specialization.talents_add_hint"));
        lore.add(lang.component("gui.class_specialization.talents_format_hint"));
        lore.add(lang.component("gui.class_specialization.talents_dash_hint"));

        for (TalentNode node : current.talentTree()) {
            lore.add(lang.component("gui.class_specialization.talents_node_entry", "id", node.id(), "cost",
                    node.cost()));
        }

        return lore;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == BASE_CLASS_SLOT) {
            chatPromptManager.prompt(player, "gui.class_specialization.prompt_base_class",
                    value -> replace(new ClassSpecialization(current.id(), value.trim().toLowerCase(Locale.ROOT),
                            current.displayName(), current.requirements(), current.statBonus(),
                            current.restrictions(), current.exclusiveEquipment(), current.talentTree())));
            return;
        }

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "gui.class_specialization.prompt_new_name",
                    value -> replace(new ClassSpecialization(current.id(), current.baseClass(), value,
                            current.requirements(), current.statBonus(), current.restrictions(),
                            current.exclusiveEquipment(), current.talentTree())));
            return;
        }

        if (slot == REQUIREMENTS_SLOT) {
            chatPromptManager.prompt(player, "gui.class_specialization.prompt_requirements", value -> {
                        try {
                            replace(new ClassSpecialization(current.id(), current.baseClass(), current.displayName(),
                                    RequirementsPrompt.parse(value), current.statBonus(), current.restrictions(),
                                    current.exclusiveEquipment(), current.talentTree()));
                        } catch (NumberFormatException e) {
                            lang.send(player, "gui.common.invalid_format");
                        }
                    });
            return;
        }

        if (slot == STATS_SLOT) {
            chatPromptManager.prompt(player, "gui.class_specialization.prompt_stats", value -> {
                try {
                    replace(new ClassSpecialization(current.id(), current.baseClass(), current.displayName(),
                            current.requirements(), NumberMapPrompt.parse(value), current.restrictions(),
                            current.exclusiveEquipment(), current.talentTree()));
                } catch (NumberFormatException e) {
                    lang.send(player, "gui.common.invalid_format");
                }
            });
            return;
        }

        if (slot == RESTRICTIONS_SLOT) {
            chatPromptManager.prompt(player, "gui.class_specialization.prompt_restrictions",
                    value -> replace(new ClassSpecialization(current.id(), current.baseClass(), current.displayName(),
                            current.requirements(), current.statBonus(), List.of(value.trim().split(",")),
                            current.exclusiveEquipment(), current.talentTree())));
            return;
        }

        if (slot == EQUIPMENT_SLOT) {
            chatPromptManager.prompt(player, "gui.class_specialization.prompt_equipment",
                    value -> replace(new ClassSpecialization(current.id(), current.baseClass(), current.displayName(),
                            current.requirements(), current.statBonus(), current.restrictions(),
                            List.of(value.trim().split(",")), current.talentTree())));
            return;
        }

        if (slot == TALENT_ADD_SLOT) {
            chatPromptManager.prompt(player, "gui.class_specialization.prompt_talent_add", value -> {
                        try {
                            TalentNode node = parseTalentNode(value);
                            List<TalentNode> nodes = new ArrayList<>(current.talentTree());
                            nodes.removeIf(existing -> existing.id().equalsIgnoreCase(node.id()));
                            nodes.add(node);
                            replace(new ClassSpecialization(current.id(), current.baseClass(), current.displayName(),
                                    current.requirements(), current.statBonus(), current.restrictions(),
                                    current.exclusiveEquipment(), nodes));
                        } catch (RuntimeException e) {
                            lang.send(player, "gui.common.invalid_format");
                        }
                    });
            return;
        }

        if (slot == TALENT_REMOVE_SLOT) {
            chatPromptManager.prompt(player, "gui.class_specialization.prompt_talent_remove", value -> {

                List<TalentNode> nodes = new ArrayList<>(current.talentTree());
                boolean removed = nodes.removeIf(node -> node.id().equalsIgnoreCase(value.trim()));

                if (!removed) {
                    lang.send(player, "gui.common.node_not_found");
                    return;
                }

                replace(new ClassSpecialization(current.id(), current.baseClass(), current.displayName(),
                        current.requirements(), current.statBonus(), current.restrictions(),
                        current.exclusiveEquipment(), nodes));
            });
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private TalentNode parseTalentNode(String line) {

        String[] parts = line.split(";", -1);

        if (parts.length < 8) {
            throw new IllegalArgumentException("faltan campos");
        }

        String id = parts[0].trim();
        String displayName = parts[1].trim();
        int cost = Integer.parseInt(parts[2].trim());
        List<String> prerequisites = parts[3].isBlank() || parts[3].trim().equals("-") ? List.of()
                : List.of(parts[3].trim().split(","));
        Map<String, Double> stats = parts[4].trim().equals("-") ? Map.of() : NumberMapPrompt.parse(parts[4]);
        String skill = parts[5].trim().equals("-") || parts[5].isBlank() ? null : parts[5].trim();
        String trait = parts[6].trim().equals("-") || parts[6].isBlank() ? null : parts[6].trim();
        String enchantment = parts[7].trim().equals("-") || parts[7].isBlank() ? null : parts[7].trim();

        return new TalentNode(id, displayName, cost, prerequisites, stats, skill, trait, enchantment);
    }

}
