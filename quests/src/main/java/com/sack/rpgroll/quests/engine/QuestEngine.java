package com.sack.rpgroll.quests.engine;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.api.RPGRollAPI;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.quests.condition.QuestConditionContext;
import com.sack.rpgroll.quests.condition.QuestConditionEvaluator;
import com.sack.rpgroll.quests.core.Dialog;
import com.sack.rpgroll.quests.core.DialogLine;
import com.sack.rpgroll.quests.core.ItemRequirement;
import com.sack.rpgroll.quests.core.Quest;
import com.sack.rpgroll.quests.core.QuestEventType;
import com.sack.rpgroll.quests.core.QuestManager;
import com.sack.rpgroll.quests.core.QuestObjective;
import com.sack.rpgroll.quests.core.QuestStage;
import com.sack.rpgroll.quests.player.ActiveQuestProgress;
import com.sack.rpgroll.quests.player.QuestPlayerState;
import com.sack.rpgroll.quests.player.QuestPlayerStateManager;
import com.sack.rpgroll.quests.registry.ActionRegistry;
import com.sack.rpgroll.quests.registry.QuestActionContext;
import com.sack.rpgroll.quests.requirement.QuestRequirementChecker;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Orquesta todo el ciclo de vida de las quests: iniciar, progresar
 * objetivos, avanzar/saltar de stage, completar, fallar y abandonar. Es el
 * único lugar que muta {@link QuestPlayerState} — listeners y comandos
 * solo llaman a estos métodos, nunca tocan el estado directamente.
 */
public class QuestEngine {


    private final Plugin plugin;
    private final QuestManager questManager;
    private final QuestPlayerStateManager stateManager;
    private final QuestRequirementChecker requirementChecker;
    private final QuestConditionEvaluator conditionEvaluator;
    private final ActionRegistry actionRegistry;
    private final LangManager lang;

    public QuestEngine(
            Plugin plugin,
            QuestManager questManager,
            QuestPlayerStateManager stateManager,
            QuestRequirementChecker requirementChecker,
            QuestConditionEvaluator conditionEvaluator,
            ActionRegistry actionRegistry,
            LangManager lang) {

        this.plugin = plugin;
        this.questManager = questManager;
        this.stateManager = stateManager;
        this.requirementChecker = requirementChecker;
        this.conditionEvaluator = conditionEvaluator;
        this.actionRegistry = actionRegistry;
        this.lang = lang;
    }

    // ============ Ciclo de vida ============

    /** @return lista de motivos de rechazo — vacía significa que la quest arrancó. */
    public List<String> startQuest(Player player, Quest quest) {

        QuestPlayerState state = stateManager.getOrLoad(player);

        if (state.isActive(quest.id())) {
            return List.of(lang.raw("engine.already_active"));
        }

        if (!quest.repeatable() && state.hasCompleted(quest.id())) {
            return List.of(lang.raw("engine.already_completed_not_repeatable"));
        }

        if (quest.repeatable() && isOnCooldown(quest, state)) {
            return List.of(lang.raw("engine.on_cooldown"));
        }

        List<String> reasons = requirementChecker.check(player, quest.requirements(), state);
        if (!reasons.isEmpty()) {
            return reasons;
        }

        long now = System.currentTimeMillis();
        ActiveQuestProgress progress = state.startQuest(quest.id(), now);

        actionRegistry.executeAll(quest.events(QuestEventType.ON_START), new QuestActionContext(player, quest, null));
        enterStage(player, quest, progress, quest.firstStage());

        return List.of();
    }

    public boolean abandonQuest(Player player, String questId) {

        QuestPlayerState state = stateManager.getOrLoad(player);

        if (!state.isActive(questId)) {
            return false;
        }

        questManager.get(questId).ifPresent(quest -> actionRegistry.executeAll(
                quest.events(QuestEventType.ON_ABANDON), new QuestActionContext(player, quest, null)));

        state.abandonQuest(questId);
        return true;
    }

    public void failQuest(Player player, Quest quest) {

        QuestPlayerState state = stateManager.getOrLoad(player);

        actionRegistry.executeAll(quest.events(QuestEventType.ON_FAIL), new QuestActionContext(player, quest, null));
        state.failQuest(quest.id());
    }

    /** Admin: inicia una quest ignorando requisitos, cooldown y estado previo. */
    public void forceStartQuest(Player player, Quest quest) {

        QuestPlayerState state = stateManager.getOrLoad(player);

        if (state.isActive(quest.id())) {
            return;
        }

        long now = System.currentTimeMillis();
        ActiveQuestProgress progress = state.startQuest(quest.id(), now);

        actionRegistry.executeAll(quest.events(QuestEventType.ON_START), new QuestActionContext(player, quest, null));
        enterStage(player, quest, progress, quest.firstStage());
    }

