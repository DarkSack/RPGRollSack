package com.sack.rpgroll.dungeons.ranking;

public enum RankingPeriod {

    DAILY(24L * 60 * 60 * 1000),
    WEEKLY(7L * 24 * 60 * 60 * 1000),
    MONTHLY(30L * 24 * 60 * 60 * 1000),
    GLOBAL(Long.MAX_VALUE);

    private final long windowMillis;

    RankingPeriod(long windowMillis) {
        this.windowMillis = windowMillis;
    }

    public long windowMillis() {
        return windowMillis;
    }

}
