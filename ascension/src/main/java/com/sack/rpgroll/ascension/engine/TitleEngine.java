package com.sack.rpgroll.ascension.engine;

import com.sack.rpgroll.ascension.deferred.Title;
import com.sack.rpgroll.ascension.deferred.TitleManager;
import com.sack.rpgroll.ascension.player.AscensionPlayerState;
import com.sack.rpgroll.ascension.player.AscensionPlayerStateManager;
import com.sack.rpgroll.ascension.requirement.AscensionRequirementChecker;
import com.sack.rpgroll.common.lang.LangManager;

import org.bukkit.entity.Player;

/**
 * Desbloqueo de títulos: por recompensa (logros, rangos de facción), a mano,
 * o solo, al cumplir los {@code requirements} del título. El título activo
 * se ve con el placeholder {@code %rpgrollascension_title%}.
 */
public class TitleEngine {

    private final TitleManager titleManager;
    private final AscensionPlayerStateManager stateManager;
    private final AscensionRequirementChecker requirementChecker;
    private final LangManager lang;

    public TitleEngine(TitleManager titleManager, AscensionPlayerStateManager stateManager,
            AscensionRequirementChecker requirementChecker, LangManager lang) {
        this.titleManager = titleManager;
        this.stateManager = stateManager;
        this.requirementChecker = requirementChecker;
        this.lang = lang;
    }

    /** @return true si el jugador no lo tenía */
    public boolean unlock(Player player, String titleId) {

        AscensionPlayerState state = stateManager.getOrLoad(player);

        if (!state.unlockTitle(titleId)) {
            return false;
        }

        String name = titleManager.get(titleId).map(Title::displayName).orElse(titleId);
        lang.send(player, "progress.title_unlocked", "name", name, "id", titleId);
        return true;
    }

    /** Desbloquea los títulos automáticos cuyos requisitos ya se cumplen. */
    public void checkRequirements(Player player) {

        AscensionPlayerState state = stateManager.getOrLoad(player);

        for (Title title : titleManager.getAll()) {
            if (title.unlocksAutomatically() && !state.getUnlockedTitles().contains(title.id())
                    && requirementChecker.check(player, title.requirements(), state).isEmpty()) {
                unlock(player, title.id());
            }
        }
    }

}
