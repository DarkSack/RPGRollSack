package com.sack.rpgroll.extras.expression;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parsea y evalúa expresiones tipo {@code "<=30"}/{@code ">40"}/{@code "==5"} contra un valor numérico dado. */
public final class NumericComparison {

    private static final Pattern PATTERN = Pattern.compile("^(==|!=|>=|<=|>|<)?\\s*(-?\\d+(?:\\.\\d+)?)$");

    private NumericComparison() {
    }

    public static boolean evaluate(String expression, double value) {

        if (expression == null || expression.isBlank()) {
            return true;
        }

        Matcher matcher = PATTERN.matcher(expression.trim());

        if (!matcher.matches()) {
            return false;
        }

        String operator = matcher.group(1) == null ? "==" : matcher.group(1);
        double target = Double.parseDouble(matcher.group(2));

        return switch (operator) {
            case "==" -> value == target;
            case "!=" -> value != target;
            case ">=" -> value >= target;
            case "<=" -> value <= target;
            case ">" -> value > target;
            case "<" -> value < target;
            default -> false;
        };
    }

}
