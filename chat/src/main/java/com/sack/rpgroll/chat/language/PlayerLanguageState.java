package com.sack.rpgroll.chat.language;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Estado persistente de idiomas de un jugador — cuáles conoce y cuál habla activamente. */
public class PlayerLanguageState {

    private final UUID uuid;
    private final Set<String> knownLanguageIds = new LinkedHashSet<>();
    private String speakingLanguageId;
    private boolean seeded;
    /** Raza con la que se sembró por última vez; null si fue antes de que el jugador eligiera una. */
    private String seededRaceId;

    public PlayerLanguageState(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID uuid() {
        return uuid;
    }

    public Set<String> knownLanguageIds() {
        return Set.copyOf(knownLanguageIds);
    }

    public boolean knows(String languageId) {
        return knownLanguageIds.contains(languageId.toLowerCase(java.util.Locale.ROOT));
    }

    public boolean learn(String languageId) {
        return knownLanguageIds.add(languageId.toLowerCase(java.util.Locale.ROOT));
    }

    public String speakingLanguageId() {
        return speakingLanguageId;
    }

    public void setSpeakingLanguageId(String speakingLanguageId) {
        this.speakingLanguageId = speakingLanguageId;
    }

    public boolean seeded() {
        return seeded;
    }

    public String seededRaceId() {
        return seededRaceId;
    }

    public void markSeeded(String raceId) {
        this.seeded = true;
        this.seededRaceId = raceId;
    }

    public void restore(Set<String> knownLanguageIds, String speakingLanguageId, boolean seeded, String seededRaceId) {
        this.knownLanguageIds.addAll(knownLanguageIds);
        this.speakingLanguageId = speakingLanguageId;
        this.seeded = seeded;
        this.seededRaceId = seededRaceId;
    }

}
