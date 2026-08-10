package com.sack.rpgroll.tab.animation;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;

/**
 * Definición de una animación reusable (header/footer/scoreboard-title/
 * bossbar-title). {@code intervalTicks} es cada cuántos ticks avanza un frame.
 */
public record AnimationDefinition(String id, AnimationType type, int intervalTicks,
        List<String> frames) implements RPGContent {

    public String frameAt(long tickCounter) {

        if (frames.isEmpty()) {
            return "";
        }

        int frameCount = frames.size();
        int index = (int) ((tickCounter / Math.max(1, intervalTicks)) % frameCount);

        return frames.get(index);
    }

}
