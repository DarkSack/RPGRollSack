package com.sack.rpgroll.api;

import com.sack.rpgroll.common.character.RPGCharacters;
import com.sack.rpgroll.gameplay.job.PlacedBlockTracker;
import com.sack.rpgroll.gameplay.levelup.PlayerLevelUpHandler;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * El core visto desde RPGRoll-Lib: así los módulos leen los datos de
 * personaje sin compilar contra el core, y funcionan también sin él.
 */
public final class CoreCharacters implements RPGCharacters {

    private final PlayerManager players;
    private final ExperienceBonusService experienceBonus;
    private final PlacedBlockTracker placedBlocks;
    private final PlayerLevelUpHandler levelUps;

    public CoreCharacters(PlayerManager players, ExperienceBonusService experienceBonus,
            PlacedBlockTracker placedBlocks, PlayerLevelUpHandler levelUps) {
        this.players = players;
        this.experienceBonus = experienceBonus;
        this.placedBlocks = placedBlocks;
        this.levelUps = levelUps;
    }

    private Optional<RPGPlayer> player(UUID uuid) {
        return players.getPlayer(uuid);
    }

    @Override
    public boolean hasCharacter(UUID player) {
        return player(player).map(RPGPlayer::isCharacterComplete).orElse(false);
    }

    @Override
    public int level(UUID player) {
        return player(player).map(RPGPlayer::getLevel).orElse(0);
    }

    @Override
    public long experience(UUID player) {
        return player(player).map(RPGPlayer::getExperience).orElse(0);
    }

    @Override
    public Optional<String> race(UUID player) {
        return player(player).map(RPGPlayer::getRace);
    }

    @Override
    public Optional<String> playerClass(UUID player) {
        return player(player).map(RPGPlayer::getPlayerClass);
    }

    @Override
    public boolean hasJob(UUID player, String jobId) {
        return player(player).map(p -> p.getJobs().hasJob(jobId)).orElse(false);
    }

    @Override
    public int jobLevel(UUID player, String jobId) {
        return player(player).map(p -> p.getJobs().getLevel(jobId)).orElse(0);
    }

    @Override
    public Set<String> activeJobs(UUID player) {
        return player(player).map(p -> Set.copyOf(p.getJobs().getActiveJobIds())).orElse(Set.of());
    }

    @Override
    public boolean hasTrait(UUID player, String traitId) {
        return player(player).map(p -> p.getTraits().hasTrait(traitId)).orElse(false);
    }

    @Override
    public boolean hasSkill(UUID player, String skillId) {
        return player(player).map(p -> p.getSkills().hasSkill(skillId)).orElse(false);
    }

    @Override
    public int mana(UUID player) {
        return player(player).map(p -> p.getCombatStats().currentMana()).orElse(0);
    }

    @Override
    public int maxMana(UUID player) {
        return player(player).map(p -> p.getCombatStats().maxMana()).orElse(0);
    }

    @Override
    public void addExperience(UUID player, int amount) {
        player(player).ifPresent(p -> {
            RPGPlayer updated = p.addExperience(amount);
            players.savePlayer(updated);

            // Igual que al matar un mob: si la EXP alcanza, sube en el acto.
            Player online = Bukkit.getPlayer(player);
            if (online != null) {
                levelUps.levelUpAll(online, updated);
            }
        });
    }

    @Override
    public int boostExperience(Player player, int base) {
        return experienceBonus.boost(player, base);
    }

    @Override
    public void learnSkill(UUID player, String skillId) {
        player(player).ifPresent(p -> players.savePlayer(p.learnSkill(skillId)));
    }

    @Override
    public void joinJob(UUID player, String jobId) {
        player(player).ifPresent(p -> players.savePlayer(p.joinJob(jobId)));
    }

    @Override
    public boolean isPlayerPlaced(Block block) {
        return placedBlocks.isPlayerPlaced(block);
    }

}
