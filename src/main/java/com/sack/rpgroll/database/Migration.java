package com.sack.rpgroll.database;

public record Migration(

        int version,

        String filename,

        String resourcePath

) implements Comparable<Migration> {

    @Override
    public int compareTo(Migration other) {
        return Integer.compare(version, other.version());
    }

}