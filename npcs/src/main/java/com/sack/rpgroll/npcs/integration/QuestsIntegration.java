package com.sack.rpgroll.npcs.integration;

import com.sack.rpgroll.quests.api.NpcTalkEvent;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Avisa a RPGRoll-Quests de que un jugador habló con un NPC, para que avancen
 * los objetivos TALK_TO_NPC y DELIVER_ITEM. Hasta ahora nadie disparaba el
 * evento, así que esos objetivos no se podían completar.
 * <p>
 * Esta es la única clase que toca RPGRoll-Quests: llamarla solo tras
 * comprobar {@link #isAvailable()}, o fallaría al cargar la clase del evento.
 */
public final class QuestsIntegration {

    private QuestsIntegration() {
    }

    public static boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("RPGRoll-Quests");
    }

    public static void fireTalk(Player player, String npcId) {
        Bukkit.getPluginManager().callEvent(new NpcTalkEvent(player, npcId));
    }

}
