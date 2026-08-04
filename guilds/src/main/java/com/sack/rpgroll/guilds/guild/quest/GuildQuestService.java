package com.sack.rpgroll.guilds.guild.quest;

import com.sack.rpgroll.guilds.guild.Guild;
import com.sack.rpgroll.guilds.guild.GuildManager;
import com.sack.rpgroll.guilds.guild.achievement.GuildAchievementChecker;
import com.sack.rpgroll.guilds.guild.bank.VaultTransaction;
import com.sack.rpgroll.guilds.guild.bank.VaultTransactionType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Orquesta el progreso de quests de guild — punto único al que otros
 * sistemas (recolección de bloques, integración con Dungeons, etc.)
 * reportan avance sin tener que conocer premios/logros/estadísticas.
 */
public class GuildQuestService {

    private final GuildManager guildManager;
    private final GuildQuestManager questManager;
    private final GuildAchievementChecker achievementChecker = new GuildAchievementChecker();

    public GuildQuestService(GuildManager guildManager, GuildQuestManager questManager) {
        this.guildManager = guildManager;
        this.questManager = questManager;
    }

    public enum StartResult {
        OK,
        NOT_FOUND,
        LEVEL_TOO_LOW,
        ALREADY_ACTIVE
    }

    public StartResult start(Guild guild, String questId) {

        GuildQuestDefinition definition = questManager.get(questId).orElse(null);

        if (definition == null) {
            return StartResult.NOT_FOUND;
        }

        if (guild.level() < definition.minGuildLevel()) {
            return StartResult.LEVEL_TOO_LOW;
        }

        boolean alreadyActive = guild.activeQuests().stream()
                .anyMatch(progress -> progress.questId().equalsIgnoreCase(questId) && !progress.completed());

        if (alreadyActive) {
            return StartResult.ALREADY_ACTIVE;
        }

        guild.addActiveQuest(new GuildQuestProgress(definition.id()));
        return StartResult.OK;
    }

    /** Reporta progreso de todas las quests activas de {@code guild} que coincidan con el tipo/referencia dados. */
    public void reportProgress(Guild guild, GuildQuestType type, String reference, int amount, UUID contributorId) {

        for (GuildQuestProgress progress : guild.activeQuests()) {

            if (progress.completed()) {
                continue;
            }

            GuildQuestDefinition definition = questManager.get(progress.questId()).orElse(null);

            if (definition == null || definition.type() != type) {
                continue;
            }

            if (definition.targetReference() != null && !definition.targetReference().equalsIgnoreCase(reference)) {
                continue;
            }

            boolean justCompleted = progress.addProgress(contributorId, amount, definition.targetAmount());

            if (justCompleted) {
                complete(guild, definition);
            }
        }

        guildManager.save(guild);
    }

    private void complete(Guild guild, GuildQuestDefinition definition) {

        if (definition.rewardMoney() > 0) {
            guild.vault().deposit(definition.rewardMoney(),
                    VaultTransaction.of(null, "Sistema", VaultTransactionType.DONATION,
                            definition.rewardMoney(), "Recompensa de quest: " + definition.displayName()));
        }

        if (definition.rewardXp() > 0) {
            guild.addExperience(definition.rewardXp());
        }

        guild.statistics().incrementQuestsCompleted();

        var unlocked = achievementChecker.check(guild);

        Component message = Component.text("✔ Quest de guild completada: ", NamedTextColor.GREEN)
                .append(Component.text(definition.displayName(), NamedTextColor.YELLOW));

        for (UUID memberId : guild.members().keySet()) {
            Player player = Bukkit.getPlayer(memberId);
            if (player != null) {
                player.sendMessage(message);
                unlocked.forEach(achievement -> player.sendMessage(
                        Component.text("🏆 Logro desbloqueado: " + achievement.displayName(), NamedTextColor.GOLD)));
            }
        }
    }

}
