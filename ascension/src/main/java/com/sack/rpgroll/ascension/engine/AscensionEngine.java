package com.sack.rpgroll.ascension.engine;

import com.sack.rpgroll.api.RPGRollAPI;
import com.sack.rpgroll.ascension.core.ClassSpecialization;
import com.sack.rpgroll.ascension.core.ClassSpecializationManager;
import com.sack.rpgroll.ascension.core.PrestigeManager;
import com.sack.rpgroll.ascension.core.RaceEvolution;
import com.sack.rpgroll.ascension.core.RaceEvolutionManager;
import com.sack.rpgroll.ascension.core.TalentNode;
import com.sack.rpgroll.ascension.deferred.LegacyManager;
import com.sack.rpgroll.ascension.player.AscensionPlayerState;
import com.sack.rpgroll.ascension.player.AscensionPlayerStateManager;
import com.sack.rpgroll.ascension.requirement.AscensionRequirementChecker;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.player.identity.PlayerIdentity;
import com.sack.rpgroll.player.progression.PlayerProgression;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Orquesta los 4 pilares con mecánica real: evolución de raza,
 * especialización + talentos, prestigio y afinidades. Los sistemas
 * "diferidos" (jobs avanzados, dominio, contenido secreto, reputación,
 * logros, títulos, legado) viven acá también para las operaciones de
 * datos simples (grant/query) que sí forman parte de esta pasada.
 */
public class AscensionEngine {

    private final Plugin plugin;
    private final AscensionPlayerStateManager stateManager;
    private final RaceEvolutionManager evolutionManager;
    private final ClassSpecializationManager specializationManager;
    private final PrestigeManager prestigeManager;
    private final LegacyManager legacyManager;
    private final AscensionRequirementChecker requirementChecker;

    private final NamespacedKey healthModifierKey;
    private final NamespacedKey speedModifierKey;

    public AscensionEngine(
            Plugin plugin,
            AscensionPlayerStateManager stateManager,
            RaceEvolutionManager evolutionManager,
            ClassSpecializationManager specializationManager,
            PrestigeManager prestigeManager,
            LegacyManager legacyManager,
            AscensionRequirementChecker requirementChecker) {

        this.plugin = plugin;
        this.stateManager = stateManager;
        this.evolutionManager = evolutionManager;
        this.specializationManager = specializationManager;
        this.prestigeManager = prestigeManager;
        this.legacyManager = legacyManager;
        this.requirementChecker = requirementChecker;
        this.healthModifierKey = new NamespacedKey(plugin, "ascension-health-modifier");
        this.speedModifierKey = new NamespacedKey(plugin, "ascension-speed-modifier");
    }

    public AscensionPlayerStateManager getStateManager() {
        return stateManager;
    }

    public RaceEvolutionManager getEvolutionManager() {
        return evolutionManager;
    }

    public ClassSpecializationManager getSpecializationManager() {
        return specializationManager;
    }

    public PrestigeManager getPrestigeManager() {
        return prestigeManager;
    }

    public LegacyManager getLegacyManager() {
        return legacyManager;
    }

    // ============ Evolución de raza ============

    public List<String> evolveRace(Player player, RaceEvolution evolution) {

        AscensionPlayerState state = stateManager.getOrLoad(player);

        Optional<RPGPlayer> rpgPlayerOpt = RPGRollAPI.isReady()
                ? RPGRollAPI.get().getPlayer(player.getUniqueId())
                : Optional.empty();

        if (rpgPlayerOpt.isEmpty()) {
            return List.of("No se pudo leer tu personaje de RPGRoll.");
        }

        RPGPlayer rpgPlayer = rpgPlayerOpt.get();

        if (!evolution.baseRace().equalsIgnoreCase(rpgPlayer.getRace())) {
            return List.of("Esta evolución no corresponde a tu raza: " + evolution.baseRace());
        }

        List<String> reasons = requirementChecker.check(player, evolution.requirements(), state);
        if (!reasons.isEmpty()) {
            return reasons;
        }

        for (String traitId : evolution.grantedTraits()) {
            rpgPlayer = rpgPlayer.acquireTrait(traitId);
        }
        for (String skillId : evolution.grantedSkills()) {
            rpgPlayer = rpgPlayer.learnSkill(skillId);
        }

        RPGRollAPI.get().getPlayerManager().savePlayer(rpgPlayer);

        state.setCurrentEvolutionId(evolution.id());
        applyAttributeBonuses(player, state);

        return List.of();
    }

    // ============ Especialización + talentos ============

