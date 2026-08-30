package com.sack.rpgroll.traps.condition;

import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evalúa condiciones de texto tipo "player.level &gt;= 30",
 * "player.sneaking == true", "player.health &lt; 50%", "world == world",
 * "night == true". Mismo enfoque que los demás evaluadores del ecosistema
 * (MobConditionEvaluator, ItemConditionEvaluator) — no es un lenguaje de
 * expresiones completo, solo lo que este sistema necesita más lo que se
 * sume vía {@link TrapConditionRegistry}. Si no hay jugador en contexto
 * (disparo por TIMER/REDSTONE sin un jugador cerca) cualquier condición
 * "player.*" se evalúa como no cumplida.
 */
public class TrapConditionEvaluator {

    private static final Pattern COMPARISON = Pattern.compile("^(\\S+)\\s*(==|!=|<=|>=|<|>)\\s*(\\S+)$");

    private final TrapConditionRegistry conditionRegistry;

    public TrapConditionEvaluator(TrapConditionRegistry conditionRegistry) {
        this.conditionRegistry = conditionRegistry;
    }

    public boolean evaluateAll(List<String> conditions, TrapConditionContext context) {

        for (String condition : conditions) {
            if (!evaluate(condition, context)) {
                return false;
            }
        }

        return true;
    }

    private boolean evaluate(String rawCondition, TrapConditionContext context) {

        Matcher matcher = COMPARISON.matcher(rawCondition.trim());

        if (!matcher.matches()) {
            return false;
        }

        return evaluateComparison(matcher.group(1), matcher.group(2), matcher.group(3), context);
    }

    private boolean evaluateComparison(String leftPath, String operator, String rightRaw,
            TrapConditionContext context) {

        String path = leftPath.toLowerCase(Locale.ROOT);
        String right = rightRaw.trim();

        if (right.endsWith("%") && path.equals("player.health")) {
            return evaluateHealthPercentage(operator, right, context);
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

    private boolean evaluateHealthPercentage(String operator, String right, TrapConditionContext context) {

        Player player = context.player();
        if (player == null) {
            return false;
        }

        Double percentValue = tryParseDouble(right.substring(0, right.length() - 1));
        if (percentValue == null) {
            return false;
        }

        var maxHealthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxHealthAttribute != null ? maxHealthAttribute.getValue() : 20.0;
        double currentPercent = maxHealth <= 0 ? 0 : (player.getHealth() / maxHealth) * 100.0;

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

    private Object resolve(String path, TrapConditionContext context) {

        Player player = context.player();

        Object builtin = switch (path) {
            case "player.level" -> player != null ? (double) player.getLevel() : null;
            case "player.health" -> player != null ? player.getHealth() : null;
            case "player.sneaking" -> player != null ? player.isSneaking() : null;
            case "player.foodlevel" -> player != null ? (double) player.getFoodLevel() : null;
            case "world" -> player != null ? player.getWorld().getName() : null;
            case "weather" -> player != null ? resolveWeather(player.getWorld()) : null;
            case "night" -> player != null && isNight(player.getWorld());
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
