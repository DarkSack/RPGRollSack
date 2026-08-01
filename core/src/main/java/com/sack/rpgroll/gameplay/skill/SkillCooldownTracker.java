package com.sack.rpgroll.gameplay.skill;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registra en memoria los cooldowns de habilidades por jugador: uno propio
 * por skill (ver {@link Skill#cooldownSeconds()}) y uno global compartido
 * entre todas las skills (ver {@code skills.global_cooldown} en
 * gameplay.yml), para evitar que se encadenen varias skills sin pausa.
 * <p>
 * No se persiste — es intencional que los cooldowns se reinicien si el
 * jugador se desconecta o el servidor reinicia.
 */
public class SkillCooldownTracker {

    private final Map<UUID, Map<String, Long>> lastUsedPerSkill = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastUsedAny = new ConcurrentHashMap<>();

    /**
     * @return segundos restantes antes de poder usar la skill (0 si ya está lista)
     */
    public double getRemainingSeconds(UUID uuid, String skillId, int skillCooldownSeconds,
            double globalCooldownSeconds) {

        long now = System.currentTimeMillis();

        Long lastAny = lastUsedAny.get(uuid);
        if (lastAny != null) {
            double elapsed = (now - lastAny) / 1000.0;
            double remaining = globalCooldownSeconds - elapsed;
            if (remaining > 0) {
                return remaining;
            }
        }

        Long lastSkill = lastUsedPerSkill.getOrDefault(uuid, Map.of()).get(skillId);
        if (lastSkill != null) {
            double elapsed = (now - lastSkill) / 1000.0;
            double remaining = skillCooldownSeconds - elapsed;
            if (remaining > 0) {
                return remaining;
            }
        }

        return 0;
    }

    public void recordUse(UUID uuid, String skillId) {
        long now = System.currentTimeMillis();
        lastUsedAny.put(uuid, now);
        lastUsedPerSkill.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(skillId, now);
    }

    public void clear(UUID uuid) {
        lastUsedAny.remove(uuid);
        lastUsedPerSkill.remove(uuid);
    }

}
