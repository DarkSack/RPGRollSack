package com.sack.rpgroll.quests.condition;

import com.sack.rpgroll.quests.registry.ConditionRegistry;

import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evalúa las condiciones de texto de un stage (ej. "player.level &gt;= 10",
 * "race == elf", "world == world", "weather == CLEAR"). Mismo enfoque que
 * el evaluador de encantamientos: no es un lenguaje de expresiones
 * completo, solo las rutas y operadores que este sistema necesita — más
 * lo que un addon sume vía {@link ConditionRegistry}.
 */
public class QuestConditionEvaluator {

    private static final Pattern FUNCTION_CALL = Pattern.compile("^(\\w+)\\.(\\w+)\\((.*)\\)$");
    private static final Pattern COMPARISON = Pattern.compile("^(\\S+)\\s*(==|!=|<=|>=|<|>)\\s*(\\S+)$");

    private final ConditionRegistry conditionRegistry;

    public QuestConditionEvaluator(ConditionRegistry conditionRegistry) {
        this.conditionRegistry = conditionRegistry;
    }

    public boolean evaluateAll(List<String> conditions, QuestConditionContext context) {

        for (String condition : conditions) {
            if (!evaluate(condition, context)) {
                return false;
            }
        }

        return true;
    }

    private boolean evaluate(String rawCondition, QuestConditionContext context) {

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

    private boolean evaluateFunction(String subject, String method, String arg, QuestConditionContext context) {

        if (subject.equals("player") && method.equals("hasPermission")) {
            return context.player().hasPermission(arg.trim());
        }

        return false;
    }

    private boolean evaluateComparison(String leftPath, String operator, String rightRaw,
            QuestConditionContext context) {

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

    private Object resolve(String path, QuestConditionContext context) {

        Player player = context.player();
        String normalized = path.toLowerCase(Locale.ROOT);

        Object builtin = switch (normalized) {
            case "player.level" -> context.rpgPlayer() != null ? (double) context.rpgPlayer().getLevel() : null;
            case "player.health" -> player.getHealth();
            case "player.foodlevel" -> (double) player.getFoodLevel();
            case "race" -> context.rpgPlayer() != null ? context.rpgPlayer().getRace() : null;
            case "class" -> context.rpgPlayer() != null ? context.rpgPlayer().getPlayerClass() : null;
            case "world" -> player.getWorld().getName();
            case "weather" -> resolveWeather(player.getWorld());
            default -> null;
        };

        return builtin != null ? builtin : conditionRegistry.resolve(normalized, context);
    }

    private String resolveWeather(World world) {

        if (world.isThundering()) {
            return "STORM";
        }

        return world.hasStorm() ? "RAIN" : "CLEAR";
    }

}
