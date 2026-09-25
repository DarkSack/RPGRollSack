package com.sack.rpgroll.ascension.engine;

import com.sack.rpgroll.ascension.deferred.Faction;
import com.sack.rpgroll.ascension.deferred.FactionManager;
import com.sack.rpgroll.ascension.deferred.FactionRank;
import com.sack.rpgroll.ascension.player.AscensionPlayerState;
import com.sack.rpgroll.ascension.player.AscensionPlayerStateManager;
import com.sack.rpgroll.ascension.progress.Criterion;
import com.sack.rpgroll.ascension.progress.ProgressEvent;
import com.sack.rpgroll.common.lang.LangManager;

import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Reputación con facciones: la ganan (o la pierden) las {@code sources} de
 * cada facción, las recompensas y los admins. Al entrar en un rango se
 * entregan sus recompensas, una sola vez por jugador y rango. Ganar con una
 * facción resta con sus rivales.
 */
public class FactionEngine {

    private final FactionManager factionManager;
    private final AscensionPlayerStateManager stateManager;
    private final RewardService rewardService;
    private final LangManager lang;

    public FactionEngine(FactionManager factionManager, AscensionPlayerStateManager stateManager,
            RewardService rewardService, LangManager lang) {
        this.factionManager = factionManager;
        this.stateManager = stateManager;
        this.rewardService = rewardService;
        this.lang = lang;
    }

    public FactionManager getFactionManager() {
        return factionManager;
    }

    public void onEvent(Player player, ProgressEvent event) {

        for (Faction faction : factionManager.getAll()) {

            int gained = 0;
            for (Criterion source : faction.sources()) {
                if (source.matches(event)) {
                    gained += source.amount();
                }
            }

            if (gained != 0) {
                addReputation(player, faction.id(), gained);
            }
        }
    }

    /**
     * Suma (o resta) reputación, respetando los límites de la facción, resta
     * a sus rivales y entrega los rangos alcanzados. Una facción que no
     * existe se guarda igual, sin rangos, para no perder lo que diera un
     * admin o una recompensa mal escrita.
     */
    public void addReputation(Player player, String factionId, int amount) {
        change(player, factionId, amount, true);
    }

    private void change(Player player, String factionId, int amount, boolean applyRivals) {

        AscensionPlayerState state = stateManager.getOrLoad(player);
        Optional<Faction> factionOpt = factionManager.get(factionId);

        if (factionOpt.isEmpty()) {
            state.addReputation(factionId, amount);
            return;
        }

        Faction faction = factionOpt.get();
        int before = state.getReputation(faction.id());
        int after = faction.clamp(before + amount);

        if (after == before) {
            return;
        }

        state.setReputation(faction.id(), after);

        for (FactionRank rank : ReputationMath.ranksEntered(faction, before, after)) {
            enterRank(player, state, faction, rank, after > before);
        }

        // La pérdida de las rivales no arrastra a sus propias rivales:
        // si no, dos facciones enfrentadas se restarían en bucle.
        if (applyRivals) {
            int loss = ReputationMath.rivalLoss(faction, after - before);
            if (loss > 0) {
                for (String rival : faction.rivals()) {
                    change(player, rival, -loss, false);
                }
            }
        }
    }

    private void enterRank(Player player, AscensionPlayerState state, Faction faction, FactionRank rank,
            boolean rising) {

        lang.send(player, rising ? "progress.rank_up" : "progress.rank_down",
                "rank", rank.displayName(), "faction", faction.displayName());

        if (state.claimFactionRank(faction.id(), rank.id())) {
            rewardService.grant(player, rank.rewards(), rank.displayName());
        }
    }

    public Optional<FactionRank> currentRank(Player player, Faction faction) {
        return faction.rankFor(stateManager.getOrLoad(player).getReputation(faction.id()));
    }

}
