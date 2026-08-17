package com.sack.rpgroll.chat.gui;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.chat.channel.ChannelManager;
import com.sack.rpgroll.chat.channel.ChannelScope;
import com.sack.rpgroll.chat.channel.ChatChannel;
import com.sack.rpgroll.chat.channel.ChatTextFormat;
import com.sack.rpgroll.common.lang.LangManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Locale;

/** Editor de un canal — cubre spec: nombre/icono/color/prioridad/distancia/permisos/cooldown/formato/sonidos/filtros. */
public class ChannelEditorGUI extends InventoryGUI {

    private static final int SIZE = 54;

    private static final int NAME_SLOT = 10;
    private static final int ICON_SLOT = 11;
    private static final int COLOR_SLOT = 12;
    private static final int PRIORITY_SLOT = 13;
    private static final int SCOPE_SLOT = 14;
    private static final int DISTANCE_SLOT = 15;
    private static final int VIEW_PERM_SLOT = 16;

    private static final int SPEAK_PERM_SLOT = 19;
    private static final int COOLDOWN_SLOT = 20;
    private static final int FORMAT_SLOT = 21;
    private static final int TEXT_FORMAT_SLOT = 22;
    private static final int SOUND_SLOT = 23;

    private static final int FILTER_PROFANITY_SLOT = 28;
    private static final int FILTER_CAPS_SLOT = 29;
    private static final int ALLOW_URLS_SLOT = 30;
    private static final int DEFAULT_JOINED_SLOT = 31;
    private static final int CROSS_WORLD_SLOT = 32;
    private static final int ACTION_BAR_SLOT = 33;

    private static final int BACK_SLOT = 49;

    private final ChannelManager channelManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private ChatChannel current;

