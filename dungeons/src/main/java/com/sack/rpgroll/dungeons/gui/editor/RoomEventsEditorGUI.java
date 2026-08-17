package com.sack.rpgroll.dungeons.gui.editor;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.util.ComponentUtils;
import com.sack.rpgroll.dungeons.core.DungeonAction;
import com.sack.rpgroll.dungeons.core.DungeonTrigger;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Un botón por {@link DungeonTrigger}, acotado a los eventos de una sola sala (mapa compartido con {@code RoomEditGUI}). */
public class RoomEventsEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int BACK_SLOT = 26;

    private final DungeonEditorSession session;
    private final Map<DungeonTrigger, List<DungeonAction>> events;
    private final Runnable onBack;
    private final List<DungeonTrigger> triggers;
    private final LangManager lang;

    public RoomEventsEditorGUI(Player player, DungeonEditorSession session,
            Map<DungeonTrigger, List<DungeonAction>> events, Runnable onBack) {
        super(player, ComponentUtils.parse(session.chatPromptManager.lang().raw("gui.editor.roomevents.title")),
                SIZE);
        this.session = session;
        this.events = events;
        this.onBack = onBack;
        this.triggers = List.of(DungeonTrigger.values());
        this.lang = session.chatPromptManager.lang();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < triggers.size(); i++) {

            DungeonTrigger trigger = triggers.get(i);
            int count = events.getOrDefault(trigger, List.of()).size();

            setItem(i, new ItemBuilder(count > 0 ? Material.COMPARATOR : Material.LEVER)
                    .setName(Component.text(trigger.name(), NamedTextColor.YELLOW))
                    .setLore(ComponentUtils.parse(lang.raw("gui.editor.roomevents.item.actions", "count", count)))
                    .build());
        }

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < triggers.size()) {

            DungeonTrigger trigger = triggers.get(slot);
            List<DungeonAction> actions = new ArrayList<>(events.getOrDefault(trigger, List.of()));

            new DungeonActionListEditorGUI(player, lang.raw("gui.editor.actionlist.title_for", "trigger", trigger.name()), actions,
                    session.chatPromptManager, () -> {
                        events.put(trigger, actions);
                        reopen();
                    }).open();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void reopen() {
        new RoomEventsEditorGUI(player, session, events, onBack).open();
    }

}
