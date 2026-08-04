package com.sack.rpgroll.chat.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.chat.channel.ChannelManager;
import com.sack.rpgroll.chat.channel.ChannelScope;
import com.sack.rpgroll.chat.channel.ChatChannel;
import com.sack.rpgroll.chat.channel.ChatTextFormat;

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
        super(player, Component.text("Canal: " + channel.id(), NamedTextColor.GOLD), SIZE);
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

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(Component.text("Nombre: " + current.displayName(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir uno nuevo", NamedTextColor.GRAY))
                .build());

        setItem(ICON_SLOT, new ItemBuilder(resolveIcon())
                .setName(Component.text("Ícono: " + current.icon(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir un Material nuevo", NamedTextColor.GRAY))
                .build());

        setItem(COLOR_SLOT, new ItemBuilder(Material.WHITE_DYE)
                .setName(Component.text("Color: " + current.color(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir un color nuevo", NamedTextColor.GRAY))
                .build());

        setItem(PRIORITY_SLOT, new ItemBuilder(Material.COMPARATOR)
                .setName(Component.text("Prioridad: " + current.priority(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +1 · Click derecho: -1", NamedTextColor.GRAY))
                .build());

        setItem(SCOPE_SLOT, new ItemBuilder(Material.COMPASS)
                .setName(Component.text("Alcance: " + current.scope(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para pasar al siguiente", NamedTextColor.GRAY))
                .build());

        setItem(DISTANCE_SLOT, new ItemBuilder(Material.SPYGLASS)
                .setName(Component.text("Distancia (proximidad): " + current.distance(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +10 · Click derecho: -10", NamedTextColor.GRAY))
                .build());

        setItem(VIEW_PERM_SLOT, new ItemBuilder(Material.PAPER)
                .setName(Component.text("Permiso para ver: "
                        + (current.requiresViewPermission() ? current.viewPermission() : "(ninguno)"),
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir uno nuevo (vacío = sin permiso)", NamedTextColor.GRAY))
                .build());

        setItem(SPEAK_PERM_SLOT, new ItemBuilder(Material.PAPER)
                .setName(Component.text("Permiso para hablar: "
                        + (current.requiresSpeakPermission() ? current.speakPermission() : "(ninguno)"),
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir uno nuevo (vacío = sin permiso)", NamedTextColor.GRAY))
                .build());

        setItem(COOLDOWN_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(Component.text("Cooldown: " + current.cooldownMillis() + "ms", NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +1000ms · Click derecho: -1000ms", NamedTextColor.GRAY))
                .build());

        setItem(FORMAT_SLOT, new ItemBuilder(Material.WRITABLE_BOOK)
                .setName(Component.text("Formato", NamedTextColor.YELLOW))
                .setLore(LegacyComponentSerializer.legacyAmpersand().deserialize(current.format()),
                        Component.text("Click para escribir uno nuevo", NamedTextColor.GRAY))
                .build());

        setItem(TEXT_FORMAT_SLOT, new ItemBuilder(Material.BOOK)
                .setName(Component.text("Tipo de texto: " + current.textFormat(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para alternar Legacy/MiniMessage", NamedTextColor.GRAY))
                .build());

        setItem(SOUND_SLOT, new ItemBuilder(Material.NOTE_BLOCK)
                .setName(Component.text("Sonido: " + (current.joinSound() == null || current.joinSound().isBlank()
                        ? "(ninguno)" : current.joinSound()), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir un Sound nuevo", NamedTextColor.GRAY))
                .build());

        setItem(FILTER_PROFANITY_SLOT, toggleItem("Censura de palabras", current.filterProfanity()));
        setItem(FILTER_CAPS_SLOT, toggleItem("Filtro de mayúsculas", current.filterCaps()));
        setItem(ALLOW_URLS_SLOT, toggleItem("Permitir URLs", current.allowUrls()));
        setItem(DEFAULT_JOINED_SLOT, toggleItem("Unido por defecto", current.defaultJoined()));
        setItem(CROSS_WORLD_SLOT, toggleItem("Multi-mundo", current.crossWorld()));
        setItem(ACTION_BAR_SLOT, toggleItem("También en action bar", current.alsoActionBar()));

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    private org.bukkit.inventory.ItemStack toggleItem(String label, boolean enabled) {
        return new ItemBuilder(enabled ? Material.LIME_DYE : Material.GRAY_DYE)
                .setName(Component.text(label + ": " + (enabled ? "sí" : "no"),
                        enabled ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .setLore(Component.text("Click para alternar", NamedTextColor.GRAY))
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

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo nombre del canal:",
                    value -> replace(withDisplayName(value.trim())));
            return;
        }

        if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nombre del Material (ej. PAPER):",
                    value -> replace(withIcon(value.trim().toUpperCase(Locale.ROOT))));
            return;
        }

        if (slot == COLOR_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nombre del color (ej. GOLD):",
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
            chatPromptManager.prompt(player, "Escribí el permiso para VER el canal (vacío = ninguno):",
                    value -> replace(withViewPermission(value.trim())));
            return;
        }

        if (slot == SPEAK_PERM_SLOT) {
            chatPromptManager.prompt(player, "Escribí el permiso para HABLAR en el canal (vacío = ninguno):",
                    value -> replace(withSpeakPermission(value.trim())));
            return;
        }

        if (slot == COOLDOWN_SLOT) {
            long delta = click == ClickType.RIGHT ? -1000 : 1000;
            replace(withCooldown(Math.max(0, current.cooldownMillis() + delta)));
            return;
        }

        if (slot == FORMAT_SLOT) {
            chatPromptManager.prompt(player,
                    "Escribí el nuevo formato (tokens: {player} {message} {channel} {world} {role_prefix} {role_suffix} {context_prefix}):",
                    value -> replace(withFormat(value)));
            return;
        }

        if (slot == TEXT_FORMAT_SLOT) {
            replace(withTextFormat(current.textFormat() == ChatTextFormat.LEGACY ? ChatTextFormat.MINIMESSAGE
                    : ChatTextFormat.LEGACY));
            return;
        }

        if (slot == SOUND_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nombre del Sound (vacío = ninguno):",
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