    public List<String> specialize(Player player, ClassSpecialization specialization) {

        AscensionPlayerState state = stateManager.getOrLoad(player);

        RPGPlayer rpgPlayer = RPGRollAPI.isReady()
                ? RPGRollAPI.get().getPlayer(player.getUniqueId()).orElse(null)
                : null;

        if (rpgPlayer == null) {
            return List.of("No se pudo leer tu personaje de RPGRoll.");
        }

        if (!specialization.baseClass().equalsIgnoreCase(rpgPlayer.getPlayerClass())) {
            return List.of("Esta especialización no corresponde a tu clase: " + specialization.baseClass());
        }

        List<String> reasons = requirementChecker.check(player, specialization.requirements(), state);
        if (!reasons.isEmpty()) {
            return reasons;
        }

        state.setCurrentSpecializationId(specialization.id());
        state.getUnlockedTalents().clear();
        applyAttributeBonuses(player, state);

        return List.of();
    }

    public enum TalentResult {
        OK,
        NO_SPECIALIZATION,
        NODE_NOT_FOUND,
        ALREADY_UNLOCKED,
        MISSING_PREREQUISITES,
        NOT_ENOUGH_POINTS
    }

    public TalentResult unlockTalent(Player player, String talentId) {

        AscensionPlayerState state = stateManager.getOrLoad(player);

        if (state.getCurrentSpecializationId() == null) {
            return TalentResult.NO_SPECIALIZATION;
        }

        ClassSpecialization specialization = specializationManager.get(state.getCurrentSpecializationId())
                .orElse(null);

        if (specialization == null) {
            return TalentResult.NO_SPECIALIZATION;
        }

        TalentNode node = specialization.talent(talentId).orElse(null);
        if (node == null) {
            return TalentResult.NODE_NOT_FOUND;
        }

        if (state.hasUnlockedTalent(node.id())) {
            return TalentResult.ALREADY_UNLOCKED;
        }

        boolean prerequisitesMet = node.prerequisites().stream().allMatch(state::hasUnlockedTalent);
        if (!prerequisitesMet) {
            return TalentResult.MISSING_PREREQUISITES;
        }

        if (state.getAvailableTalentPoints() < node.cost()) {
            return TalentResult.NOT_ENOUGH_POINTS;
        }

        state.spendTalentPoints(node.cost());
        state.getUnlockedTalents().add(node.id());

        grantTalentRewards(player, node);
        applyAttributeBonuses(player, state);

        return TalentResult.OK;
    }

    private void grantTalentRewards(Player player, TalentNode node) {

        if (!RPGRollAPI.isReady()) {
            return;
        }

        RPGRollAPI api = RPGRollAPI.get();
        Optional<RPGPlayer> rpgPlayerOpt = api.getPlayer(player.getUniqueId());

        if (rpgPlayerOpt.isEmpty()) {
            return;
        }

        RPGPlayer rpgPlayer = rpgPlayerOpt.get();

        if (node.grantedSkill() != null) {
            rpgPlayer = rpgPlayer.learnSkill(node.grantedSkill());
        }
        if (node.grantedTrait() != null) {
            rpgPlayer = rpgPlayer.acquireTrait(node.grantedTrait());
        }

        api.getPlayerManager().savePlayer(rpgPlayer);

        if (node.grantedEnchantment() != null) {
            com.sack.rpgroll.ascension.integration.EnchantmentsIntegration.grantToHeldItem(player,
                    node.grantedEnchantment());
        }
    }

    // ============ Prestigio ============

    public List<String> prestige(Player player) {

        AscensionPlayerState state = stateManager.getOrLoad(player);

        if (!RPGRollAPI.isReady()) {
            return List.of("RPGRoll Core todavía no está listo.");
        }

        RPGPlayer rpgPlayer = RPGRollAPI.get().getPlayer(player.getUniqueId()).orElse(null);
        if (rpgPlayer == null) {
            return List.of("No se pudo leer tu personaje de RPGRoll.");
        }

        int nextPrestige = state.getPrestigeCount() + 1;
        int requiredLevel = prestigeManager.getByNumber(nextPrestige).map(p -> p.requiredLevel()).orElse(100);

        if (rpgPlayer.getLevel() < requiredLevel) {
            return List.of("Necesitás nivel " + requiredLevel + " para prestigiar (tenés "
                    + rpgPlayer.getLevel() + ").");
        }

        PlayerIdentity identity = new PlayerIdentity(rpgPlayer.getUUID(), rpgPlayer.getUsername(),
                rpgPlayer.getRace(), rpgPlayer.getPlayerClass());
        PlayerProgression resetProgression = new PlayerProgression(1, 0, rpgPlayer.getProgression().createdAt(),
                System.currentTimeMillis(), 0);

        RPGPlayer resetPlayer = RPGPlayer.from(identity, rpgPlayer.getStats(), resetProgression,
                rpgPlayer.getSkills(), rpgPlayer.getTraits(), rpgPlayer.getCombatStats(), rpgPlayer.getJobs());

        List<String> grantedSkills = prestigeManager.getByNumber(nextPrestige)
                .map(level -> level.grantedSkills())
                .orElse(List.of());

        for (String skillId : grantedSkills) {
            resetPlayer = resetPlayer.learnSkill(skillId);
        }

        state.incrementPrestige();
        RPGRollAPI.get().getPlayerManager().savePlayer(resetPlayer);

        return List.of();
    }

