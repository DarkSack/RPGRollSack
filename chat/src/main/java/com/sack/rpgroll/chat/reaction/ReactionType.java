package com.sack.rpgroll.chat.reaction;

/** Reacciones soportadas — spec: 👍❤️🔥😂⭐⚔. */
public enum ReactionType {
    THUMBS_UP("👍"),
    HEART("❤"),
    FIRE("🔥"),
    LAUGH("😂"),
    STAR("⭐"),
    SWORD("⚔");

    private final String symbol;

    ReactionType(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }

    public static ReactionType fromSymbolOrName(String raw) {

        for (ReactionType type : values()) {
            if (type.symbol.equals(raw) || type.name().equalsIgnoreCase(raw)) {
                return type;
            }
        }

        return switch (raw.toLowerCase(java.util.Locale.ROOT)) {
            case "like", "+1" -> THUMBS_UP;
            case "love" -> HEART;
            case "lol", "haha" -> LAUGH;
            default -> null;
        };
    }

}
