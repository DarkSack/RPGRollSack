package com.sack.rpgroll.sackresourcepack.validation;

public record ValidationIssue(Severity severity, String message) {

    public enum Severity {
        WARNING, ERROR
    }

    public static ValidationIssue warning(String message) {
        return new ValidationIssue(Severity.WARNING, message);
    }

    public static ValidationIssue error(String message) {
        return new ValidationIssue(Severity.ERROR, message);
    }

}
