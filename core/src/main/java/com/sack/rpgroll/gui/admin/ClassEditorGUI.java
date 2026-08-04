package com.sack.rpgroll.gui.admin;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.api.playerclass.PlayerClass;
import com.sack.rpgroll.api.stats.StatType;
import com.sack.rpgroll.playerclass.ClassManagerImpl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int NAME_SLOT = 9;
    private static final int DESCRIPTION_SLOT = 10;
    private static final int ATTRIBUTES_SLOT = 11;
    private static final int PASSIVE_TRAITS_SLOT = 12;
    private static final int LORE_SLOT = 13;
    private static final int BACK_SLOT = 26;

    private final ClassManagerImpl classManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private PlayerClass current;

    public ClassEditorGUI(Player player, PlayerClass playerClass, ClassManagerImpl classManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Clase: " + playerClass.id(), NamedTextColor.GOLD), SIZE);
        this.current = playerClass;
        this.classManager = classManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(PlayerClass updated) {
        current = updated;
        classManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(LegacyComponentSerializer.legacyAmpersand()
                        .deserialize("Nombre: " + current.displayName()).colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir uno nuevo", NamedTextColor.GRAY))
                .build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(Component.text("Descripción", NamedTextColor.YELLOW))
                .setLore(ItemBuilder.toLoreLines(current.description().isBlank() ? "(sin descripción)"
                        : current.description()))
                .build());

        setItem(ATTRIBUTES_SLOT, new ItemBuilder(Material.BEACON)
                .setName(Component.text("Atributos base: " + current.baseAttributes(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir (ej. STR=3,CON=2)", NamedTextColor.GRAY))
                .build());

        setItem(PASSIVE_TRAITS_SLOT, new ItemBuilder(Material.BOOK)
                .setName(Component.text("Traits pasivos: " + String.join(", ", current.passiveTraits()),
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir la lista separada por comas", NamedTextColor.GRAY))
                .build());

        setItem(LORE_SLOT, new ItemBuilder(Material.PAPER)
                .setName(Component.text("Lore: " + current.lore().size() + " línea(s)", NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir todas separadas por ';'", NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo nombre:", value -> replace(new PlayerClass(
                    current.id(), value, current.description(), current.baseAttributes(), current.passiveTraits(),
                    current.icon(), current.lore())));
            return;
        }

        if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, "Escribí la nueva descripción:", value -> replace(new PlayerClass(
                    current.id(), current.displayName(), value, current.baseAttributes(), current.passiveTraits(),
                    current.icon(), current.lore())));
            return;
        }

        if (slot == ATTRIBUTES_SLOT) {
            chatPromptManager.prompt(player, "Escribí los atributos separados por comas (ej. STR=3,CON=2):",
                    value -> {
                        Map<StatType, Integer> attributes = parseAttributes(value);
                        replace(new PlayerClass(current.id(), current.displayName(), current.description(),
                                attributes, current.passiveTraits(), current.icon(), current.lore()));
                    });
            return;
        }

        if (slot == PASSIVE_TRAITS_SLOT) {
            chatPromptManager.prompt(player, "Escribí los ids de traits pasivos separados por comas:", value -> {
                List<String> traits = value.isBlank() ? List.of() : List.of(value.split("\\s*,\\s*"));
                replace(new PlayerClass(current.id(), current.displayName(), current.description(),
                        current.baseAttributes(), traits, current.icon(), current.lore()));
            });
            return;
        }

        if (slot == LORE_SLOT) {
            chatPromptManager.prompt(player, "Escribí las líneas de lore separadas por ';':", value -> {
                List<String> lore = value.isBlank() ? List.of() : List.of(value.split("\\s*;\\s*"));
                replace(new PlayerClass(current.id(), current.displayName(), current.description(),
                        current.baseAttributes(), current.passiveTraits(), current.icon(), lore));
            });
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private Map<StatType, Integer> parseAttributes(String raw) {

        Map<StatType, Integer> attributes = new HashMap<>();

        if (raw.isBlank()) {
            return attributes;
        }

        for (String part : raw.split("\\s*,\\s*")) {

            String[] kv = part.split("=", 2);
            if (kv.length != 2) {
                continue;
            }

            StatType stat = StatType.fromString(kv[0].trim());
            if (stat == null) {
                continue;
            }

            try {
                attributes.put(stat, Integer.parseInt(kv[1].trim()));
            } catch (NumberFormatException ignored) {
            }
        }

        return attributes;
    }

}
