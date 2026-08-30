package com.sack.rpgroll.guilds.guild;

import com.sack.rpgroll.guilds.guild.territory.GuildTerritory;
import com.sack.rpgroll.guilds.guild.upgrade.GuildUpgradeBranch;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuildTest {

    private final UUID founder = UUID.randomUUID();

    @Test
    void founderStartsAsLeaderAtLevelOne() {
        Guild guild = new Guild("crypt", "Crypt Guild", founder);

        assertEquals(1, guild.level());
        assertEquals(0, guild.experience());
        assertTrue(guild.isMember(founder));
        assertEquals(GuildRole.LEADER, guild.roleOf(founder));
    }

    @Test
    void unknownMemberDefaultsToRecruitRole() {
        Guild guild = new Guild("crypt", "Crypt Guild", founder);
        assertEquals(GuildRole.RECRUIT, guild.roleOf(UUID.randomUUID()));
    }

    @Test
    void addExperienceLevelsUpWhenThresholdReached() {
        Guild guild = new Guild("crypt", "Crypt Guild", founder);

        boolean leveledUp = guild.addExperience(Guild.experienceForLevel(2));

        assertTrue(leveledUp);
        assertEquals(2, guild.level());
    }

    @Test
    void addExperienceBelowThresholdDoesNotLevelUp() {
        Guild guild = new Guild("crypt", "Crypt Guild", founder);

        boolean leveledUp = guild.addExperience(1);

        assertFalse(leveledUp);
        assertEquals(1, guild.level());
    }

    @Test
    void addExperienceCanCascadeMultipleLevels() {
        Guild guild = new Guild("crypt", "Crypt Guild", founder);

        guild.addExperience(Guild.experienceForLevel(5));

        assertTrue(guild.level() >= 5);
    }

    @Test
    void levelNeverExceedsMaxLevel() {
        Guild guild = new Guild("crypt", "Crypt Guild", founder);

        guild.addExperience(Long.MAX_VALUE / 2);

        assertEquals(Guild.MAX_LEVEL, guild.level());
    }

    @Test
    void restoreLevelClampsToValidRange() {
        Guild guild = new Guild("crypt", "Crypt Guild", founder);

        guild.restoreLevel(-5, 100);
        assertEquals(1, guild.level());

        guild.restoreLevel(9999, 100);
        assertEquals(Guild.MAX_LEVEL, guild.level());
    }

    @Test
    void removingLeaderPromotesAnotherRemainingMember() {
        Guild guild = new Guild("crypt", "Crypt Guild", founder);
        UUID other = UUID.randomUUID();
        guild.addMember(other, GuildRole.MEMBER);

        guild.removeMember(founder);

        assertFalse(guild.isMember(founder));
        assertEquals(GuildRole.LEADER, guild.roleOf(other));
    }

    @Test
    void removingLastMemberLeavesGuildEmpty() {
        Guild guild = new Guild("crypt", "Crypt Guild", founder);
        guild.removeMember(founder);

        assertEquals(0, guild.memberCount());
    }

    @Test
    void addReputationAccumulatesPerFactionCaseInsensitively() {
        Guild guild = new Guild("crypt", "Crypt Guild", founder);
        guild.addReputation("Bandits", 10);
        guild.addReputation("bandits", 5);

        assertEquals(15, guild.reputationWith("BANDITS"));
        assertEquals(0, guild.reputationWith("unknown"));
    }

    @Test
    void toggleBuffRespectsUpgradeTreeCapacity() {
        Guild guild = new Guild("crypt", "Crypt Guild", founder);

        assertTrue(guild.toggleBuff(GuildBuff.XP_BOOST));
        assertFalse(guild.toggleBuff(GuildBuff.LOOT_BOOST));

        assertTrue(guild.toggleBuff(GuildBuff.XP_BOOST));
        assertTrue(guild.toggleBuff(GuildBuff.LOOT_BOOST));
    }

    @Test
    void toggleBuffCapacityGrowsWithUpgradeTree() {
        Guild guild = new Guild("crypt", "Crypt Guild", founder);
        guild.upgradeTree().upgrade(GuildUpgradeBranch.BUFFS);

        assertTrue(guild.toggleBuff(GuildBuff.XP_BOOST));
        assertTrue(guild.toggleBuff(GuildBuff.LOOT_BOOST));
        assertFalse(guild.toggleBuff(GuildBuff.PROFESSION_BOOST));
    }

    @Test
    void removeTerritoryIsCaseInsensitive() {
        Guild guild = new Guild("crypt", "Crypt Guild", founder);
        guild.addTerritory(new GuildTerritory("Base", "world", 0, 0, 0, 10, 10, 10));

        guild.removeTerritory("BASE");

        assertTrue(guild.territories().isEmpty());
    }

    @Test
    void unlockAchievementReturnsFalseWhenAlreadyUnlocked() {
        Guild guild = new Guild("crypt", "Crypt Guild", founder);

        assertTrue(guild.unlockAchievement("first-blood"));
        assertFalse(guild.unlockAchievement("first-blood"));
    }
}
