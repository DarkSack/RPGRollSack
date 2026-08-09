package com.sack.rpgroll.workers.core.ai;

/** Snapshot del mundo en el momento de decidir qué hace un worker — evita que AiEngine dependa de Bukkit directo. */
public record WorldContext(boolean raining, boolean night) {
}
