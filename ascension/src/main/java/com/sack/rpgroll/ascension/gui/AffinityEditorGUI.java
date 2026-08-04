package com.sack.rpgroll.ascension.gui;

import com.sack.rpgroll.ascension.core.Affinity;
import com.sack.rpgroll.ascension.core.AffinityManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class AffinityEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int NAME_SLOT = 10;
    private static final int OPPOSING_SLOT = 12;
    private static final int RESIST_SLOT = 14;
    private static final int BACK_SLOT = 26;

    private final AffinityManager manager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private Affinity current;

    public AffinityEditorGUI(Player player, Affinity affinity, AffinityManager manager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Afinidad: " + affinity.id(), NamedTextColor.GOLD), SIZE);
        this.current = affinity;
        this.manager = manager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(Affinity updated) {
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

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(Component.text("Nombre: " + current.displayName(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir uno nuevo", NamedTextColor.GRAY))
                .build());

        setItem(OPPOSING_SLOT, new ItemBuilder(Material.SHIELD)
                .setName(Component.text("Opuesta: " + (current.opposingId() == null ? "(ninguna)"
                        : current.opposingId()), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir el id de la afinidad opuesta", NamedTextColor.GRAY))
                .build());

        setItem(RESIST_SLOT, new ItemBuilder(Material.SHIELD)
                .setName(Component.text("Causas resistidas: " + current.resistCauses().size(), NamedTextColor.YELLOW))
                .setLore(Component.text(String.join(", ", current.resistCauses()), NamedTextColor.GRAY),
                        Component.text("Click para escribir lista separada por comas", NamedTextColor.GRAY),
                        Component.text("(ej. FIRE,LAVA,HOT_FLOOR)", NamedTextColor.DARK_GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo nombre:", value -> replace(
                    new Affinity(current.id(), value, current.opposingId(), current.resistCauses())));
            return;
        }

        if (slot == OPPOSING_SLOT) {
            chatPromptManager.prompt(player, "Escribí el id de la afinidad opuesta (o '-' para ninguna):", value -> {
                String opposing = value.trim().equals("-") ? null : value.trim();
                replace(new Affinity(current.id(), current.displayName(), opposing, current.resistCauses()));
            });
            return;
        }

        if (slot == RESIST_SLOT) {
            chatPromptManager.prompt(player, "Escribí las causas de daño resistidas, separadas por comas:",
                    value -> replace(new Affinity(current.id(), current.displayName(), current.opposingId(),
                            List.of(value.trim().toUpperCase().split(",")))));
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
