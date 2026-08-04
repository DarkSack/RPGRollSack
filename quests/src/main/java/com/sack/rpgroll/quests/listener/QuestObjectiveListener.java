package com.sack.rpgroll.quests.listener;

import com.sack.rpgroll.quests.api.NpcTalkEvent;
import com.sack.rpgroll.quests.core.Quest;
import com.sack.rpgroll.quests.core.QuestObjective;
import com.sack.rpgroll.quests.core.QuestStage;
import com.sack.rpgroll.quests.engine.QuestEngine;
import com.sack.rpgroll.quests.player.ActiveQuestProgress;
import com.sack.rpgroll.quests.player.QuestPlayerState;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Traduce eventos de Bukkit a progreso de objetivos base (KILL_ENTITY,
 * BREAK_BLOCK, PLACE_BLOCK, COLLECT_ITEM, COMMAND) vía
 * {@link QuestEngine#progressObjective}, y maneja TALK_TO_NPC/DELIVER_ITEM
 * a partir del {@link NpcTalkEvent} que dispara cualquier sistema de NPCs.
 * También carga/guarda el estado de quests del jugador al entrar/salir.
 */
public class QuestObjectiveListener implements Listener {

    private final QuestEngine engine;

    public QuestObjectiveListener(QuestEngine engine) {
        this.engine = engine;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        engine.getStateManager().getOrLoad(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        engine.getStateManager().unload(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {

        if (!(event.getEntity().getKiller() instanceof Player killer)) {
            return;
        }

        engine.progressObjective(killer, "KILL_ENTITY", Map.of("entity", event.getEntityType().name()), 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        engine.progressObjective(event.getPlayer(), "BREAK_BLOCK",
                Map.of("material", event.getBlock().getType().name()), 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        engine.progressObjective(event.getPlayer(), "PLACE_BLOCK",
                Map.of("material", event.getBlock().getType().name()), 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ItemStack stack = event.getItem().getItemStack();

        engine.progressObjective(player, "COLLECT_ITEM",
                Map.of("material", stack.getType().name()), stack.getAmount());
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {

        String command = event.getMessage().substring(1).split(" ")[0];

        engine.progressObjective(event.getPlayer(), "COMMAND", Map.of("command", command), 1);
    }

    @EventHandler
    public void onNpcTalk(NpcTalkEvent event) {

        Player player = event.getPlayer();
        String npcId = event.getNpcId();

        engine.progressObjective(player, "TALK_TO_NPC", Map.of("npc", npcId), 1);
        tryDeliverItems(player, npcId);
    }

    /**
     * DELIVER_ITEM no puede pasar por progressObjective genérico: necesita
     * chequear y CONSUMIR items del inventario, no solo comparar parámetros.
     */
    private void tryDeliverItems(Player player, String npcId) {

        QuestPlayerState state = engine.getStateManager().getOrLoad(player);

        for (ActiveQuestProgress progress : List.copyOf(state.allActive().values())) {

            Optional<Quest> questOpt = engine.getQuestManager().get(progress.questId());
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
                tryDeliverOne(player, quest, stage, progress, objectives.get(i), i, npcId);
            }
        }
    }

    private void tryDeliverOne(Player player, Quest quest, QuestStage stage, ActiveQuestProgress progress,
            QuestObjective objective, int index, String npcId) {

        if (!objective.type().equalsIgnoreCase("DELIVER_ITEM")
                || !npcId.equalsIgnoreCase(objective.param("npc", ""))
                || progress.getProgress(index) >= objective.amount()) {
            return;
        }

        org.bukkit.Material material;

        try {
            material = org.bukkit.Material.valueOf(objective.param("material", "").toUpperCase());
        } catch (IllegalArgumentException e) {
            return;
        }

        int required = objective.amount() - progress.getProgress(index);
        int available = player.getInventory().all(material).values().stream()
                .mapToInt(ItemStack::getAmount).sum();

        if (available < required) {
            player.sendMessage(Component.text(
                    "Necesitás " + required + "x " + material + " para entregar.", NamedTextColor.RED));
            return;
        }

        removeItems(player, material, required);
        engine.completeObjectiveDirectly(player, quest, stage, progress, index);
    }

    private void removeItems(Player player, org.bukkit.Material material, int amount) {

        int remaining = amount;

        for (ItemStack stack : player.getInventory().all(material).values()) {

            if (remaining <= 0) {
                break;
            }

            int take = Math.min(remaining, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            remaining -= take;
        }
    }

}
