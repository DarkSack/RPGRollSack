package com.sack.rpgroll.ascension.listener;

import com.sack.rpgroll.ascension.engine.AscensionEngine;
import com.sack.rpgroll.common.character.CharacterLevelUpEvent;
import com.sack.rpgroll.common.lang.LangManager;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Carga estado y reaplica bonos de atributo al entrar; guarda al salir. Da los puntos de talento de cada nivel. */
public class PlayerSessionListener implements Listener {

    private final AscensionEngine engine;
    private final LangManager lang;
    private final int talentPointsFromLevel;
    private final int talentPointsPerLevel;

    public PlayerSessionListener(AscensionEngine engine, LangManager lang, int talentPointsFromLevel,
            int talentPointsPerLevel) {
        this.engine = engine;
        this.lang = lang;
        this.talentPointsFromLevel = talentPointsFromLevel;
        this.talentPointsPerLevel = talentPointsPerLevel;
    }

    /** Sin esto los puntos solo salían de un par de logros y ningún árbol de talentos se podía completar. */
    @EventHandler
    public void onLevelUp(CharacterLevelUpEvent event) {
        int levels = event.getNewLevel() - Math.max(event.getPreviousLevel(), talentPointsFromLevel - 1);
        int points = levels * talentPointsPerLevel;
        if (points <= 0) {
            return;
        }
        engine.getStateManager().getOrLoad(event.getPlayer()).addTalentPoints(points);
        lang.send(event.getPlayer(), "progress.reward_talent_points", "amount", points);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        engine.getStateManager().getOrLoad(event.getPlayer());
        engine.reapplyBonuses(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        engine.getStateManager().unload(event.getPlayer().getUniqueId());
    }

}