    public ChannelEditorGUI(Player player, ChatChannel channel, ChannelManager channelManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, chatPromptManager.lang().component("channel.editor_title", "id", channel.id()), SIZE);
        this.current = channel;
        this.channelManager = channelManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(ChatChannel updated) {
        current = updated;
        channelManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        LangManager lang = chatPromptManager.lang();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(lang.component("channel.label_name", "value", current.displayName())
                        .colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(lang.component("gui.click_new").colorIfAbsent(NamedTextColor.GRAY))
                .build());

        setItem(ICON_SLOT, new ItemBuilder(resolveIcon())
                .setName(lang.component("channel.label_icon", "value", current.icon())
                        .colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(lang.component("gui.click_new_material").colorIfAbsent(NamedTextColor.GRAY))
                .build());

        setItem(COLOR_SLOT, new ItemBuilder(Material.WHITE_DYE)
                .setName(lang.component("channel.label_color", "value", current.color())
                        .colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(lang.component("gui.click_new_color").colorIfAbsent(NamedTextColor.GRAY))
                .build());

        setItem(PRIORITY_SLOT, new ItemBuilder(Material.COMPARATOR)
                .setName(lang.component("channel.label_priority", "value", current.priority())
                        .colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(lang.component("gui.click_priority").colorIfAbsent(NamedTextColor.GRAY))
                .build());

        setItem(SCOPE_SLOT, new ItemBuilder(Material.COMPASS)
                .setName(lang.component("channel.label_scope", "value", current.scope())
                        .colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(lang.component("channel.lore_scope_next").colorIfAbsent(NamedTextColor.GRAY))
                .build());

        setItem(DISTANCE_SLOT, new ItemBuilder(Material.SPYGLASS)
                .setName(lang.component("channel.label_distance", "value", current.distance())
                        .colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(lang.component("channel.lore_distance").colorIfAbsent(NamedTextColor.GRAY))
                .build());

        setItem(VIEW_PERM_SLOT, new ItemBuilder(Material.PAPER)
                .setName(lang.component("channel.label_view_permission", "value",
                        current.requiresViewPermission() ? current.viewPermission() : lang.raw("gui.none"))
                        .colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(lang.component("channel.lore_permission_hint").colorIfAbsent(NamedTextColor.GRAY))
                .build());

        setItem(SPEAK_PERM_SLOT, new ItemBuilder(Material.PAPER)
                .setName(lang.component("channel.label_speak_permission", "value",
                        current.requiresSpeakPermission() ? current.speakPermission() : lang.raw("gui.none"))
                        .colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(lang.component("channel.lore_permission_hint").colorIfAbsent(NamedTextColor.GRAY))
                .build());

        setItem(COOLDOWN_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(lang.component("channel.label_cooldown", "value", current.cooldownMillis())
                        .colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(lang.component("channel.lore_cooldown").colorIfAbsent(NamedTextColor.GRAY))
                .build());

        setItem(FORMAT_SLOT, new ItemBuilder(Material.WRITABLE_BOOK)
                .setName(lang.component("channel.label_format").colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(ComponentUtils.parse(current.format()),
                        lang.component("gui.click_new").colorIfAbsent(NamedTextColor.GRAY))
                .build());

        setItem(TEXT_FORMAT_SLOT, new ItemBuilder(Material.BOOK)
                .setName(lang.component("channel.label_text_format", "value", current.textFormat())
                        .colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(lang.component("channel.lore_text_format_toggle").colorIfAbsent(NamedTextColor.GRAY))
                .build());

        setItem(SOUND_SLOT, new ItemBuilder(Material.NOTE_BLOCK)
                .setName(lang.component("channel.label_sound", "value",
                        current.joinSound() == null || current.joinSound().isBlank() ? lang.raw("gui.none")
                                : current.joinSound())
                        .colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(lang.component("channel.lore_sound_new").colorIfAbsent(NamedTextColor.GRAY))
                .build());

        setItem(FILTER_PROFANITY_SLOT, toggleItem(lang, lang.raw("channel.toggle_profanity"), current.filterProfanity()));
        setItem(FILTER_CAPS_SLOT, toggleItem(lang, lang.raw("channel.toggle_caps"), current.filterCaps()));
        setItem(ALLOW_URLS_SLOT, toggleItem(lang, lang.raw("channel.toggle_urls"), current.allowUrls()));
        setItem(DEFAULT_JOINED_SLOT, toggleItem(lang, lang.raw("channel.toggle_default_joined"), current.defaultJoined()));
        setItem(CROSS_WORLD_SLOT, toggleItem(lang, lang.raw("channel.toggle_cross_world"), current.crossWorld()));
        setItem(ACTION_BAR_SLOT, toggleItem(lang, lang.raw("channel.toggle_action_bar"), current.alsoActionBar()));

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.back")));
    }

    private org.bukkit.inventory.ItemStack toggleItem(LangManager lang, String label, boolean enabled) {
        return new ItemBuilder(enabled ? Material.LIME_DYE : Material.GRAY_DYE)
                .setName(lang.component("channel.toggle_name", "label", label,
                        "value", lang.raw(enabled ? "channel.toggle_yes" : "channel.toggle_no"))
                        .colorIfAbsent(enabled ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .setLore(lang.component("channel.toggle_lore").colorIfAbsent(NamedTextColor.GRAY))
                .build();
    }

    private Material resolveIcon() {
        try {
            return Material.valueOf(current.icon().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Material.PAPER;
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        ClickType click = event.getClick();
        LangManager lang = chatPromptManager.lang();

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, lang.raw("channel.prompt_name"),
                    value -> replace(withDisplayName(value.trim())));
            return;
        }

        if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, lang.raw("channel.prompt_icon"),
                    value -> replace(withIcon(value.trim().toUpperCase(Locale.ROOT))));
            return;
        }

        if (slot == COLOR_SLOT) {
            chatPromptManager.prompt(player, lang.raw("channel.prompt_color"),
                    value -> replace(withColor(value.trim().toUpperCase(Locale.ROOT))));
            return;
        }

        if (slot == PRIORITY_SLOT) {
            replace(withPriority(current.priority() + (click == ClickType.RIGHT ? -1 : 1)));
            return;
        }

        if (slot == SCOPE_SLOT) {
            ChannelScope[] values = ChannelScope.values();
            replace(withScope(values[(current.scope().ordinal() + 1) % values.length]));
            return;
        }

        if (slot == DISTANCE_SLOT) {
            replace(withDistance(Math.max(0, current.distance() + (click == ClickType.RIGHT ? -10 : 10))));
            return;
        }

        if (slot == VIEW_PERM_SLOT) {
            chatPromptManager.prompt(player, lang.raw("channel.prompt_view_permission"),
                    value -> replace(withViewPermission(value.trim())));
            return;
        }

        if (slot == SPEAK_PERM_SLOT) {
            chatPromptManager.prompt(player, lang.raw("channel.prompt_speak_permission"),
                    value -> replace(withSpeakPermission(value.trim())));
            return;
        }

        if (slot == COOLDOWN_SLOT) {
            long delta = click == ClickType.RIGHT ? -1000 : 1000;
            replace(withCooldown(Math.max(0, current.cooldownMillis() + delta)));
            return;
        }

        if (slot == FORMAT_SLOT) {
            chatPromptManager.prompt(player, lang.raw("channel.prompt_format"),
                    value -> replace(withFormat(value)));
            return;
        }

        if (slot == TEXT_FORMAT_SLOT) {
            replace(withTextFormat(current.textFormat() == ChatTextFormat.LEGACY ? ChatTextFormat.MINIMESSAGE
                    : ChatTextFormat.LEGACY));
            return;
        }

        if (slot == SOUND_SLOT) {
            chatPromptManager.prompt(player, lang.raw("channel.prompt_sound"),
                    value -> replace(withJoinSound(value.trim())));
            return;
        }

        if (slot == FILTER_PROFANITY_SLOT) {
            replace(withFilterProfanity(!current.filterProfanity()));
            return;
        }

        if (slot == FILTER_CAPS_SLOT) {
            replace(withFilterCaps(!current.filterCaps()));
            return;
        }

        if (slot == ALLOW_URLS_SLOT) {
            replace(withAllowUrls(!current.allowUrls()));
            return;
        }

        if (slot == DEFAULT_JOINED_SLOT) {
            replace(withDefaultJoined(!current.defaultJoined()));
            return;
        }

        if (slot == CROSS_WORLD_SLOT) {
            replace(withCrossWorld(!current.crossWorld()));
            return;
        }

        if (slot == ACTION_BAR_SLOT) {
            replace(withAlsoActionBar(!current.alsoActionBar()));
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    // ============ Helpers de copia inmutable ============

    private ChatChannel withDisplayName(String v) {
        return new ChatChannel(current.id(), v, current.icon(), current.color(), current.priority(), current.scope(),
                current.distance(), current.viewPermission(), current.speakPermission(), current.cooldownMillis(),
                current.format(), current.textFormat(), current.joinSound(), current.filterProfanity(),
                current.filterCaps(), current.allowUrls(), current.defaultJoined(), current.crossWorld(),
                current.alsoActionBar());
    }

    private ChatChannel withIcon(String v) {
        return new ChatChannel(current.id(), current.displayName(), v, current.color(), current.priority(),
                current.scope(), current.distance(), current.viewPermission(), current.speakPermission(),
                current.cooldownMillis(), current.format(), current.textFormat(), current.joinSound(),
                current.filterProfanity(), current.filterCaps(), current.allowUrls(), current.defaultJoined(),
                current.crossWorld(), current.alsoActionBar());
    }

    private ChatChannel withColor(String v) {
        return new ChatChannel(current.id(), current.displayName(), current.icon(), v, current.priority(),
                current.scope(), current.distance(), current.viewPermission(), current.speakPermission(),
                current.cooldownMillis(), current.format(), current.textFormat(), current.joinSound(),
                current.filterProfanity(), current.filterCaps(), current.allowUrls(), current.defaultJoined(),
                current.crossWorld(), current.alsoActionBar());
    }

    private ChatChannel withPriority(int v) {
        return new ChatChannel(current.id(), current.displayName(), current.icon(), current.color(), v,
                current.scope(), current.distance(), current.viewPermission(), current.speakPermission(),
                current.cooldownMillis(), current.format(), current.textFormat(), current.joinSound(),
                current.filterProfanity(), current.filterCaps(), current.allowUrls(), current.defaultJoined(),
                current.crossWorld(), current.alsoActionBar());
    }

    private ChatChannel withScope(ChannelScope v) {
        return new ChatChannel(current.id(), current.displayName(), current.icon(), current.color(),
                current.priority(), v, current.distance(), current.viewPermission(), current.speakPermission(),
                current.cooldownMillis(), current.format(), current.textFormat(), current.joinSound(),
                current.filterProfanity(), current.filterCaps(), current.allowUrls(), current.defaultJoined(),
                current.crossWorld(), current.alsoActionBar());
    }

    private ChatChannel withDistance(double v) {
        return new ChatChannel(current.id(), current.displayName(), current.icon(), current.color(),
                current.priority(), current.scope(), v, current.viewPermission(), current.speakPermission(),
                current.cooldownMillis(), current.format(), current.textFormat(), current.joinSound(),
                current.filterProfanity(), current.filterCaps(), current.allowUrls(), current.defaultJoined(),
                current.crossWorld(), current.alsoActionBar());
    }

    private ChatChannel withViewPermission(String v) {
        return new ChatChannel(current.id(), current.displayName(), current.icon(), current.color(),
                current.priority(), current.scope(), current.distance(), v, current.speakPermission(),
                current.cooldownMillis(), current.format(), current.textFormat(), current.joinSound(),
                current.filterProfanity(), current.filterCaps(), current.allowUrls(), current.defaultJoined(),
                current.crossWorld(), current.alsoActionBar());
    }

    private ChatChannel withSpeakPermission(String v) {
        return new ChatChannel(current.id(), current.displayName(), current.icon(), current.color(),
                current.priority(), current.scope(), current.distance(), current.viewPermission(), v,
                current.cooldownMillis(), current.format(), current.textFormat(), current.joinSound(),
                current.filterProfanity(), current.filterCaps(), current.allowUrls(), current.defaultJoined(),
                current.crossWorld(), current.alsoActionBar());
    }

    private ChatChannel withCooldown(long v) {
        return new ChatChannel(current.id(), current.displayName(), current.icon(), current.color(),
                current.priority(), current.scope(), current.distance(), current.viewPermission(),
                current.speakPermission(), v, current.format(), current.textFormat(), current.joinSound(),
                current.filterProfanity(), current.filterCaps(), current.allowUrls(), current.defaultJoined(),
                current.crossWorld(), current.alsoActionBar());
    }

    private ChatChannel withFormat(String v) {
        return new ChatChannel(current.id(), current.displayName(), current.icon(), current.color(),
                current.priority(), current.scope(), current.distance(), current.viewPermission(),
                current.speakPermission(), current.cooldownMillis(), v, current.textFormat(), current.joinSound(),
                current.filterProfanity(), current.filterCaps(), current.allowUrls(), current.defaultJoined(),
                current.crossWorld(), current.alsoActionBar());
    }

    private ChatChannel withTextFormat(ChatTextFormat v) {
        return new ChatChannel(current.id(), current.displayName(), current.icon(), current.color(),
                current.priority(), current.scope(), current.distance(), current.viewPermission(),
                current.speakPermission(), current.cooldownMillis(), current.format(), v, current.joinSound(),
                current.filterProfanity(), current.filterCaps(), current.allowUrls(), current.defaultJoined(),
                current.crossWorld(), current.alsoActionBar());
    }

    private ChatChannel withJoinSound(String v) {
        return new ChatChannel(current.id(), current.displayName(), current.icon(), current.color(),
                current.priority(), current.scope(), current.distance(), current.viewPermission(),
                current.speakPermission(), current.cooldownMillis(), current.format(), current.textFormat(), v,
                current.filterProfanity(), current.filterCaps(), current.allowUrls(), current.defaultJoined(),
                current.crossWorld(), current.alsoActionBar());
    }

    private ChatChannel withFilterProfanity(boolean v) {
        return new ChatChannel(current.id(), current.displayName(), current.icon(), current.color(),
                current.priority(), current.scope(), current.distance(), current.viewPermission(),
                current.speakPermission(), current.cooldownMillis(), current.format(), current.textFormat(),
                current.joinSound(), v, current.filterCaps(), current.allowUrls(), current.defaultJoined(),
                current.crossWorld(), current.alsoActionBar());
    }

    private ChatChannel withFilterCaps(boolean v) {
        return new ChatChannel(current.id(), current.displayName(), current.icon(), current.color(),
                current.priority(), current.scope(), current.distance(), current.viewPermission(),
                current.speakPermission(), current.cooldownMillis(), current.format(), current.textFormat(),
                current.joinSound(), current.filterProfanity(), v, current.allowUrls(), current.defaultJoined(),
                current.crossWorld(), current.alsoActionBar());
    }

    private ChatChannel withAllowUrls(boolean v) {
        return new ChatChannel(current.id(), current.displayName(), current.icon(), current.color(),
                current.priority(), current.scope(), current.distance(), current.viewPermission(),
                current.speakPermission(), current.cooldownMillis(), current.format(), current.textFormat(),
                current.joinSound(), current.filterProfanity(), current.filterCaps(), v, current.defaultJoined(),
                current.crossWorld(), current.alsoActionBar());
    }

    private ChatChannel withDefaultJoined(boolean v) {
        return new ChatChannel(current.id(), current.displayName(), current.icon(), current.color(),
                current.priority(), current.scope(), current.distance(), current.viewPermission(),
                current.speakPermission(), current.cooldownMillis(), current.format(), current.textFormat(),
                current.joinSound(), current.filterProfanity(), current.filterCaps(), current.allowUrls(), v,
                current.crossWorld(), current.alsoActionBar());
    }

    private ChatChannel withCrossWorld(boolean v) {
        return new ChatChannel(current.id(), current.displayName(), current.icon(), current.color(),
                current.priority(), current.scope(), current.distance(), current.viewPermission(),
                current.speakPermission(), current.cooldownMillis(), current.format(), current.textFormat(),
                current.joinSound(), current.filterProfanity(), current.filterCaps(), current.allowUrls(),
                current.defaultJoined(), v, current.alsoActionBar());
    }

    private ChatChannel withAlsoActionBar(boolean v) {
        return new ChatChannel(current.id(), current.displayName(), current.icon(), current.color(),
                current.priority(), current.scope(), current.distance(), current.viewPermission(),
                current.speakPermission(), current.cooldownMillis(), current.format(), current.textFormat(),
                current.joinSound(), current.filterProfanity(), current.filterCaps(), current.allowUrls(),
                current.defaultJoined(), current.crossWorld(), v);
    }

}
