package com.sack.rpgroll.chat.gui;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.chat.emote.EmoteDefinition;
import com.sack.rpgroll.chat.emote.EmoteManager;
import com.sack.rpgroll.common.lang.LangManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

public class EmoteEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int TEMPLATE_SLOT = 10;
    private static final int TARGET_TEMPLATE_SLOT = 11;
    private static final int RADIUS_SLOT = 12;
    private static final int BACK_SLOT = 26;

    private final EmoteManager emoteManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private EmoteDefinition current;

    public EmoteEditorGUI(Player player, EmoteDefinition emote, EmoteManager emoteManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, chatPromptManager.lang().component("emote.editor_title", "id", emote.id()), SIZE);
        this.current = emote;
        this.emoteManager = emoteManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(EmoteDefinition updated) {
        current = updated;
        emoteManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        LangManager lang = chatPromptManager.lang();

        setItem(TEMPLATE_SLOT, new ItemBuilder(Material.WRITABLE_BOOK)
                .setName(lang.component("emote.label_message").colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(ComponentUtils.parse(current.template()),
                        lang.component("emote.lore_message_hint").colorIfAbsent(NamedTextColor.GRAY))
                .build());

        setItem(TARGET_TEMPLATE_SLOT, new ItemBuilder(Material.BOOK)
                .setName(lang.component("emote.label_target_message").colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(ComponentUtils.parse(
                        current.targetTemplate() == null || current.targetTemplate().isBlank()
                                ? lang.raw("emote.target_not_set") : current.targetTemplate()),
                        lang.component("emote.lore_target_hint").colorIfAbsent(NamedTextColor.GRAY))
                .build());

        setItem(RADIUS_SLOT, new ItemBuilder(Material.SPYGLASS)
                .setName(lang.component("emote.label_radius", "value",
                                current.radius() <= 0 ? lang.raw("emote.radius_world") : current.radius())
                        .colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(lang.component("emote.lore_radius").colorIfAbsent(NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        ClickType click = event.getClick();

        if (slot == TEMPLATE_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("emote.prompt_message"),
                    value -> replace(new EmoteDefinition(current.id(), value, current.targetTemplate(),
                            current.radius())));
            return;
        }

        if (slot == TARGET_TEMPLATE_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("emote.prompt_target_message"),
                    value -> replace(new EmoteDefinition(current.id(), current.template(), value, current.radius())));
            return;
        }

        if (slot == RADIUS_SLOT) {
            double delta = click == ClickType.RIGHT ? -10 : 10;
            replace(new EmoteDefinition(current.id(), current.template(), current.targetTemplate(),
                    Math.max(0, current.radius() + delta)));
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
