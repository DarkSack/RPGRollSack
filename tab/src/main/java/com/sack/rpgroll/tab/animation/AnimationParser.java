package com.sack.rpgroll.tab.animation;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;

public class AnimationParser implements ContentParser<AnimationDefinition> {

    @Override
    public AnimationDefinition parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        AnimationType type;
        String rawType = config.getString("type", "FRAME");
        try {
            type = AnimationType.valueOf(rawType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("animación '" + id + "': tipo inválido: " + rawType);
        }

        int intervalTicks = Math.max(1, config.getInt("interval", 10));
        List<String> frames = buildFrames(config, type);

        if (frames.isEmpty()) {
            throw new IllegalArgumentException("animación '" + id + "': no tiene frames");
        }

        return new AnimationDefinition(id, type, intervalTicks, frames);
    }

    private List<String> buildFrames(YamlConfiguration config, AnimationType type) {

        if (type == AnimationType.SCROLL) {
            return buildScrollFrames(config);
        }

        if (type == AnimationType.BLINK) {
            return buildBlinkFrames(config);
        }

        return config.getStringList("frames");
    }

    /** Precalcula la "ventana deslizante" sobre un texto largo como una lista de frames FRAME-style. */
    private List<String> buildScrollFrames(YamlConfiguration config) {

        String text = config.getString("text", "");
        int width = Math.max(1, config.getInt("width", 20));
        String separator = config.getString("separator", "   ");

        String looped = text + separator;

        if (looped.length() <= width) {
            return List.of(text);
        }

        java.util.List<String> frames = new java.util.ArrayList<>();
        String doubled = looped.repeat(2);

        for (int i = 0; i < looped.length(); i++) {
            frames.add(doubled.substring(i, i + width));
        }

        return frames;
    }

    private List<String> buildBlinkFrames(YamlConfiguration config) {
        String text = config.getString("text", "");
        return List.of(text, "");
    }

}