    /** Admin: completa de inmediato una quest activa, otorgando sus recompensas. */
    public boolean forceCompleteQuest(Player player, String questId) {

        QuestPlayerState state = stateManager.getOrLoad(player);
        ActiveQuestProgress progress = state.getActive(questId).orElse(null);
        Optional<Quest> questOpt = questManager.get(questId);

        if (progress == null || questOpt.isEmpty()) {
            return false;
        }

        completeQuest(player, questOpt.get(), progress);
        return true;
    }

    private void completeQuest(Player player, Quest quest, ActiveQuestProgress progress) {

        QuestPlayerState state = stateManager.getOrLoad(player);

        state.completeQuest(quest.id(), System.currentTimeMillis());
        actionRegistry.executeAll(quest.events(QuestEventType.ON_COMPLETE), new QuestActionContext(player, quest, null));
        grantRewards(player, quest);
    }

    private boolean isOnCooldown(Quest quest, QuestPlayerState state) {

        return state.completedAt(quest.id())
                .map(lastCompleted -> System.currentTimeMillis() - lastCompleted < quest.cooldownMillis())
                .orElse(false);
    }

    // ============ Progreso de objetivos ============

    /**
     * Punto de entrada genérico para que listeners (propios o de otros
     * addons) avancen objetivos por tipo+parámetros. Un solo evento puede
     * hacer progresar varias quests activas a la vez si todas tienen un
     * objetivo del mismo tipo esperando esos parámetros.
     */
    public void progressObjective(Player player, String type, Map<String, String> matchParams, int amount) {

        QuestPlayerState state = stateManager.getOrLoad(player);

        for (ActiveQuestProgress progress : List.copyOf(state.allActive().values())) {

            Optional<Quest> questOpt = questManager.get(progress.questId());
            if (questOpt.isEmpty()) {
                continue;
            }

            Quest quest = questOpt.get();
            Optional<QuestStage> stageOpt = quest.stageAt(progress.stageIndex());
            if (stageOpt.isEmpty()) {
                continue;
            }

            QuestStage stage = stageOpt.get();
            List<QuestObjective> objectives = stage.objectives();

            for (int i = 0; i < objectives.size(); i++) {

                QuestObjective objective = objectives.get(i);

                if (!objective.type().equalsIgnoreCase(type) || !objective.matches(matchParams)) {
                    continue;
                }

                if (progress.getProgress(i) >= objective.amount()) {
                    continue;
                }

                applyProgress(player, quest, stage, progress, i, amount);
            }
        }
    }

    /** Completa directamente un objetivo (WAIT/REACH_LOCATION/DISCOVER_REGION, chequeados por polling). */
    public void completeObjectiveDirectly(Player player, Quest quest, QuestStage stage, ActiveQuestProgress progress,
            int objectiveIndex) {

        QuestObjective objective = stage.objectives().get(objectiveIndex);
        int remaining = objective.amount() - progress.getProgress(objectiveIndex);

        if (remaining <= 0) {
            return;
        }

        applyProgress(player, quest, stage, progress, objectiveIndex, remaining);
    }

    private void applyProgress(Player player, Quest quest, QuestStage stage, ActiveQuestProgress progress,
            int objectiveIndex, int amount) {

        progress.addProgress(objectiveIndex, amount);

        actionRegistry.executeAll(stage.events(QuestEventType.ON_PROGRESS),
                new QuestActionContext(player, quest, stage));

        maybeAdvanceStage(player, quest, stage, progress);
    }

    /**
     * Se llama tanto tras cada progreso como en cada tick periódico (para
     * que un stage bloqueado únicamente por condiciones —no por
     * objetivos— avance solo apenas la condición se cumpla, ej. al subir
     * de nivel).
     */
    public void maybeAdvanceStage(Player player, Quest quest, QuestStage stage, ActiveQuestProgress progress) {

        List<QuestObjective> objectives = stage.objectives();

        for (int i = 0; i < objectives.size(); i++) {
            if (progress.getProgress(i) < objectives.get(i).amount()) {
                return;
            }
        }

        RPGPlayer rpgPlayer = RPGRollAPI.isReady() ? RPGRollAPI.get().getPlayer(player.getUniqueId()).orElse(null)
                : null;

        if (!conditionEvaluator.evaluateAll(stage.conditions(), new QuestConditionContext(player, rpgPlayer))) {
            return;
        }

        actionRegistry.executeAll(stage.events(QuestEventType.ON_COMPLETE), new QuestActionContext(player, quest, stage));

        if (quest.isLastStage(progress.stageIndex())) {
            completeQuest(player, quest, progress);
            return;
        }

        QuestStage nextStage = quest.stageAt(progress.stageIndex() + 1).orElseThrow();
        actionRegistry.executeAll(quest.events(QuestEventType.ON_STAGE_CHANGE),
                new QuestActionContext(player, quest, nextStage));

        progress.setStageIndex(progress.stageIndex() + 1);
        enterStage(player, quest, progress, nextStage);
    }

