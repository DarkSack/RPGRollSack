package com.sack.rpgroll.tab.scoreboard;

import com.sack.rpgroll.tab.placeholder.PlaceholderEngine;

import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evalúa expresiones simples de una sola comparación, ej.
 * {@code "{health} < 30"} o {@code "{guild_name} == Knights"}. El lado
 * izquierdo siempre es un placeholder ya resuelto por {@link PlaceholderEngine};
 * si ambos lados parsean como número, compara numéricamente — si no, como texto.
 */
public class LineConditionEvaluator {

    private static final Pattern EXPRESSION = Pattern.compile(
            "^(.*?)\\s*(==|!=|>=|<=|>|<)\\s*(.*)$");

    private final PlaceholderEngine placeholderEngine;

    public LineConditionEvaluator(PlaceholderEngine placeholderEngine) {
        this.placeholderEngine = placeholderEngine;
    }

    public boolean evaluate(String condition, Player player) {

        if (condition == null || condition.isBlank()) {
            return true;
        }

        Matcher matcher = EXPRESSION.matcher(condition.trim());

        if (!matcher.matches()) {
            return true;
        }

        String left = placeholderEngine.resolve(matcher.group(1).trim(), player);
        String operator = matcher.group(2);
        String right = placeholderEngine.resolve(matcher.group(3).trim(), player);

        Double leftNumber = tryParse(left);
        Double rightNumber = tryParse(right);

        if (leftNumber != null && rightNumber != null) {
            return evaluateNumeric(leftNumber, operator, rightNumber);
        }

        return evaluateText(left, operator, right);
    }

    private boolean evaluateNumeric(double left, String operator, double right) {

        return switch (operator) {
            case "==" -> left == right;
            case "!=" -> left != right;
            case ">=" -> left >= right;
            case "<=" -> left <= right;
            case ">" -> left > right;
            case "<" -> left < right;
            default -> true;
        };
    }

    private boolean evaluateText(String left, String operator, String right) {

        return switch (operator) {
            case "==" -> left.equalsIgnoreCase(right);
            case "!=" -> !left.equalsIgnoreCase(right);
            default -> true;
        };
    }

    private Double tryParse(String value) {

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
