package com.sack.rpgroll.database;

import java.nio.file.Path;

public record Migration(

        int version,

        String filename,

        Path path

) implements Comparable<Migration> {

    @Override
    public int compareTo(Migration other) {
        return Integer.compare(version, other.version());
    }

}