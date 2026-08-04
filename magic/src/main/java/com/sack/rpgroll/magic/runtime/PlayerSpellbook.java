package com.sack.rpgroll.magic.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Estado de magia de un jugador: hechizos aprendidos, cuál tiene
 * seleccionado para lanzar, qué runas tiene adjuntas a cada uno (tope 3) y
 * los cooldowns activos. Mutable — vive en memoria mientras está online y
 * se persiste vía {@link SpellbookStore}, mismo patrón que
 * QuestPlayerState/QuestPlayerStateStore de RPGRoll-Quests.
 */
public class PlayerSpellbook {

    public static final int MAX_RUNES_PER_SPELL = 3;

    private final UUID uuid;
    private final Set<String> learnedSpells = new HashSet<>();
    private final Map<String, List<String>> attachedRunes = new HashMap<>();
    private final Map<String, Long> cooldownEndMillis = new HashMap<>();
    private String selectedSpellId;

    public PlayerSpellbook(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID uuid() {
        return uuid;
    }

    // ============ Hechizos aprendidos ============

    public boolean knows(String spellId) {
        return learnedSpells.contains(spellId);
    }

    public boolean learn(String spellId) {
        return learnedSpells.add(spellId);
    }

    public void forget(String spellId) {
        learnedSpells.remove(spellId);
        attachedRunes.remove(spellId);
        if (spellId.equals(selectedSpellId)) {
            selectedSpellId = null;
        }
    }

    public Set<String> allLearned() {
        return learnedSpells;
    }

    // ============ Selección activa ============

    public String selectedSpellId() {
        return selectedSpellId;
    }

    public boolean select(String spellId) {

        if (!knows(spellId)) {
            return false;
        }

        selectedSpellId = spellId;
        return true;
    }

    // ============ Runas adjuntas ============

    public List<String> runesFor(String spellId) {
        return attachedRunes.getOrDefault(spellId, List.of());
    }

    public boolean attachRune(String spellId, String runeId) {

        List<String> runes = attachedRunes.computeIfAbsent(spellId, key -> new ArrayList<>());

        if (runes.size() >= MAX_RUNES_PER_SPELL || runes.contains(runeId)) {
            return false;
        }

        runes.add(runeId);
        return true;
    }

    public boolean detachRune(String spellId, String runeId) {

        List<String> runes = attachedRunes.get(spellId);
        return runes != null && runes.remove(runeId);
    }

    public Map<String, List<String>> allAttachedRunes() {
        return attachedRunes;
    }

    // ============ Cooldowns ============

    public boolean isOnCooldown(String spellId, long nowMillis) {
        return cooldownEndMillis.getOrDefault(spellId, 0L) > nowMillis;
    }

    public long remainingCooldownMillis(String spellId, long nowMillis) {
        return Math.max(0, cooldownEndMillis.getOrDefault(spellId, 0L) - nowMillis);
    }

    public void startCooldown(String spellId, long durationMillis, long nowMillis) {
        cooldownEndMillis.put(spellId, nowMillis + durationMillis);
    }

    public Map<String, Long> allCooldowns() {
        return cooldownEndMillis;
    }

}
