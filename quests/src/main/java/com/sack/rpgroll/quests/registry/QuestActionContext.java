package com.sack.rpgroll.quests.registry;

import com.sack.rpgroll.quests.core.Quest;
import com.sack.rpgroll.quests.core.QuestStage;

import org.bukkit.entity.Player;

/**
 * Contexto disponible al ejecutar una {@link com.sack.rpgroll.quests.core.QuestAction}.
 * {@code stage} es nulo cuando la acción no ocurre dentro de un stage
 * concreto (ej. eventos a nivel de quest completa).
 */
public record QuestActionContext(Player player, Quest quest, QuestStage stage) {
}
