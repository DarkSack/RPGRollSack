package com.sack.rpgroll.dungeons.gui.editor;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.util.ComponentUtils;
import com.sack.rpgroll.dungeons.core.DungeonObjective;
import com.sack.rpgroll.dungeons.core.DungeonObjectiveType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Editor de la lista de objetivos de una sala (lista compartida por
 * referencia con {@code RoomEditGUI} — el "Volver" es lo que vuelca los
 * cambios, ver {@code RoomEditGUI.syncAndReopen}). Alta por chat:
 * {@code TIPO cantidad clave=valor clave2=valor2} (ej.
 * {@code KILL_ENTITY 5 entity=ZOMBIE}).
 */
public class ObjectivesEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int ADD_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final DungeonEditorSession session;
    private final List<DungeonObjective> objectives;
    private final Runnable onBack;
    private final LangManager lang;

    public ObjectivesEditorGUI(Player player, DungeonEditorSession session, List<DungeonObjective> objectives,
            Runnable onBack) {
        super(player, ComponentUtils.parse(session.chatPromptManager.lang().raw("gui.editor.objectives.title")),
                SIZE);
        this.session = session;
        this.objectives = objectives;
        this.onBack = onBack;
        this.lang = session.chatPromptManager.lang();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < objectives.size() && i < 36; i++) {

            DungeonObjective objective = objectives.get(i);

            setItem(i, new ItemBuilder(Material.TARGET)
                    .setName(Component.text(objective.type() + " x" + objective.amount(), NamedTextColor.YELLOW))
                    .setLore(
                            Component.text(objective.description(), NamedTextColor.GRAY),
                            Component.text(objective.params().toString(), NamedTextColor.DARK_GRAY),
                            ComponentUtils.parse(lang.raw("gui.editor.actionlist.item.remove_hint")))
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(ComponentUtils.parse(lang.raw("gui.editor.objectives.add")))
                .setLore(Component.text(lang.raw("gui.editor.objectives.format_hint"), NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < objectives.size() && slot < 36) {
            if (event.isShiftClick()) {
                objectives.remove(slot);
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
        session.chatPromptManager.prompt(player, "gui.editor.objectives.prompt.add", value -> {

            String[] tokens = value.trim().split("\\s+");

            if (tokens.length < 2) {
                lang.send(player, "gui.editor.actionlist.invalid_format");
                return;
            }

            DungeonObjectiveType type;
            try {
                type = DungeonObjectiveType.valueOf(tokens[0].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                lang.send(player, "gui.editor.objectives.invalid_type");
                return;
            }

            int amount;
            try {
                amount = Integer.parseInt(tokens[1]);
            } catch (NumberFormatException e) {
                lang.send(player, "gui.editor.objectives.invalid_amount");
                return;
            }

            Map<String, String> params = new HashMap<>();
            for (int i = 2; i < tokens.length; i++) {
                String[] kv = tokens[i].split("=", 2);
                if (kv.length == 2) {
                    params.put(kv[0], kv[1]);
                }
            }

            objectives.add(new DungeonObjective(type, null, amount, params));
            build();
        });
    }

}
