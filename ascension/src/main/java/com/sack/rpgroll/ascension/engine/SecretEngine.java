package com.sack.rpgroll.ascension.engine;

import com.sack.rpgroll.api.RPGRollAPI;
import com.sack.rpgroll.ascension.deferred.SecretTargetType;
import com.sack.rpgroll.ascension.deferred.SecretUnlockManager;
import com.sack.rpgroll.ascension.deferred.SecretUnlockRequirement;
import com.sack.rpgroll.ascension.player.AscensionPlayerState;
import com.sack.rpgroll.ascension.player.AscensionPlayerStateManager;
import com.sack.rpgroll.ascension.requirement.AscensionRequirementChecker;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.player.RPGPlayer;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

/**
 * Contenido secreto de RPGRoll y de Ascension:
 * <ul>
 *   <li><b>Razas y clases</b>: no se pueden elegir (ni al crear el personaje
 *       ni con {@code /race} o {@code /class}) hasta cumplir los requisitos.
 *       Como el core no deja cambiar de raza o clase una vez elegida, al
 *       cumplirlos el jugador puede reclamarla con {@code /ascend secret claim}.</li>
 *   <li><b>Rasgos</b>: se conceden solos al cumplir los requisitos.</li>
 *   <li><b>Especializaciones</b>: {@code /ascend specialize} no las deja tomar
 *       hasta cumplirlos.</li>
 * </ul>
 * Un secreto cumplido queda desbloqueado para siempre, aunque después el
 * jugador deje de cumplir algún requisito (por ejemplo, tras un legado).
 */
public class SecretEngine {

    public enum ClaimResult {
        OK,
        NOT_FOUND,
        NOT_CLAIMABLE,
        LOCKED,
        FAILED
    }

    private final SecretUnlockManager secretManager;
    private final AscensionPlayerStateManager stateManager;
    private final AscensionRequirementChecker requirementChecker;
    private final LangManager lang;

    public SecretEngine(SecretUnlockManager secretManager, AscensionPlayerStateManager stateManager,
            AscensionRequirementChecker requirementChecker, LangManager lang) {
        this.secretManager = secretManager;
        this.stateManager = stateManager;
        this.requirementChecker = requirementChecker;
        this.lang = lang;
    }

    public SecretUnlockManager getSecretManager() {
        return secretManager;
    }

    /**
     * @return vacío si el contenido no es secreto o ya está desbloqueado;
     *         si no, los requisitos que faltan
     */
    public List<String> lockReasons(Player player, SecretTargetType type, String targetId) {

        Optional<SecretUnlockRequirement> secret = secretManager.find(type, targetId);

        if (secret.isEmpty()) {
            return List.of();
        }

        AscensionPlayerState state = stateManager.getOrLoad(player);

        if (state.getUnlockedSecrets().contains(secret.get().id())) {
            return List.of();
        }

        List<String> reasons = requirementChecker.check(player, secret.get().requirements(), state);

        if (reasons.isEmpty()) {
            discover(player, state, secret.get());
        }

        return reasons;
    }

    /** Desbloquea los secretos cuyos requisitos ya se cumplen, y concede los rasgos. */
    public void check(Player player) {

        AscensionPlayerState state = stateManager.getOrLoad(player);

        for (SecretUnlockRequirement secret : secretManager.getAll()) {
            if (!state.getUnlockedSecrets().contains(secret.id())
                    && requirementChecker.check(player, secret.requirements(), state).isEmpty()) {
                discover(player, state, secret);
            }
        }
    }

    private void discover(Player player, AscensionPlayerState state, SecretUnlockRequirement secret) {

        if (!state.unlockSecret(secret.id())) {
            return;
        }

        if (secret.targetType() == SecretTargetType.TRAIT) {
            grantTrait(player, secret.targetId());
            lang.send(player, "progress.secret_trait", "name", secret.targetId());
            return;
        }

        lang.send(player, "progress.secret_discovered", "type", typeName(secret.targetType()),
                "name", secret.targetId());

        if (secret.targetType() == SecretTargetType.CLASS || secret.targetType() == SecretTargetType.RACE) {
            lang.send(player, "progress.secret_claim_hint", "id", secret.id());
        }
    }

    private void grantTrait(Player player, String traitId) {

        if (!RPGRollAPI.isReady()) {
            return;
        }

        RPGRollAPI.get().getPlayer(player.getUniqueId())
                .filter(rpgPlayer -> !rpgPlayer.getTraits().hasTrait(traitId))
                .ifPresent(rpgPlayer -> RPGRollAPI.get().getPlayerManager().savePlayer(rpgPlayer.acquireTrait(traitId)));
    }

    /** Cambia la raza o clase del jugador a la secreta, recalculando sus stats. */
    public ClaimResult claim(Player player, String secretId) {

        Optional<SecretUnlockRequirement> secretOpt = secretManager.get(secretId);

        if (secretOpt.isEmpty()) {
            return ClaimResult.NOT_FOUND;
        }

        SecretUnlockRequirement secret = secretOpt.get();

        if (secret.targetType() != SecretTargetType.CLASS && secret.targetType() != SecretTargetType.RACE) {
            return ClaimResult.NOT_CLAIMABLE;
        }

        if (!lockReasons(player, secret.targetType(), secret.targetId()).isEmpty()) {
            return ClaimResult.LOCKED;
        }

        if (!RPGRollAPI.isReady()) {
            return ClaimResult.FAILED;
        }

        var service = RPGRollAPI.get().getCharacterChangeService();
        Optional<RPGPlayer> changed = secret.targetType() == SecretTargetType.CLASS
                ? service.changeClass(player, secret.targetId(), true)
                : service.changeRace(player, secret.targetId(), true);

        return changed.isPresent() ? ClaimResult.OK : ClaimResult.FAILED;
    }

    public boolean isUnlocked(Player player, SecretUnlockRequirement secret) {
        return stateManager.getOrLoad(player).getUnlockedSecrets().contains(secret.id());
    }

    public String typeName(SecretTargetType type) {
        return lang.raw("secret_type." + type.name().toLowerCase(java.util.Locale.ROOT));
    }

}
