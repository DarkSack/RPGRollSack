package com.sack.rpgroll.dungeons.gui.editor;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.util.ComponentUtils;
import com.sack.rpgroll.dungeons.core.DungeonWave;
import com.sack.rpgroll.dungeons.core.DungeonWaveMob;
import com.sack.rpgroll.dungeons.util.DurationParser;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Editor de la lista de oleadas de una sala de combate (lista compartida
 * por referencia con {@code RoomEditGUI}). Alta por chat:
 * {@code id tiempoLímite tiempoAntes mob1:cantidad,mob2:cantidad} — ej.
 * {@code wave1 30s 5s skeleton_archer:3,zombie_grunt:2} (tiempos en 0
 * para "sin límite"/"sin demora").
 */
public class WavesEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int ADD_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final DungeonEditorSession session;
    private final List<DungeonWave> waves;
    private final Runnable onBack;
    private final LangManager lang;

    public WavesEditorGUI(Player player, DungeonEditorSession session, List<DungeonWave> waves, Runnable onBack) {
        super(player, ComponentUtils.parse(session.chatPromptManager.lang().raw("gui.editor.waves.title")), SIZE);
        this.session = session;
        this.waves = waves;
        this.onBack = onBack;
        this.lang = session.chatPromptManager.lang();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < waves.size() && i < 36; i++) {

            DungeonWave wave = waves.get(i);

            List<Component> lore = new ArrayList<>();
            lore.add(ComponentUtils.parse(lang.raw("gui.editor.waves.item.mobs", "count", wave.totalMobCount())));
            for (DungeonWaveMob mob : wave.mobs()) {
                lore.add(Component.text("  " + mob.mobId() + " x" + mob.amount(), NamedTextColor.DARK_GRAY));
            }
            lore.add(ComponentUtils.parse(lang.raw("gui.editor.waves.item.time_limit",
                    "seconds", wave.timeLimitMillis() / 1000)));
            lore.add(ComponentUtils.parse(lang.raw("gui.editor.actionlist.item.remove_hint")));

            setItem(i, new ItemBuilder(Material.SPAWNER)
                    .setName(Component.text(wave.id(), NamedTextColor.YELLOW))
                    .setLore(lore)
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(ComponentUtils.parse(lang.raw("gui.editor.waves.add")))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.editor.waves.format_hint"), NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < waves.size() && slot < 36) {
            if (event.isShiftClick()) {
                waves.remove(slot);
                build();
            }
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
        session.chatPromptManager.prompt(player, "gui.editor.waves.prompt.add", value -> {

            String[] tokens = value.trim().split("\\s+");

            if (tokens.length < 3) {
                lang.send(player, "gui.editor.actionlist.invalid_format");
                return;
            }

            String id = tokens[0];
            long timeLimitMillis = DurationParser.parseMillis(tokens[1]);
            long delayBeforeMillis = DurationParser.parseMillis(tokens[2]);

            List<DungeonWaveMob> mobs = new ArrayList<>();

            if (tokens.length >= 4) {
                for (String entry : tokens[3].split(",")) {

                    String[] parts = entry.split(":", 2);
                    if (parts.length != 2) {
                        continue;
                    }

                    try {
                        mobs.add(new DungeonWaveMob(parts[0], Integer.parseInt(parts[1])));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            waves.add(new DungeonWave(id, mobs, timeLimitMillis, delayBeforeMillis));
            build();
        });
    }

}
