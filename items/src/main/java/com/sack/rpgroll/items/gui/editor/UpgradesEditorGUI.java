package com.sack.rpgroll.items.gui.editor;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.items.core.UpgradeLevel;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Editor de niveles de mejora ("+1", "+2", ...). Click en un nivel
 * existente agrega/edita un bono de stat para ESE nivel; shift-click lo
 * elimina por completo.
 */
public class UpgradesEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int ADD_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final EditorSession session;
    private final Runnable onBack;

    public UpgradesEditorGUI(Player player, EditorSession session, Runnable onBack) {
        super(player, Component.text("Mejoras: " + session.original.id(), NamedTextColor.GOLD), SIZE);
        this.session = session;
        this.onBack = onBack;
        session.upgrades.sort((a, b) -> Integer.compare(a.level(), b.level()));
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int slot = 36; slot < SIZE; slot++) {
            setItem(slot, new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                    .setName(Component.text(" ", NamedTextColor.GRAY)).build());
        }

        for (int i = 0; i < session.upgrades.size() && i < 36; i++) {

            UpgradeLevel upgrade = session.upgrades.get(i);

            List<Component> lore = new ArrayList<>();
            for (var entry : upgrade.statBonus().entrySet()) {
                lore.add(Component.text("+ " + entry.getKey() + ": " + entry.getValue(), NamedTextColor.GRAY));
            }
            if (upgrade.cost() > 0) {
                lore.add(Component.text("Costo: " + upgrade.cost(), NamedTextColor.GOLD));
            }
            lore.add(Component.text("Click: agregar/editar stat · Shift-click: quitar nivel", NamedTextColor.DARK_GRAY));

            setItem(i, new ItemBuilder(Material.ANVIL)
                    .setName(Component.text("Nivel +" + upgrade.level(), NamedTextColor.YELLOW))
                    .setLore(lore)
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Agregar nivel de mejora", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < session.upgrades.size() && slot < 36) {

            if (event.isShiftClick()) {
                session.upgrades.remove(slot);
                build();
            } else {
                promptStatBonus(slot);
            }
            return;
        }

        if (slot == ADD_SLOT) {
            promptNewLevel();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void promptNewLevel() {
        session.chatPromptManager.prompt(player, "Escribí: <nivel> <costo> (ej. 1 100):", value -> {

            String[] parts = value.trim().split("\\s+");

            if (parts.length < 1) {
                player.sendMessage(Component.text("Formato inválido.", NamedTextColor.RED));
                return;
            }

            try {
                int level = Integer.parseInt(parts[0]);
                double cost = parts.length >= 2 ? Double.parseDouble(parts[1]) : 0;

                session.upgrades.add(new UpgradeLevel(level, Map.of(), null, null, cost, null, 0));
                session.upgrades.sort((a, b) -> Integer.compare(a.level(), b.level()));
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Número inválido.", NamedTextColor.RED));
                return;
            }

            build();
        });
    }

    private void promptStatBonus(int index) {
        session.chatPromptManager.prompt(player, "Escribí: <stat> <valor> para agregar a este nivel:", value -> {

            String[] parts = value.trim().split("\\s+");

            if (parts.length != 2) {
                player.sendMessage(Component.text("Formato inválido.", NamedTextColor.RED));
                return;
            }

            UpgradeLevel current = session.upgrades.get(index);
            Map<String, Double> statBonus = new HashMap<>(current.statBonus());

            try {
                statBonus.put(parts[0].toLowerCase(Locale.ROOT), Double.parseDouble(parts[1]));
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Valor inválido.", NamedTextColor.RED));
                return;
            }

            session.upgrades.set(index, new UpgradeLevel(current.level(), statBonus, current.displayNameOverride(),
                    current.rarityOverride(), current.cost(), current.costMaterial(), current.costAmount()));

            build();
        });
    }

}
