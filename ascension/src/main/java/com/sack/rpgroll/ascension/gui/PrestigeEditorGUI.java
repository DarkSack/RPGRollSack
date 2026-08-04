package com.sack.rpgroll.ascension.gui;

import com.sack.rpgroll.ascension.core.PrestigeLevel;
import com.sack.rpgroll.ascension.core.PrestigeManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class PrestigeEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int LEVEL_SLOT = 10;
    private static final int BONUS_SLOT = 12;
    private static final int SKILLS_SLOT = 14;
    private static final int BACK_SLOT = 26;

    private final PrestigeManager manager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private PrestigeLevel current;

    public PrestigeEditorGUI(Player player, PrestigeLevel level, PrestigeManager manager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Prestigio: " + level.id(), NamedTextColor.GOLD), SIZE);
        this.current = level;
        this.manager = manager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(PrestigeLevel updated) {
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

        setItem(LEVEL_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(Component.text("Nivel requerido: " + current.requiredLevel(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +10 · Click derecho: -10", NamedTextColor.GRAY))
                .build());

        setItem(BONUS_SLOT, new ItemBuilder(Material.NETHER_STAR)
                .setName(Component.text("Bono de XP: " + current.expBonusPercent() + "%", NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +1 · Click derecho: -1", NamedTextColor.GRAY))
                .build());

        setItem(SKILLS_SLOT, new ItemBuilder(Material.BOOK)
                .setName(Component.text("Skills otorgadas: " + current.grantedSkills().size(), NamedTextColor.YELLOW))
                .setLore(Component.text(String.join(", ", current.grantedSkills()), NamedTextColor.GRAY),
                        Component.text("Click para escribir lista separada por comas", NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        ClickType click = event.getClick();

        if (slot == LEVEL_SLOT) {
            int delta = click == ClickType.RIGHT ? -10 : 10;
            replace(new PrestigeLevel(current.id(), Math.max(1, current.requiredLevel() + delta),
                    current.expBonusPercent(), current.grantedSkills()));
            return;
        }

        if (slot == BONUS_SLOT) {
            double delta = click == ClickType.RIGHT ? -1 : 1;
            replace(new PrestigeLevel(current.id(), current.requiredLevel(),
                    Math.max(0, current.expBonusPercent() + delta), current.grantedSkills()));
            return;
        }

        if (slot == SKILLS_SLOT) {
            chatPromptManager.prompt(player, "Escribí las skills otorgadas, separadas por comas:",
                    value -> replace(new PrestigeLevel(current.id(), current.requiredLevel(),
                            current.expBonusPercent(), List.of(value.trim().split(",")))));
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
