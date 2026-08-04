package com.sack.rpgroll.guilds.guild.stats;

/** Contadores agregados de una guild — spec: "estadísticas (jefes, quests, guerras, muertes, PvP, recursos, tiempo)". */
public class GuildStatistics {

    private int bossesDefeated;
    private int dungeonsCompleted;
    private int questsCompleted;
    private int deaths;
    private int pvpKills;
    private int resourcesGathered;
    private long playtimeMillis;

    public int bossesDefeated() {
        return bossesDefeated;
    }

    public void incrementBossesDefeated() {
        bossesDefeated++;
    }

    public int dungeonsCompleted() {
        return dungeonsCompleted;
    }

    public void incrementDungeonsCompleted() {
        dungeonsCompleted++;
    }

    public int questsCompleted() {
        return questsCompleted;
    }

    public void incrementQuestsCompleted() {
        questsCompleted++;
    }

    public int deaths() {
        return deaths;
    }

    public void incrementDeaths() {
        deaths++;
    }

    public int pvpKills() {
        return pvpKills;
    }

    public void incrementPvpKills() {
        pvpKills++;
    }

    public int resourcesGathered() {
        return resourcesGathered;
    }

    public void addResourcesGathered(int amount) {
        resourcesGathered += amount;
    }

    public long playtimeMillis() {
        return playtimeMillis;
    }

    public void addPlaytimeMillis(long millis) {
        playtimeMillis += millis;
    }

    public void restore(int bossesDefeated, int dungeonsCompleted, int questsCompleted, int deaths, int pvpKills,
            int resourcesGathered, long playtimeMillis) {
        this.bossesDefeated = bossesDefeated;
        this.dungeonsCompleted = dungeonsCompleted;
        this.questsCompleted = questsCompleted;
        this.deaths = deaths;
        this.pvpKills = pvpKills;
        this.resourcesGathered = resourcesGathered;
        this.playtimeMillis = playtimeMillis;
    }

}
