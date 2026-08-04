package com.sack.rpgroll.chat.channel;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.Objects;

/**
 * Definición autoreada de un canal (plugins/RPGRoll-Chat/channels/*.yml).
 * Cubre spec: nombre/icono/color/prioridad/distancia/permisos/cooldown/
 * formato/sonidos/filtros. "Animaciones" se resuelve como
 * {@code alsoActionBar} — un anuncio de alta prioridad también se
 * muestra en la action bar, sin motor de animación de texto dedicado.
 */
public record ChatChannel(
        String id,
        String displayName,
        String icon,
        String color,
        int priority,
        ChannelScope scope,
        double distance,
        String viewPermission,
        String speakPermission,
        long cooldownMillis,
        String format,
        ChatTextFormat textFormat,
        String joinSound,
        boolean filterProfanity,
        boolean filterCaps,
        boolean allowUrls,
        boolean defaultJoined,
        boolean crossWorld,
        boolean alsoActionBar) implements RPGContent {

    public ChatChannel {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        icon = icon == null || icon.isBlank() ? "PAPER" : icon;
        color = color == null || color.isBlank() ? "WHITE" : color;
        format = format == null || format.isBlank() ? "&7[{channel}] &f{player}&7: &f{message}" : format;
        textFormat = textFormat == null ? ChatTextFormat.LEGACY : textFormat;
        scope = scope == null ? ChannelScope.GLOBAL : scope;
    }

    public boolean requiresViewPermission() {
        return viewPermission != null && !viewPermission.isBlank();
    }

    public boolean requiresSpeakPermission() {
        return speakPermission != null && !speakPermission.isBlank();
    }

}
