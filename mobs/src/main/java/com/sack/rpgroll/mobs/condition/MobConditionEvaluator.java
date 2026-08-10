package com.sack.rpgroll.mobs.condition;

import com.sack.rpgroll.mobs.registry.ConditionRegistry;

import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evalúa condiciones de texto tipo "mob.health &lt; 50%", "target.type ==
 * PLAYER", "world == world", "weather == STORM", "night == true". Mismo
 * enfoque que los demás evaluadores del ecosistema — no es un lenguaje de
 * expresiones completo, solo lo que este sistema necesita más lo que un
 * addon sume vía {@link ConditionRegistry}.
 */
public class MobConditionEvaluator {

    private static final Pattern COMPARISON = Pattern.compile("^(\\S+)\\s*(==|!=|<=|>=|<|>)\\s*(\\S+)$");

    private final ConditionRegistry conditionRegistry;

    public MobConditionEvaluator(ConditionRegistry conditionRegistry) {
        this.conditionRegistry = conditionRegistry;
    }

    public boolean evaluateAll(List<String> conditions, MobConditionContext context) {

        for (String condition : conditions) {
            if (!evaluate(condition, context)) {
                return false;
            }
        }

        return true;
    }

    private boolean evaluate(String rawCondition, MobConditionContext context) {

        Matcher matcher = COMPARISON.matcher(rawCondition.trim());

        if (!matcher.matches()) {
            return false;
        }

        return evaluateComparison(matcher.group(1), matcher.group(2), matcher.group(3), context);
    }

    private boolean evaluateComparison(String leftPath, String operator, String rightRaw,
            MobConditionContext context) {

        String path = leftPath.toLowerCase(Locale.ROOT);
        String right = rightRaw.trim();

        if (right.endsWith("%") && isHealthPath(path)) {
            return evaluateHealthPercentage(path, operator, right, context);
        }

        Object left = resolve(path, context);

        if (left == null) {
            return false;
        }

        if (left instanceof Number leftNumber) {
            Double rightNumber = tryParseDouble(right);
            return rightNumber != null && compareNumbers(leftNumber.doubleValue(), operator, rightNumber);
        }

        String leftText = left.toString();

        return switch (operator) {
            case "==" -> leftText.equalsIgnoreCase(right);
            case "!=" -> !leftText.equalsIgnoreCase(right);
            default -> false;
        };
    }

    private boolean isHealthPath(String path) {
        return path.equals("mob.health") || path.equals("target.health");
    }

    private boolean evaluateHealthPercentage(String path, String operator, String right,
            MobConditionContext context) {

        LivingEntity entity = path.equals("mob.health") ? context.mob() : context.target();

        if (entity == null) {
            return false;
        }

        Double percentValue = tryParseDouble(right.substring(0, right.length() - 1));
        if (percentValue == null) {
            return false;
        }

        var maxHealthAttribute = entity.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxHealthAttribute != null ? maxHealthAttribute.getValue() : 20.0;
        double currentPercent = maxHealth <= 0 ? 0 : (entity.getHealth() / maxHealth) * 100.0;

        return compareNumbers(currentPercent, operator, percentValue);
    }

    private boolean compareNumbers(double left, String operator, double right) {
        return switch (operator) {
            case "==" -> left == right;
            case "!=" -> left != right;
            case "<" -> left < right;
            case "<=" -> left <= right;
            case ">" -> left > right;
            case ">=" -> left >= right;
            default -> false;
        };
    }

    private Double tryParseDouble(String raw) {
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Object resolve(String path, MobConditionContext context) {

        LivingEntity mob = context.mob();
        LivingEntity target = context.target();

        Object builtin = switch (path) {
            case "mob.health" -> mob.getHealth();
            case "target.health" -> target != null ? target.getHealth() : null;
            case "target.type" -> target != null ? target.getType().name() : null;
            case "world" -> mob.getWorld().getName();
            case "weather" -> resolveWeather(mob.getWorld());
            case "night" -> isNight(mob.getWorld());
            case "biome" -> mob.getLocation().getBlock().getBiome().getKey().getKey();
            default -> null;
        };

        return builtin != null ? builtin : conditionRegistry.resolve(path, context);
    }

    private String resolveWeather(World world) {

        if (world.isThundering()) {
            return "STORM";
        }

        return world.hasStorm() ? "RAIN" : "CLEAR";
    }

    private boolean isNight(World world) {
        long time = world.getTime();
        return time >= 13000 && time <= 23000;
    }

}
