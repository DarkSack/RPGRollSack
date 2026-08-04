package com.sack.rpgroll.effects.condition;

import com.sack.rpgroll.api.RPGRollAPI;
import com.sack.rpgroll.effects.core.EffectCondition;
import com.sack.rpgroll.effects.core.EffectDefinition;
import com.sack.rpgroll.guilds.GuildsAPI;
import com.sack.rpgroll.player.RPGPlayer;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Evalúa los {@link EffectCondition} de un efecto contra el objetivo al que
 * se le quiere aplicar — se chequea una sola vez, al aplicar (no se
 * re-evalúa mientras el efecto sigue activo). Devuelve los motivos de
 * rechazo (vacío = puede aplicarse), mismo patrón que
 * {@code QuestRequirementChecker}.
 * <p>
 * LEVEL/RACE/CLASS/JOB/MANA/GUILD/TEAM/PERMISSION solo tienen sentido para
 * un {@link Player} — sobre cualquier otro {@link LivingEntity} (mobs) esas
 * condiciones se consideran automáticamente satisfechas. GUILD/TEAM son
 * dependencias blandas de {@code RPGRoll-Guilds}: si no está instalado, se
 * consideran no satisfechas (no se puede verificar lo que no existe).
 */
public class EffectConditionEvaluator {

    public List<String> check(LivingEntity target, EffectDefinition effect) {

        List<String> reasons = new ArrayList<>();

        RPGPlayer rpgPlayer = null;

        if (target instanceof Player player && RPGRollAPI.isReady()) {
            rpgPlayer = RPGRollAPI.get().getPlayer(player.getUniqueId()).orElse(null);
        }

        for (EffectCondition condition : effect.conditions()) {
            checkOne(condition, target, rpgPlayer, reasons);
        }

        return reasons;
    }

    private void checkOne(EffectCondition condition, LivingEntity target, RPGPlayer rpgPlayer, List<String> reasons) {

        switch (condition.type()) {

            case LEVEL_MIN -> {
                int min = condition.paramInt("value", 0);
                int level = rpgPlayer != null ? rpgPlayer.getLevel() : 0;
                if (rpgPlayer != null && level < min) {
                    reasons.add("Necesitás nivel " + min + " (tenés " + level + ").");
                }
            }

            case LEVEL_MAX -> {
                int max = condition.paramInt("value", Integer.MAX_VALUE);
                int level = rpgPlayer != null ? rpgPlayer.getLevel() : 0;
                if (rpgPlayer != null && level > max) {
                    reasons.add("Nivel máximo permitido: " + max + " (tenés " + level + ").");
                }
            }

            case RACE -> {
                String race = condition.param("value", "");
                if (rpgPlayer != null && !race.isBlank()
                        && !race.equalsIgnoreCase(String.valueOf(rpgPlayer.getRace()))) {
                    reasons.add("Necesitás ser de la raza: " + race);
                }
            }

            case CLASS -> {
                String playerClass = condition.param("value", "");
                if (rpgPlayer != null && !playerClass.isBlank()
                        && !playerClass.equalsIgnoreCase(String.valueOf(rpgPlayer.getPlayerClass()))) {
                    reasons.add("Necesitás ser de la clase: " + playerClass);
                }
            }

            case JOB -> {
                String job = condition.param("value", "");
                if (rpgPlayer != null && !job.isBlank() && !rpgPlayer.getJobs().hasJob(job)) {
                    reasons.add("Necesitás la profesión: " + job);
                }
            }

            case WORLD -> {
                String world = condition.param("value", "");
                if (!world.isBlank() && !target.getWorld().getName().equalsIgnoreCase(world)) {
                    reasons.add("Tenés que estar en el mundo: " + world);
                }
            }

            case WEATHER -> {
                String expected = condition.param("value", "");
                if (!expected.isBlank()) {
                    String weather = target.getWorld().isThundering() ? "STORM"
                            : target.getWorld().hasStorm() ? "RAIN" : "CLEAR";
                    if (!weather.equalsIgnoreCase(expected)) {
                        reasons.add("Necesitás clima: " + expected);
                    }
                }
            }

            case TIME_RANGE -> {
                long min = condition.paramInt("min", 0);
                long max = condition.paramInt("max", 24000);
                long time = target.getWorld().getTime();
                if (time < min || time > max) {
                    reasons.add("Solo disponible entre las " + min + " y " + max + " (hora del mundo).");
                }
            }

            case HEALTH_BELOW -> {
                double percent = condition.paramDouble("percent", 50);
                if (healthPercent(target) >= percent) {
                    reasons.add("Necesitás tener menos de " + percent + "% de vida.");
                }
            }

            case HEALTH_ABOVE -> {
                double percent = condition.paramDouble("percent", 50);
                if (healthPercent(target) <= percent) {
                    reasons.add("Necesitás tener más de " + percent + "% de vida.");
                }
            }

            case MANA_BELOW -> {
                double percent = condition.paramDouble("percent", 50);
                if (rpgPlayer != null && manaPercent(rpgPlayer) >= percent) {
                    reasons.add("Necesitás tener menos de " + percent + "% de maná.");
                }
            }

            case MANA_ABOVE -> {
                double percent = condition.paramDouble("percent", 50);
                if (rpgPlayer != null && manaPercent(rpgPlayer) <= percent) {
                    reasons.add("Necesitás tener más de " + percent + "% de maná.");
                }
            }

            case GUILD -> {
                if (rpgPlayer == null) {
                    break;
                }
                if (!checkGuild(target, condition.param("value", ""))) {
                    reasons.add("Necesitás pertenecer a un guild"
                            + (condition.param("value", "").isBlank() ? "." : (": " + condition.param("value", ""))));
                }
            }

            case TEAM -> {
                if (rpgPlayer == null) {
                    break;
                }
                if (!checkTeam(target)) {
                    reasons.add("Necesitás estar en un team.");
                }
            }

            case PERMISSION -> {
                String permission = condition.param("value", "");
                if (!permission.isBlank() && !target.hasPermission(permission)) {
                    reasons.add("No tenés el permiso requerido.");
                }
            }
        }
    }

    private double healthPercent(LivingEntity target) {

        AttributeInstance maxHealthAttribute = target.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double maxHealth = maxHealthAttribute != null ? maxHealthAttribute.getValue() : 20.0;

        return maxHealth <= 0 ? 0 : (target.getHealth() / maxHealth) * 100.0;
    }

    private double manaPercent(RPGPlayer rpgPlayer) {

        var stats = rpgPlayer.getCombatStats();

        if (stats == null || stats.maxMana() <= 0) {
            return 0;
        }

        return ((double) stats.currentMana() / stats.maxMana()) * 100.0;
    }

    private boolean checkGuild(LivingEntity target, String requiredGuildId) {

        if (!(target instanceof Player player) || !GuildsAPI.isReady()) {
            return false;
        }

        var guild = GuildsAPI.getGuildManager().findByMember(player.getUniqueId());

        if (guild.isEmpty()) {
            return false;
        }

        return requiredGuildId.isBlank() || guild.get().id().equalsIgnoreCase(requiredGuildId);
    }

    private boolean checkTeam(LivingEntity target) {

        if (!(target instanceof Player player) || !GuildsAPI.isReady()) {
            return false;
        }

        return GuildsAPI.getTeamManager().isInTeam(player.getUniqueId());
    }

}