    public double getExperienceBonusPercent(Player player) {

        AscensionPlayerState state = stateManager.getOrLoad(player);
        return prestigeManager.cumulativeExpBonus(state.getPrestigeCount()) + state.getPermanentExpBonusPercent();
    }

    // ============ Legado ============

    public List<String> performLegacy(Player player) {

        AscensionPlayerState state = stateManager.getOrLoad(player);

        var tier = legacyManager.highestAvailable(state.getPrestigeCount());
        if (tier.isEmpty()) {
            return List.of("Todavía no alcanzaste el prestigio mínimo para un Legado.");
        }

        if (!RPGRollAPI.isReady()) {
            return List.of("RPGRoll Core todavía no está listo.");
        }

        RPGPlayer rpgPlayer = RPGRollAPI.get().getPlayer(player.getUniqueId()).orElse(null);
        if (rpgPlayer == null) {
            return List.of("No se pudo leer tu personaje de RPGRoll.");
        }

        PlayerIdentity identity = new PlayerIdentity(rpgPlayer.getUUID(), rpgPlayer.getUsername(),
                rpgPlayer.getRace(), rpgPlayer.getPlayerClass());
        PlayerProgression resetProgression = new PlayerProgression(1, 0, System.currentTimeMillis(),
                System.currentTimeMillis(), tier.get().bonusStatPoints());

        RPGPlayer legacyPlayer = RPGPlayer.from(identity, rpgPlayer.getStats(), resetProgression,
                rpgPlayer.getSkills(), rpgPlayer.getTraits(), rpgPlayer.getCombatStats(), rpgPlayer.getJobs());

        RPGRollAPI.get().getPlayerManager().savePlayer(legacyPlayer);

        state.resetForLegacy();
        state.incrementLegacy();
        state.addPermanentExpBonusPercent(tier.get().permanentExpBonusPercent());

        clearAttributeBonuses(player);

        return List.of();
    }

    // ============ Stats (evolución + especialización + talentos) ============

    /**
     * Los AttributeModifier no persisten entre reinicios del servidor —
     * hay que reaplicarlos en cada login (mismo patrón que
     * RaceAttributeApplier en :core).
     */
    public void reapplyBonuses(Player player) {
        applyAttributeBonuses(player, stateManager.getOrLoad(player));
    }

    private void applyAttributeBonuses(Player player, AscensionPlayerState state) {

        Map<String, Double> combined = new HashMap<>();

        if (state.getCurrentEvolutionId() != null) {
            evolutionManager.get(state.getCurrentEvolutionId())
                    .ifPresent(evolution -> evolution.statBonus().forEach((k, v) -> combined.merge(k, v, Double::sum)));
        }

        if (state.getCurrentSpecializationId() != null) {
            Optional<ClassSpecialization> specializationOpt = specializationManager.get(
                    state.getCurrentSpecializationId());

            specializationOpt.ifPresent(specialization -> {

                specialization.statBonus().forEach((k, v) -> combined.merge(k, v, Double::sum));

                for (TalentNode node : specialization.talentTree()) {
                    if (state.hasUnlockedTalent(node.id())) {
                        node.statBonus().forEach((k, v) -> combined.merge(k, v, Double::sum));
                    }
                }
            });
        }

        applyAttribute(player, Attribute.GENERIC_MAX_HEALTH, healthModifierKey, combined.getOrDefault("health", 0.0));
        applyAttribute(player, Attribute.GENERIC_MOVEMENT_SPEED, speedModifierKey,
                combined.getOrDefault("speed", 0.0) / 100.0);
    }

    private void clearAttributeBonuses(Player player) {
        applyAttribute(player, Attribute.GENERIC_MAX_HEALTH, healthModifierKey, 0);
        applyAttribute(player, Attribute.GENERIC_MOVEMENT_SPEED, speedModifierKey, 0);
    }

    private void applyAttribute(Player player, Attribute attribute, NamespacedKey key, double amount) {

        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        instance.getModifiers().stream()
                .filter(modifier -> modifier.getKey().equals(key))
                .findFirst()
                .ifPresent(instance::removeModifier);

        if (amount == 0) {
            return;
        }

        instance.addModifier(new AttributeModifier(key, amount, AttributeModifier.Operation.ADD_NUMBER));
    }

}
