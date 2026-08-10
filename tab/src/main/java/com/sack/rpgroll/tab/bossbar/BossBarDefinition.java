package com.sack.rpgroll.tab.bossbar;

import com.sack.rpgroll.common.content.RPGContent;

/** {@code progress} debe resolver a un número 0-100. {@code color}/{@code style} son nombres de {@code BossBar.Color}/{@code BossBar.Overlay}. */
public record BossBarDefinition(
        String id,
        String title,
        String progressPlaceholder,
        String color,
        String style,
        int priority) implements RPGContent {
}
