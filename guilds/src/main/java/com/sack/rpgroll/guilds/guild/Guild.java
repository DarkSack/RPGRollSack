package com.sack.rpgroll.guilds.guild;

import com.sack.rpgroll.guilds.guild.bank.GuildVault;
import com.sack.rpgroll.guilds.guild.capital.GuildCapital;
import com.sack.rpgroll.guilds.guild.event.GuildEvent;
import com.sack.rpgroll.guilds.guild.quest.GuildQuestProgress;
import com.sack.rpgroll.guilds.guild.stats.GuildStatistics;
import com.sack.rpgroll.guilds.guild.territory.GuildTerritory;
import com.sack.rpgroll.guilds.guild.upgrade.GuildUpgradeTree;

import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Organización permanente — a diferencia de {@link com.sack.rpgroll.guilds.team.Team}
 * (temporal, en memoria), una Guild persiste en su propio archivo YAML
 * (ver {@code GuildStore}) mientras exista.
 */
public class Guild {

    public static final int MAX_LEVEL = 100;

    private final String id;
    private String name;
    private final UUID founderId;
    private final long createdAtMillis;

    private int level = 1;
    private long experience;

    private final Map<UUID, GuildRole> members = new LinkedHashMap<>();

    private final GuildVault vault = new GuildVault();
    private final GuildCapital capital = new GuildCapital();
    private final GuildUpgradeTree upgradeTree = new GuildUpgradeTree();
    private final List<GuildTerritory> territories = new ArrayList<>();
    private final Map<String, Integer> reputation = new LinkedHashMap<>();
    private final List<GuildQuestProgress> activeQuests = new ArrayList<>();
    private final Set<String> unlockedAchievements = new LinkedHashSet<>();
    private final GuildStatistics statistics = new GuildStatistics();
    private final List<GuildEvent> calendar = new ArrayList<>();
    private final Set<GuildBuff> enabledBuffs = EnumSet.noneOf(GuildBuff.class);

    private NamedTextColor color = NamedTextColor.GOLD;
    private Material icon = Material.SHIELD;
    private String motto = "";
    private String description = "";

    public Guild(String id, String name, UUID founderId) {
        this.id = id;
        this.name = name;
        this.founderId = founderId;
        this.createdAtMillis = System.currentTimeMillis();
        this.members.put(founderId, GuildRole.LEADER);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID founderId() {
        return founderId;
    }

    public long createdAtMillis() {
        return createdAtMillis;
    }

    // ============ Nivel / experiencia ============

    public int level() {
        return level;
    }

    public long experience() {
        return experience;
    }

    public static long experienceForLevel(int level) {
        return (long) (1000 * Math.pow(level, 1.8));
    }

    /** @return true si la guild subió al menos un nivel con esta experiencia. */
    public boolean addExperience(long amount) {

        experience += amount;
        boolean leveledUp = false;

        while (level < MAX_LEVEL && experience >= experienceForLevel(level + 1)) {
            level++;
            leveledUp = true;
        }

        return leveledUp;
    }

    public void restoreLevel(int level, long experience) {
        this.level = Math.max(1, Math.min(MAX_LEVEL, level));
        this.experience = experience;
    }

    // ============ Miembros ============

    public Map<UUID, GuildRole> members() {
        return Map.copyOf(members);
    }

    public boolean isMember(UUID playerId) {
        return members.containsKey(playerId);
    }

    public GuildRole roleOf(UUID playerId) {
        return members.getOrDefault(playerId, GuildRole.RECRUIT);
    }

    public void addMember(UUID playerId, GuildRole role) {
        members.put(playerId, role);
    }

    /** @return true si el fundador/último líder se fue y el liderazgo pasó a otro miembro; false si sigue igual. */
    public void removeMember(UUID playerId) {

        boolean wasLeader = roleOf(playerId) == GuildRole.LEADER;
        members.remove(playerId);

        if (wasLeader && !members.isEmpty()) {
            UUID next = members.keySet().iterator().next();
            members.put(next, GuildRole.LEADER);
        }
    }

    public void setRole(UUID playerId, GuildRole role) {
        if (members.containsKey(playerId)) {
            members.put(playerId, role);
        }
    }

    public int memberCount() {
        return members.size();
    }

    // ============ Subsistemas ============

    public GuildVault vault() {
        return vault;
    }

    public GuildCapital capital() {
        return capital;
    }

    public GuildUpgradeTree upgradeTree() {
        return upgradeTree;
    }

    public List<GuildTerritory> territories() {
        return List.copyOf(territories);
    }

    public void addTerritory(GuildTerritory territory) {
        territories.add(territory);
    }

    public void removeTerritory(String name) {
        territories.removeIf(t -> t.name().equalsIgnoreCase(name));
    }

    public Map<String, Integer> reputation() {
        return Map.copyOf(reputation);
    }

    public int reputationWith(String factionId) {
        return reputation.getOrDefault(factionId.toLowerCase(java.util.Locale.ROOT), 0);
    }

    public void addReputation(String factionId, int amount) {
        reputation.merge(factionId.toLowerCase(java.util.Locale.ROOT), amount, Integer::sum);
    }

    public List<GuildQuestProgress> activeQuests() {
        return List.copyOf(activeQuests);
    }

    public void addActiveQuest(GuildQuestProgress progress) {
        activeQuests.add(progress);
    }

    public void removeActiveQuest(GuildQuestProgress progress) {
        activeQuests.remove(progress);
    }

    public Set<String> unlockedAchievements() {
        return Set.copyOf(unlockedAchievements);
    }

    public boolean unlockAchievement(String achievementId) {
        return unlockedAchievements.add(achievementId);
    }

    public GuildStatistics statistics() {
        return statistics;
    }

    public List<GuildEvent> calendar() {
        return List.copyOf(calendar);
    }

    public void addCalendarEvent(GuildEvent event) {
        calendar.add(event);
    }

    public void removeCalendarEvent(String eventId) {
        calendar.removeIf(e -> e.id().equalsIgnoreCase(eventId));
    }

    public Set<GuildBuff> enabledBuffs() {
        return EnumSet.copyOf(enabledBuffs.isEmpty() ? EnumSet.noneOf(GuildBuff.class) : enabledBuffs);
    }

    public boolean hasBuff(GuildBuff buff) {
        return enabledBuffs.contains(buff);
    }

    /** @return true si se pudo alternar (no se pudo si se intentó activar por encima del cupo del árbol de mejoras). */
    public boolean toggleBuff(GuildBuff buff) {

        if (enabledBuffs.contains(buff)) {
            enabledBuffs.remove(buff);
            return true;
        }

        if (enabledBuffs.size() >= upgradeTree.maxActiveBuffs()) {
            return false;
        }

        enabledBuffs.add(buff);
        return true;
    }

    // ============ Personalización ============

    public NamedTextColor color() {
        return color;
    }

    public void setColor(NamedTextColor color) {
        this.color = color;
    }

    public Material icon() {
        return icon;
    }

    public void setIcon(Material icon) {
        this.icon = icon;
    }

    public String motto() {
        return motto;
    }

    public void setMotto(String motto) {
        this.motto = motto;
    }

    public String description() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
