package com.sack.rpgroll.worldforge.api;

/**
 * One thing wrong (or worth knowing) about an input: which field, what, and what to do.
 *
 * @param field dotted path inside the profile or command ("ores.diamond.max-y"), or "" for the whole thing
 */
public record Problem(Severity severity, String field, String message) {

    public enum Severity { ERROR, WARNING }

    public static Problem error(String field, String message) {
        return new Problem(Severity.ERROR, field, message);
    }

    public static Problem warning(String field, String message) {
        return new Problem(Severity.WARNING, field, message);
    }

    public boolean isError() {
        return severity == Severity.ERROR;
    }

    @Override
    public String toString() {
        return (isError() ? "✘ " : "⚠ ") + (field.isEmpty() ? "" : field + ": ") + message;
    }
}
