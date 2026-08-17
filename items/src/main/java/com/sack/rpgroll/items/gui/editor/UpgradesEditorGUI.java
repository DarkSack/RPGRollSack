package com.sack.rpgroll.items.gui.editor;

import com.sack.rpgroll.common.lang.LangManager;
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
    private final LangManager lang;
    private final Runnable onBack;

    public UpgradesEditorGUI(Player player, EditorSession session, Runnable onBack) {
        super(player, session.chatPromptManager.lang().component("editor.upgrades.title", "id", session.original.id()), SIZE);
        this.session = session;
        this.lang = session.chatPromptManager.lang();
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
                lore.add(lang.component("editor.upgrades.cost", "cost", upgrade.cost()));
            }
            lore.add(lang.component("editor.upgrades.hint"));

            setItem(i, new ItemBuilder(Material.ANVIL)
                    .setName(lang.component("editor.upgrades.level_label", "level", upgrade.level()))
                    .setLore(lore)
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("editor.upgrades.add"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("editor.common.back")));
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
        session.chatPromptManager.prompt(player, lang.raw("editor.upgrades.prompt_new"), value -> {

            String[] parts = value.trim().split("\\s+");

            if (parts.length < 1) {
                lang.send(player, "editor.common.invalid_format");
                return;
            }

            try {
                int level = Integer.parseInt(parts[0]);
                double cost = parts.length >= 2 ? Double.parseDouble(parts[1]) : 0;

                session.upgrades.add(new UpgradeLevel(level, Map.of(), null, null, cost, null, 0));
                session.upgrades.sort((a, b) -> Integer.compare(a.level(), b.level()));
            } catch (NumberFormatException e) {
                lang.send(player, "editor.common.invalid_number");
                return;
            }

            build();
        });
    }

    private void promptStatBonus(int index) {
        session.chatPromptManager.prompt(player, lang.raw("editor.upgrades.prompt_stat_bonus"), value -> {

            String[] parts = value.trim().split("\\s+");

            if (parts.length != 2) {
                lang.send(player, "editor.common.invalid_format");
                return;
            }

            UpgradeLevel current = session.upgrades.get(index);
            Map<String, Double> statBonus = new HashMap<>(current.statBonus());

            try {
                statBonus.put(parts[0].toLowerCase(Locale.ROOT), Double.parseDouble(parts[1]));
            } catch (NumberFormatException e) {
                lang.send(player, "editor.upgrades.invalid_value");
                return;
            }

            session.upgrades.set(index, new UpgradeLevel(current.level(), statBonus, current.displayNameOverride(),
                    current.rarityOverride(), current.cost(), current.costMaterial(), current.costAmount()));

            build();
        });
    }

}
