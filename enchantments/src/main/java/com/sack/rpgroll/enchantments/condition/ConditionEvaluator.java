package com.sack.rpgroll.enchantments.condition;

import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evalúa las condiciones de texto de un encantamiento (ej.
 * "player.health &lt; 10", "world == world_nether", "weather == STORM",
 * "target.type == ZOMBIE", "player.hasPermission(rpg.vip)"). No es un
 * lenguaje de expresiones completo — cubre exactamente las rutas de acceso
 * y operadores que necesita el sistema de encantamientos, nada más.
 */
public class ConditionEvaluator {

    private static final Pattern FUNCTION_CALL = Pattern.compile("^(\\w+)\\.(\\w+)\\((.*)\\)$");
    private static final Pattern COMPARISON = Pattern.compile("^(\\S+)\\s*(==|!=|<=|>=|<|>)\\s*(\\S+)$");

    /** Todas las condiciones deben cumplirse (AND). Lista vacía siempre pasa. */
    public boolean evaluateAll(List<String> conditions, ConditionContext context) {

        for (String condition : conditions) {
            if (!evaluate(condition, context)) {
                return false;
            }
        }

        return true;
    }

    private boolean evaluate(String rawCondition, ConditionContext context) {

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

    private boolean evaluateFunction(String subject, String method, String arg, ConditionContext context) {

        if (subject.equals("player") && method.equals("hasPermission")) {
            return context.player().hasPermission(arg.trim());
        }

        return false;
    }

    private boolean evaluateComparison(String leftPath, String operator, String rightRaw, ConditionContext context) {

        Object left = resolve(leftPath, context);

        if (left == null) {
            return false;
        }

        String right = rightRaw.trim();

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

    private Object resolve(String path, ConditionContext context) {

        Player player = context.player();
        LivingEntity target = context.target();

        return switch (path.toLowerCase(Locale.ROOT)) {
            case "player.health" -> player.getHealth();
            case "player.level" -> (double) player.getLevel();
            case "player.foodlevel" -> (double) player.getFoodLevel();
            case "world" -> player.getWorld().getName();
            case "weather" -> resolveWeather(player.getWorld());
            case "target.type" -> target != null ? target.getType().name() : null;
            case "target.health" -> target != null ? target.getHealth() : null;
            default -> null;
        };
    }

    private String resolveWeather(World world) {

        if (world.isThundering()) {
            return "STORM";
        }

        return world.hasStorm() ? "RAIN" : "CLEAR";
    }

}
