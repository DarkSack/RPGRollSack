package com.sack.rpgroll.ascension.gui;

import com.sack.rpgroll.ascension.core.ClassSpecialization;
import com.sack.rpgroll.ascension.core.ClassSpecializationManager;
import com.sack.rpgroll.ascension.core.TalentNode;
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
    private ClassSpecialization current;

    public ClassSpecializationEditorGUI(Player player, ClassSpecialization specialization,
            ClassSpecializationManager manager, ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Especialización: " + specialization.id(), NamedTextColor.GOLD), SIZE);
        this.current = specialization;
        this.manager = manager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
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
                .setName(Component.text("Clase base: " + current.baseClass(), NamedTextColor.YELLOW))
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

        setItem(RESTRICTIONS_SLOT, new ItemBuilder(Material.BARRIER)
                .setName(Component.text("Restricciones: " + current.restrictions().size(), NamedTextColor.YELLOW))
                .setLore(Component.text(String.join(", ", current.restrictions()), NamedTextColor.GRAY),
                        Component.text("Click para escribir lista separada por comas", NamedTextColor.GRAY))
                .build());

        setItem(EQUIPMENT_SLOT, new ItemBuilder(Material.DIAMOND_CHESTPLATE)
                .setName(Component.text("Equipo exclusivo: " + current.exclusiveEquipment().size(),
                        NamedTextColor.YELLOW))
                .setLore(Component.text(String.join(", ", current.exclusiveEquipment()), NamedTextColor.GRAY),
                        Component.text("Click para escribir lista separada por comas", NamedTextColor.GRAY))
                .build());

        setItem(TALENT_ADD_SLOT, new ItemBuilder(Material.KNOWLEDGE_BOOK)
                .setName(Component.text("Talentos: " + current.talentTree().size() + " nodo(s)",
                        NamedTextColor.YELLOW))
                .setLore(joinTalentLore())
                .build());

        setItem(TALENT_REMOVE_SLOT, new ItemBuilder(Material.LAVA_BUCKET)
                .setName(Component.text("Eliminar un talento", NamedTextColor.RED))
                .setLore(Component.text("Click para escribir el id del nodo a eliminar", NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    private List<Component> joinTalentLore() {

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Click para agregar/actualizar un nodo:", NamedTextColor.GRAY));
        lore.add(Component.text("id;nombre;costo;requiere1,requiere2;stat=val;skill;trait;encantamiento",
                NamedTextColor.DARK_GRAY));
        lore.add(Component.text("(usá '-' para dejar un campo vacío)", NamedTextColor.DARK_GRAY));

        for (TalentNode node : current.talentTree()) {
            lore.add(Component.text("- " + node.id() + " (" + node.cost() + " pts)", NamedTextColor.AQUA));
        }

        return lore;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == BASE_CLASS_SLOT) {
            chatPromptManager.prompt(player, "Escribí el id de la clase base:",
                    value -> replace(new ClassSpecialization(current.id(), value.trim().toLowerCase(Locale.ROOT),
                            current.displayName(), current.requirements(), current.statBonus(),
                            current.restrictions(), current.exclusiveEquipment(), current.talentTree())));
            return;
        }

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo nombre:",
                    value -> replace(new ClassSpecialization(current.id(), current.baseClass(), value,
                            current.requirements(), current.statBonus(), current.restrictions(),
                            current.exclusiveEquipment(), current.talentTree())));
            return;
        }

        if (slot == REQUIREMENTS_SLOT) {
            chatPromptManager.prompt(player,
                    "Escribí: nivel;prestigio;trait;quest1,quest2;facción=rep (ej. 20;0;-;;reino=50):", value -> {
                        try {
                            replace(new ClassSpecialization(current.id(), current.baseClass(), current.displayName(),
                                    RequirementsPrompt.parse(value), current.statBonus(), current.restrictions(),
                                    current.exclusiveEquipment(), current.talentTree()));
                        } catch (NumberFormatException e) {
                            player.sendMessage(Component.text("Formato inválido.", NamedTextColor.RED));
                        }
                    });
            return;
        }

        if (slot == STATS_SLOT) {
            chatPromptManager.prompt(player, "Escribí: stat=valor,stat2=valor2:", value -> {
                try {
                    replace(new ClassSpecialization(current.id(), current.baseClass(), current.displayName(),
                            current.requirements(), NumberMapPrompt.parse(value), current.restrictions(),
                            current.exclusiveEquipment(), current.talentTree()));
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Formato inválido.", NamedTextColor.RED));
                }
            });
            return;
        }

        if (slot == RESTRICTIONS_SLOT) {
            chatPromptManager.prompt(player, "Escribí las restricciones, separadas por comas:",
                    value -> replace(new ClassSpecialization(current.id(), current.baseClass(), current.displayName(),
                            current.requirements(), current.statBonus(), List.of(value.trim().split(",")),
                            current.exclusiveEquipment(), current.talentTree())));
            return;
        }

        if (slot == EQUIPMENT_SLOT) {
            chatPromptManager.prompt(player, "Escribí el equipo exclusivo, separado por comas:",
                    value -> replace(new ClassSpecialization(current.id(), current.baseClass(), current.displayName(),
                            current.requirements(), current.statBonus(), current.restrictions(),
                            List.of(value.trim().split(",")), current.talentTree())));
            return;
        }

        if (slot == TALENT_ADD_SLOT) {
            chatPromptManager.prompt(player,
                    "Escribí: id;nombre;costo;requiere1,requiere2;stat=val;skill;trait;encantamiento", value -> {
                        try {
                            TalentNode node = parseTalentNode(value);
                            List<TalentNode> nodes = new ArrayList<>(current.talentTree());
                            nodes.removeIf(existing -> existing.id().equalsIgnoreCase(node.id()));
                            nodes.add(node);
                            replace(new ClassSpecialization(current.id(), current.baseClass(), current.displayName(),
                                    current.requirements(), current.statBonus(), current.restrictions(),
                                    current.exclusiveEquipment(), nodes));
                        } catch (RuntimeException e) {
                            player.sendMessage(Component.text("Formato inválido.", NamedTextColor.RED));
                        }
                    });
            return;
        }

        if (slot == TALENT_REMOVE_SLOT) {
            chatPromptManager.prompt(player, "Escribí el id del nodo a eliminar:", value -> {

                List<TalentNode> nodes = new ArrayList<>(current.talentTree());
                boolean removed = nodes.removeIf(node -> node.id().equalsIgnoreCase(value.trim()));

                if (!removed) {
                    player.sendMessage(Component.text("No se encontró ese nodo.", NamedTextColor.RED));
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
