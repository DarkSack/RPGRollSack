package com.sack.rpgroll.mobs.gui.editor;

import com.sack.rpgroll.common.lang.LangManager;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.mobs.core.MobAction;
import com.sack.rpgroll.mobs.core.MobTrigger;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

/** Un botón por {@link MobTrigger}, abre la lista de acciones directas para ese evento. */
public class MobTriggersEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int BACK_SLOT = 26;

    private final MobEditorSession session;
    private final Runnable onBack;
    private final List<MobTrigger> triggers;
    private final LangManager lang;

    public MobTriggersEditorGUI(Player player, MobEditorSession session, Runnable onBack) {
        super(player, session.chatPromptManager.lang().component("gui.triggers.title", "id",
                session.original.id()), SIZE);
        this.session = session;
        this.onBack = onBack;
        this.triggers = List.of(MobTrigger.values());
        this.lang = session.chatPromptManager.lang();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < triggers.size(); i++) {

            MobTrigger trigger = triggers.get(i);
            int count = session.triggers.getOrDefault(trigger, List.of()).size();

            setItem(i, new ItemBuilder(count > 0 ? Material.COMPARATOR : Material.LEVER)
                    .setName(Component.text(trigger.name(), NamedTextColor.YELLOW))
                    .setLore(lang.component("gui.triggers.count_lore", "count", count))
                    .build());
        }

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back_button")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < triggers.size()) {

            MobTrigger trigger = triggers.get(slot);
            List<MobAction> actions = new ArrayList<>(session.triggers.getOrDefault(trigger, List.of()));
            session.triggers.put(trigger, actions);

            new MobActionListEditorGUI(player, lang.raw("gui.triggers.actions_gui_title", "trigger", trigger.name()),
                    actions, session.chatPromptManager, this::reopen).open();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void reopen() {
        new MobTriggersEditorGUI(player, session, onBack).open();
    }

}
