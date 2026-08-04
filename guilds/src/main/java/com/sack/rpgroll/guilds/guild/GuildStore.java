package com.sack.rpgroll.guilds.guild;

import com.sack.rpgroll.guilds.guild.bank.VaultTransaction;
import com.sack.rpgroll.guilds.guild.bank.VaultTransactionType;
import com.sack.rpgroll.guilds.guild.capital.GuildPoint;
import com.sack.rpgroll.guilds.guild.event.GuildEvent;
import com.sack.rpgroll.guilds.guild.quest.GuildQuestProgress;
import com.sack.rpgroll.guilds.guild.territory.GuildTerritory;
import com.sack.rpgroll.guilds.guild.upgrade.GuildUpgradeBranch;

import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Persiste cada {@link Guild} en su propio archivo plugins/RPGRoll-Guilds/guilds/&lt;id&gt;.yml. */
public class GuildStore {

    private final Plugin plugin;
    private final File folder;

    public GuildStore(Plugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "guilds");

        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    public List<Guild> loadAll() {

        List<Guild> guilds = new ArrayList<>();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));

        if (files == null) {
            return guilds;
        }

        for (File file : files) {
            try {
                guilds.add(load(file));
            } catch (Exception e) {
                plugin.getLogger().warning("✘ Error cargando guild '" + file.getName() + "': " + e.getMessage());
            }
        }

        return guilds;
    }

    private Guild load(File file) {

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        String id = config.getString("id");
        String name = config.getString("name", id);
        UUID founderId = UUID.fromString(config.getString("founder-id"));

        Guild guild = new Guild(id, name, founderId);
        guild.restoreLevel(config.getInt("level", 1), config.getLong("experience", 0));

        var membersSection = config.getConfigurationSection("members");
        if (membersSection != null) {
            for (String uuidStr : membersSection.getKeys(false)) {
                try {
                    guild.addMember(UUID.fromString(uuidStr),
                            GuildRole.valueOf(membersSection.getString(uuidStr)));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        guild.vault().restoreBalance(config.getDouble("vault.balance", 0));

        List<?> rawStorage = config.getList("vault.storage");
        if (rawStorage != null) {
            ItemStack[] storage = new ItemStack[Math.max(9, rawStorage.size())];
            for (int i = 0; i < rawStorage.size(); i++) {
                if (rawStorage.get(i) instanceof ItemStack item) {
                    storage[i] = item;
                }
            }
            guild.vault().restoreStorage(storage);
        }

        List<Map<?, ?>> logRaw = config.getMapList("vault.log");
        List<VaultTransaction> log = new ArrayList<>();
        for (Map<?, ?> entry : logRaw) {
            try {
                log.add(new VaultTransaction(
                        (long) entry.get("timestamp"),
                        entry.get("actor-id") != null ? UUID.fromString((String) entry.get("actor-id")) : null,
                        (String) entry.get("actor-name"),
                        VaultTransactionType.valueOf((String) entry.get("type")),
                        entry.get("amount") != null ? ((Number) entry.get("amount")).doubleValue() : 0,
                        (String) entry.get("description")));
            } catch (Exception ignored) {
            }
        }
        guild.vault().restoreLog(log);

        for (GuildUpgradeBranch branch : GuildUpgradeBranch.values()) {
            guild.upgradeTree().restore(branch, config.getInt("upgrades." + branch.name(), 0));
        }

        if (config.contains("capital.spawn")) {
            guild.capital().setSpawn(readPoint(config, "capital.spawn"));
        }
        if (config.contains("capital.bank")) {
            guild.capital().setBank(readPoint(config, "capital.bank"));
        }
        var teleports = config.getConfigurationSection("capital.teleports");
        if (teleports != null) {
            for (String key : teleports.getKeys(false)) {
                guild.capital().setTeleportPoint(key, readPoint(config, "capital.teleports." + key));
            }
        }

        for (Map<?, ?> entry : config.getMapList("territories")) {
            try {
                GuildTerritory territory = new GuildTerritory(
                        (String) entry.get("name"), (String) entry.get("world"),
                        ((Number) entry.get("min-x")).doubleValue(), ((Number) entry.get("min-y")).doubleValue(),
                        ((Number) entry.get("min-z")).doubleValue(), ((Number) entry.get("max-x")).doubleValue(),
                        ((Number) entry.get("max-y")).doubleValue(), ((Number) entry.get("max-z")).doubleValue());
                Object protectBlocksRaw = entry.get("protect-blocks");
                territory.setProtectBlocks(protectBlocksRaw == null ? true : (Boolean) protectBlocksRaw);
                Object allowPvpRaw = entry.get("allow-outsider-pvp");
                territory.setAllowOutsiderPvp(allowPvpRaw != null && (Boolean) allowPvpRaw);
                guild.addTerritory(territory);
            } catch (Exception ignored) {
            }
        }

        var reputationSection = config.getConfigurationSection("reputation");
        if (reputationSection != null) {
            for (String faction : reputationSection.getKeys(false)) {
                guild.addReputation(faction, reputationSection.getInt(faction));
            }
        }

        for (Map<?, ?> entry : config.getMapList("active-quests")) {
            try {
                GuildQuestProgress progress = new GuildQuestProgress((String) entry.get("quest-id"));
                Map<UUID, Integer> contributions = new LinkedHashMap<>();
                Object rawContrib = entry.get("contributions");
                if (rawContrib instanceof Map<?, ?> map) {
                    for (var contribEntry : map.entrySet()) {
                        contributions.put(UUID.fromString((String) contribEntry.getKey()),
                                ((Number) contribEntry.getValue()).intValue());
                    }
                }
                Object completedRaw = entry.get("completed");
                progress.restore(((Number) entry.get("current-amount")).intValue(),
                        completedRaw != null && (Boolean) completedRaw,
                        entry.get("completed-at") != null ? ((Number) entry.get("completed-at")).longValue() : 0,
                        contributions);
                guild.addActiveQuest(progress);
            } catch (Exception ignored) {
            }
        }

        for (String achievementId : config.getStringList("achievements")) {
            guild.unlockAchievement(achievementId);
        }

        guild.statistics().restore(
                config.getInt("statistics.bosses-defeated", 0),
                config.getInt("statistics.dungeons-completed", 0),
                config.getInt("statistics.quests-completed", 0),
                config.getInt("statistics.deaths", 0),
                config.getInt("statistics.pvp-kills", 0),
                config.getInt("statistics.resources-gathered", 0),
                config.getLong("statistics.playtime-millis", 0));

        for (Map<?, ?> entry : config.getMapList("calendar")) {
            try {
                GuildEvent event = new GuildEvent(
                        (String) entry.get("id"), (String) entry.get("name"), (String) entry.get("type"),
                        (String) entry.get("description"), ((Number) entry.get("scheduled-at")).longValue());
                if (entry.get("linked-dungeon-id") != null) {
                    event.setLinkedDungeonId((String) entry.get("linked-dungeon-id"));
                }
                if (Boolean.TRUE.equals(entry.get("announced"))) {
                    event.markAnnounced();
                }
                guild.addCalendarEvent(event);
            } catch (Exception ignored) {
            }
        }

        for (String buffName : config.getStringList("buffs")) {
            try {
                guild.toggleBuff(com.sack.rpgroll.guilds.guild.GuildBuff.valueOf(buffName));
            } catch (IllegalArgumentException ignored) {
            }
        }

        String colorName = config.getString("customization.color");
        if (colorName != null) {
            NamedTextColor color = NamedTextColor.NAMES.value(colorName.toLowerCase(Locale.ROOT));
            if (color != null) {
                guild.setColor(color);
            }
        }

        String iconName = config.getString("customization.icon");
        if (iconName != null) {
            try {
                guild.setIcon(Material.valueOf(iconName));
            } catch (IllegalArgumentException ignored) {
            }
        }

        guild.setMotto(config.getString("customization.motto", ""));
        guild.setDescription(config.getString("customization.description", ""));

        return guild;
    }

    private GuildPoint readPoint(YamlConfiguration config, String path) {
        return new GuildPoint(
                config.getString(path + ".world"),
                config.getDouble(path + ".x"),
                config.getDouble(path + ".y"),
                config.getDouble(path + ".z"),
                (float) config.getDouble(path + ".yaw"),
                (float) config.getDouble(path + ".pitch"));
    }

    private void writePoint(YamlConfiguration config, String path, GuildPoint point) {
        config.set(path + ".world", point.world());
        config.set(path + ".x", point.x());
        config.set(path + ".y", point.y());
        config.set(path + ".z", point.z());
        config.set(path + ".yaw", point.yaw());
        config.set(path + ".pitch", point.pitch());
    }

    public void save(Guild guild) {

        YamlConfiguration config = new YamlConfiguration();

        config.set("id", guild.id());
        config.set("name", guild.name());
        config.set("founder-id", guild.founderId().toString());
        config.set("level", guild.level());
        config.set("experience", guild.experience());

        for (var entry : guild.members().entrySet()) {
            config.set("members." + entry.getKey(), entry.getValue().name());
        }

        config.set("vault.balance", guild.vault().balance());
        config.set("vault.storage", Arrays.asList(guild.vault().storage()));

        List<Map<String, Object>> logList = new ArrayList<>();
        for (VaultTransaction entry : guild.vault().log()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("timestamp", entry.timestamp());
            map.put("actor-id", entry.actorId() != null ? entry.actorId().toString() : null);
            map.put("actor-name", entry.actorName());
            map.put("type", entry.type().name());
            map.put("amount", entry.amount());
            map.put("description", entry.description());
            logList.add(map);
        }
        config.set("vault.log", logList);

        for (var branchEntry : guild.upgradeTree().all().entrySet()) {
            config.set("upgrades." + branchEntry.getKey().name(), branchEntry.getValue());
        }

        if (guild.capital().spawn() != null) {
            writePoint(config, "capital.spawn", guild.capital().spawn());
        }
        if (guild.capital().bank() != null) {
            writePoint(config, "capital.bank", guild.capital().bank());
        }
        for (var teleportEntry : guild.capital().teleportPoints().entrySet()) {
            writePoint(config, "capital.teleports." + teleportEntry.getKey(), teleportEntry.getValue());
        }

        List<Map<String, Object>> territoriesList = new ArrayList<>();
        for (GuildTerritory territory : guild.territories()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", territory.name());
            map.put("world", territory.world());
            map.put("min-x", territory.minX());
            map.put("min-y", territory.minY());
            map.put("min-z", territory.minZ());
            map.put("max-x", territory.maxX());
            map.put("max-y", territory.maxY());
            map.put("max-z", territory.maxZ());
            map.put("protect-blocks", territory.protectBlocks());
            map.put("allow-outsider-pvp", territory.allowOutsiderPvp());
            territoriesList.add(map);
        }
        config.set("territories", territoriesList);

        for (var reputationEntry : guild.reputation().entrySet()) {
            config.set("reputation." + reputationEntry.getKey(), reputationEntry.getValue());
        }

        List<Map<String, Object>> questsList = new ArrayList<>();
        for (GuildQuestProgress progress : guild.activeQuests()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("quest-id", progress.questId());
            map.put("current-amount", progress.currentAmount());
            map.put("completed", progress.completed());
            map.put("completed-at", progress.completedAtMillis());
            Map<String, Integer> contributions = new LinkedHashMap<>();
            for (var contribEntry : progress.contributions().entrySet()) {
                contributions.put(contribEntry.getKey().toString(), contribEntry.getValue());
            }
            map.put("contributions", contributions);
            questsList.add(map);
        }
        config.set("active-quests", questsList);

        config.set("achievements", new ArrayList<>(guild.unlockedAchievements()));

        config.set("statistics.bosses-defeated", guild.statistics().bossesDefeated());
        config.set("statistics.dungeons-completed", guild.statistics().dungeonsCompleted());
        config.set("statistics.quests-completed", guild.statistics().questsCompleted());
        config.set("statistics.deaths", guild.statistics().deaths());
        config.set("statistics.pvp-kills", guild.statistics().pvpKills());
        config.set("statistics.resources-gathered", guild.statistics().resourcesGathered());
        config.set("statistics.playtime-millis", guild.statistics().playtimeMillis());

        List<Map<String, Object>> calendarList = new ArrayList<>();
        for (GuildEvent event : guild.calendar()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", event.id());
            map.put("name", event.name());
            map.put("type", event.type());
            map.put("description", event.description());
            map.put("scheduled-at", event.scheduledAtMillis());
            map.put("linked-dungeon-id", event.linkedDungeonId());
            map.put("announced", event.announced());
            calendarList.add(map);
        }
        config.set("calendar", calendarList);

        List<String> buffNames = new ArrayList<>();
        guild.enabledBuffs().forEach(buff -> buffNames.add(buff.name()));
        config.set("buffs", buffNames);

        config.set("customization.color", NamedTextColor.NAMES.key(guild.color()));
        config.set("customization.icon", guild.icon().name());
        config.set("customization.motto", guild.motto());
        config.set("customization.description", guild.description());

        try {
            config.save(new File(folder, guild.id() + ".yml"));
        } catch (IOException e) {
            plugin.getLogger().warning("✘ Error guardando guild '" + guild.id() + "': " + e.getMessage());
        }
    }

    public void delete(String guildId) {
        File file = new File(folder, guildId + ".yml");
        if (file.exists()) {
            file.delete();
        }
    }

}
