package com.sack.rpgroll.guilds.guild.chat;

/** Canales de chat de guild (spec: "Guild/Oficiales/Líderes/Alianza"). LEADERS se pliega en OFFICERS (mismo permiso de gestión). */
public enum GuildChatChannel {
    GUILD("G"),
    OFFICERS("O"),
    ALLIANCE("A");

    private final String tag;

    GuildChatChannel(String tag) {
        this.tag = tag;
    }

    public String tag() {
        return tag;
    }
}
