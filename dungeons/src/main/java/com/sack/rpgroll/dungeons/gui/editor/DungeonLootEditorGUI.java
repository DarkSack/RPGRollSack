package com.sack.rpgroll.dungeons.gui.editor;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.util.ComponentUtils;
import com.sack.rpgroll.dungeons.core.DungeonLootEntry;
import com.sack.rpgroll.dungeons.core.LootScope;
import com.sack.rpgroll.dungeons.core.LootType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Editor de la loot table de la mazmorra. Alta/edición por línea de chat:
 * {@code TIPO referencia cantidadMin cantidadMax probabilidad scope}
 * (ej. {@code ITEM ancient_relic 1 1 25 PER_CONTRIBUTION}).
 */
public class DungeonLootEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int ADD_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final DungeonEditorSession session;
    private final Runnable onBack;
    private final LangManager lang;

    public DungeonLootEditorGUI(Player player, DungeonEditorSession session, Runnable onBack) {
        super(player, ComponentUtils.parse(session.chatPromptManager.lang()
                .raw("gui.editor.loot.title", "id", session.original.id())), SIZE);
        this.session = session;
        this.onBack = onBack;
        this.lang = session.chatPromptManager.lang();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        List<DungeonLootEntry> loot = session.loot;

        for (int i = 0; i < loot.size() && i < 36; i++) {

            DungeonLootEntry entry = loot.get(i);

            setItem(i, new ItemBuilder(iconFor(entry.type()))
                    .setName(Component.text(entry.type() + ": "
                            + (entry.reference() != null ? entry.reference() : "-"), NamedTextColor.YELLOW))
                    .setLore(
                            ComponentUtils.parse(lang.raw("gui.editor.loot.item.amount",
                                    "min", entry.amountMin(), "max", entry.amountMax())),
                            ComponentUtils.parse(lang.raw("gui.editor.loot.item.chance",
                                    "chance", entry.chance(), "scope", entry.scope())),
                            ComponentUtils.parse(lang.raw("gui.editor.loot.item.replace_remove_hint")))
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(ComponentUtils.parse(lang.raw("gui.editor.loot.add")))
                .setLore(Component.text(lang.raw("gui.editor.loot.format_hint_short"), NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    private Material iconFor(LootType type) {
        return switch (type) {
            case ITEM -> Material.CHEST;
            case MONEY -> Material.GOLD_INGOT;
            case EXPERIENCE -> Material.EXPERIENCE_BOTTLE;
            case COMMAND -> Material.COMMAND_BLOCK;
            case QUEST -> Material.WRITTEN_BOOK;
        };
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < session.loot.size() && slot < 36) {

            if (event.isShiftClick()) {
                List<DungeonLootEntry> updated = new ArrayList<>(session.loot);
                updated.remove(slot);
                session.loot = updated;
                build();
                return;
            }

            promptReplace(slot);
            return;
        }

        if (slot == ADD_SLOT) {
            promptAdd();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void promptAdd() {
        session.chatPromptManager.prompt(player, "gui.editor.loot.prompt.add", value -> {

            DungeonLootEntry entry = parse(value);
            if (entry == null) {
                return;
            }

            List<DungeonLootEntry> updated = new ArrayList<>(session.loot);
            updated.add(entry);
            session.loot = updated;

            build();
        });
    }

    private void promptReplace(int index) {
        session.chatPromptManager.prompt(player, "gui.editor.loot.prompt.replace", value -> {

            DungeonLootEntry entry = parse(value);
            if (entry == null) {
                return;
            }

            List<DungeonLootEntry> updated = new ArrayList<>(session.loot);
            updated.set(index, entry);
            session.loot = updated;

            build();
        });
    }

    private DungeonLootEntry parse(String raw) {

        String[] parts = raw.trim().split("\\s+");

        if (parts.length != 6) {
            lang.send(player, "gui.editor.loot.invalid_field_count");
            return null;
        }

        try {
            LootType type = LootType.valueOf(parts[0].toUpperCase(Locale.ROOT));
            String reference = parts[1].equals("-") ? null : parts[1];
            int amountMin = Integer.parseInt(parts[2]);
            int amountMax = Integer.parseInt(parts[3]);
            double chance = Double.parseDouble(parts[4]);
            LootScope scope = LootScope.valueOf(parts[5].toUpperCase(Locale.ROOT));

            return new DungeonLootEntry(type, reference, amountMin, amountMax, chance, scope);
        } catch (IllegalArgumentException e) {
            lang.send(player, "gui.editor.loot.invalid_value");
            return null;
        }
    }

}
