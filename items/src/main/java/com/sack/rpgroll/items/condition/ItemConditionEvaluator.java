package com.sack.rpgroll.items.condition;

import com.sack.rpgroll.items.registry.ConditionRegistry;

import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evalúa condiciones de texto tipo "player.level &gt;= 25",
 * "player.health &lt; 50%", "target.type == ZOMBIE", "world == world",
 * "weather == STORM". El sufijo "%" en el lado derecho compara contra el
 * porcentaje de vida actual/máxima en vez del valor absoluto — es el único
 * caso especial que este sistema necesita sobre el evaluador genérico ya
 * usado en encantamientos/quests.
 */
public class ItemConditionEvaluator {

    private static final Pattern FUNCTION_CALL = Pattern.compile("^(\\w+)\\.(\\w+)\\((.*)\\)$");
    private static final Pattern COMPARISON = Pattern.compile("^(\\S+)\\s*(==|!=|<=|>=|<|>)\\s*(\\S+)$");

    private final ConditionRegistry conditionRegistry;

    public ItemConditionEvaluator(ConditionRegistry conditionRegistry) {
        this.conditionRegistry = conditionRegistry;
    }

    public boolean evaluateAll(List<String> conditions, ItemConditionContext context) {

        for (String condition : conditions) {
            if (!evaluate(condition, context)) {
                return false;
            }
        }

        return true;
    }

    private boolean evaluate(String rawCondition, ItemConditionContext context) {

        String condition = rawCondition.trim();

        Matcher functionMatcher = FUNCTION_CALL.matcher(condition);
        if (functionMatcher.matches()) {
            return evaluateFunction(functionMatcher.group(1), functionMatcher.group(2),
                    functionMatcher.group(3), context);
        }

        Matcher comparisonMatcher = COMPARISON.matcher(condition);
        if (comparisonMatcher.matches()) {
            return evaluateComparison(comparisonMatcher.group(1), comparisonMatcher.group(2),
                    comparisonMatcher.group(3), context);
        }

        return false;
    }

    private boolean evaluateFunction(String subject, String method, String arg, ItemConditionContext context) {

        if (subject.equals("player") && method.equals("hasPermission")) {
            return context.player().hasPermission(arg.trim());
        }

        return false;
    }

    private boolean evaluateComparison(String leftPath, String operator, String rightRaw,
            ItemConditionContext context) {

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
        return path.equals("player.health") || path.equals("target.health");
    }

    private boolean evaluateHealthPercentage(String path, String operator, String right,
            ItemConditionContext context) {

        LivingEntity entity = path.equals("player.health") ? context.player() : context.target();

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

    private Object resolve(String path, ItemConditionContext context) {

        Player player = context.player();
        LivingEntity target = context.target();

        Object builtin = switch (path) {
            case "player.level" -> (double) player.getLevel();
            case "player.health" -> player.getHealth();
            case "player.foodlevel" -> (double) player.getFoodLevel();
            case "target.type" -> target != null ? target.getType().name() : null;
            case "target.health" -> target != null ? target.getHealth() : null;
            case "world" -> player.getWorld().getName();
            case "weather" -> resolveWeather(player.getWorld());
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

}
