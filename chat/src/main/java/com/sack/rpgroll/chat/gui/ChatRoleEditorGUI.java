package com.sack.rpgroll.chat.gui;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.chat.role.ChatRole;
import com.sack.rpgroll.chat.role.ChatRoleManager;
import com.sack.rpgroll.common.lang.LangManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ChatRoleEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int PREFIX_SLOT = 9;
    private static final int SUFFIX_SLOT = 10;
    private static final int COLOR_SLOT = 11;
    private static final int ICON_SLOT = 12;
    private static final int PRIORITY_SLOT = 13;
    private static final int PERMISSION_SLOT = 14;
    private static final int BACK_SLOT = 26;

    private final ChatRoleManager roleManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private ChatRole current;

    public ChatRoleEditorGUI(Player player, ChatRole role, ChatRoleManager roleManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, chatPromptManager.lang().component("role.editor_title", "id", role.id()), SIZE);
        this.current = role;
        this.roleManager = roleManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(ChatRole updated) {
        current = updated;
        roleManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        LangManager lang = chatPromptManager.lang();

        setItem(PREFIX_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(ComponentUtils.parse(lang.raw("role.label_prefix", "value", current.prefix()))
                        .colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(lang.component("gui.click_new").colorIfAbsent(NamedTextColor.GRAY))
                .build());

        setItem(SUFFIX_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(ComponentUtils.parse(lang.raw("role.label_suffix", "value", current.suffix()))
                        .colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(lang.component("gui.click_new").colorIfAbsent(NamedTextColor.GRAY))
                .build());

        setItem(COLOR_SLOT, new ItemBuilder(Material.WHITE_DYE)
                .setName(lang.component("role.label_color", "value", current.color())
                        .colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(lang.component("gui.click_new_color").colorIfAbsent(NamedTextColor.GRAY))
                .build());

        setItem(ICON_SLOT, new ItemBuilder(resolveIcon())
                .setName(lang.component("role.label_icon", "value", current.icon())
                        .colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(lang.component("gui.click_new_material").colorIfAbsent(NamedTextColor.GRAY))
                .build());

        setItem(PRIORITY_SLOT, new ItemBuilder(Material.COMPARATOR)
                .setName(lang.component("role.label_priority", "value", current.priority())
                        .colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(lang.component("gui.click_priority").colorIfAbsent(NamedTextColor.GRAY))
                .build());

        setItem(PERMISSION_SLOT, new ItemBuilder(Material.PAPER)
                .setName(lang.component("role.label_permission", "value",
                        current.permission() == null || current.permission().isBlank()
                                ? lang.raw("gui.none") : current.permission())
                        .colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(lang.component("role.lore_permission_hint").colorIfAbsent(NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.back")));
    }

    private Material resolveIcon() {
        try {
            return Material.valueOf(current.icon().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Material.PAPER;
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        ClickType click = event.getClick();

        if (slot == PREFIX_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("role.prompt_prefix"),
                    value -> replace(new ChatRole(current.id(), value, current.suffix(), current.color(),
                            current.icon(), current.priority(), current.permission())));
            return;
        }

        if (slot == SUFFIX_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("role.prompt_suffix"),
                    value -> replace(new ChatRole(current.id(), current.prefix(), value, current.color(),
                            current.icon(), current.priority(), current.permission())));
            return;
        }

        if (slot == COLOR_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("role.prompt_color"),
                    value -> replace(new ChatRole(current.id(), current.prefix(), current.suffix(),
                            value.trim().toUpperCase(java.util.Locale.ROOT), current.icon(), current.priority(),
                            current.permission())));
            return;
        }

        if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("role.prompt_icon"),
                    value -> replace(new ChatRole(current.id(), current.prefix(), current.suffix(), current.color(),
                            value.trim().toUpperCase(java.util.Locale.ROOT), current.priority(),
                            current.permission())));
            return;
        }

        if (slot == PRIORITY_SLOT) {
            int delta = click == ClickType.RIGHT ? -1 : 1;
            replace(new ChatRole(current.id(), current.prefix(), current.suffix(), current.color(), current.icon(),
                    current.priority() + delta, current.permission()));
            return;
        }

        if (slot == PERMISSION_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("role.prompt_permission"),
                    value -> replace(new ChatRole(current.id(), current.prefix(), current.suffix(), current.color(),
                            current.icon(), current.priority(), value.trim())));
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