    /** Salta a un stage arbitrario por id — usado por las opciones de diálogo (árbol de decisiones). */
    public void jumpToStage(Player player, Quest quest, String targetStageId) {

        QuestPlayerState state = stateManager.getOrLoad(player);
        ActiveQuestProgress progress = state.getActive(quest.id()).orElse(null);

        if (progress == null) {
            return;
        }

        int targetIndex = quest.indexOfStage(targetStageId);
        if (targetIndex < 0) {
            plugin.getLogger().warning("✘ Stage inexistente '" + targetStageId + "' en quest '" + quest.id() + "'");
            return;
        }

        QuestStage targetStage = quest.stageAt(targetIndex).orElseThrow();

        actionRegistry.executeAll(quest.events(QuestEventType.ON_STAGE_CHANGE),
                new QuestActionContext(player, quest, targetStage));

        progress.setStageIndex(targetIndex);
        enterStage(player, quest, progress, targetStage);
    }

    private void enterStage(Player player, Quest quest, ActiveQuestProgress progress, QuestStage stage) {

        actionRegistry.executeAll(stage.events(QuestEventType.ON_START), new QuestActionContext(player, quest, stage));
        presentDialog(player, quest, stage);

        // Un stage sin objetivos avanza solo — salvo que su diálogo tenga
        // opciones: ahí la decisión del jugador ES el gate de avance.
        boolean waitsForDialogChoice = stage.dialog() != null && stage.dialog().hasOptions();

        if (stage.objectives().isEmpty() && !waitsForDialogChoice) {
            maybeAdvanceStage(player, quest, stage, progress);
        }
    }

    // ============ Diálogo ============

    private void presentDialog(Player player, Quest quest, QuestStage stage) {

        Dialog dialog = stage.dialog();
        if (dialog == null) {
            return;
        }

        for (DialogLine line : dialog.lines()) {

            Component prefix = switch (line.speaker()) {
                case NPC -> lang.component("engine.dialog_npc_prefix");
                case PLAYER -> lang.component("engine.dialog_player_prefix");
            };

            player.sendMessage(prefix.append(ComponentUtils.parse(line.text())));
        }

        if (!dialog.hasOptions()) {
            return;
        }

        lang.send(player, "engine.dialog_choose_option");

        int index = 1;
        for (var option : dialog.options()) {

            String targetStage = option.nextStage();

            Component optionComponent = Component.text(index + ". " + option.label(), NamedTextColor.GREEN)
                    .clickEvent(ClickEvent.callback(audience -> jumpToStage(player, quest, targetStage)));

            player.sendMessage(optionComponent);
            index++;
        }
    }

    // ============ Recompensas ============

    private void grantRewards(Player player, Quest quest) {

        var rewards = quest.rewards();

        if (rewards.money() > 0 && RPGRollAPI.isReady()) {
            RPGRollAPI.get().getEconomyProvider().getEconomy()
                    .ifPresent(economy -> economy.depositPlayer(player, rewards.money()));
        }

        if (rewards.experience() > 0 && RPGRollAPI.isReady()) {

            RPGRollAPI api = RPGRollAPI.get();
            api.getPlayer(player.getUniqueId()).ifPresent(rpgPlayer -> {
                RPGPlayer updated = rpgPlayer.addExperience(rewards.experience());
                api.getPlayerManager().savePlayer(updated);
            });
        }

        for (ItemRequirement item : rewards.items()) {

            ItemStack stack = new ItemStack(item.material(), item.amount());
            var leftover = player.getInventory().addItem(stack);
            leftover.values().forEach(remaining -> player.getWorld().dropItemNaturally(player.getLocation(), remaining));
        }

        for (String command : rewards.commands()) {
            String parsed = command.replace("{player}", player.getName());
            org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), parsed);
        }

        for (String questId : rewards.quests()) {
            questManager.get(questId).ifPresent(nextQuest -> startQuest(player, nextQuest));
        }

        actionRegistry.executeAll(rewards.extra(), new QuestActionContext(player, quest, null));

        lang.send(player, "engine.quest_completed", "name", quest.displayName());
    }

    // ============ Polling (WAIT / REACH_LOCATION / DISCOVER_REGION) ============

    public QuestPlayerStateManager getStateManager() {
        return stateManager;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

}
