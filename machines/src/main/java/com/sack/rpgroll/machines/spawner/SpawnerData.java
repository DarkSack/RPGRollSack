package com.sack.rpgroll.machines.spawner;

import com.sack.rpgroll.machines.spawner.SpawnerSettings.Level;
import com.sack.rpgroll.machines.spawner.SpawnerSettings.Upgrade;

/** Los niveles de un spawner y cuántos hay apilados (1 = uno solo). */
public record SpawnerData(int speed, int count, int range, int stack) {

    public static final SpawnerData NONE = new SpawnerData(0, 0, 0, 1);

    public int level(Upgrade upgrade) {
        return switch (upgrade) {
            case SPEED -> speed;
            case COUNT -> count;
            case RANGE -> range;
        };
    }

    public SpawnerData with(Upgrade upgrade, int level) {
        return switch (upgrade) {
            case SPEED -> new SpawnerData(level, count, range, stack);
            case COUNT -> new SpawnerData(speed, level, range, stack);
            case RANGE -> new SpawnerData(speed, count, level, stack);
        };
    }

    public SpawnerData withStack(int stack) {
        return new SpawnerData(speed, count, range, stack);
    }

    public boolean upgraded() {
        return speed > 0 || count > 0 || range > 0 || stack > 1;
    }

    /** Lo que se le pone al spawner: cada apilado suma una tanda entera (y su tope de mobs cerca). */
    public Applied applied(SpawnerSettings settings) {
        Level speedLevel = settings.track(Upgrade.SPEED).level(speed);
        Level countLevel = settings.track(Upgrade.COUNT).level(count);
        Level rangeLevel = settings.track(Upgrade.RANGE).level(range);
        int stacked = Math.max(1, stack);
        return new Applied(speedLevel.minDelay(), speedLevel.maxDelay(), countLevel.spawnCount() * stacked,
                countLevel.maxNearby() * stacked, rangeLevel.range());
    }

    public record Applied(int minDelay, int maxDelay, int spawnCount, int maxNearby, int range) {
    }
}
