package com.sack.rpgroll.worldforge.api;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * The outcome of a service call: a value, or the problems that stopped it. Warnings can
 * travel with a value; any {@link Problem.Severity#ERROR} means there is no value.
 * Commands and the GUI show {@link #problems()} as they are.
 */
public final class Result<T> {

    private final T value;
    private final List<Problem> problems;

    private Result(T value, List<Problem> problems) {
        this.value = value;
        this.problems = List.copyOf(problems);
    }

    public static <T> Result<T> ok(T value) {
        return new Result<>(value, List.of());
    }

    public static <T> Result<T> ok(T value, List<Problem> warnings) {
        if (warnings.stream().anyMatch(Problem::isError)) {
            throw new IllegalArgumentException("ok() with errors: " + warnings);
        }
        return new Result<>(value, warnings);
    }

    public static <T> Result<T> fail(List<Problem> problems) {
        if (problems.stream().noneMatch(Problem::isError)) {
            throw new IllegalArgumentException("fail() needs at least one error");
        }
        return new Result<>(null, problems);
    }

    public static <T> Result<T> fail(String field, String message) {
        return fail(List.of(Problem.error(field, message)));
    }

    /** Value if {@code problems} holds no error, failure otherwise. */
    public static <T> Result<T> of(T value, List<Problem> problems) {
        return problems.stream().anyMatch(Problem::isError) ? fail(problems) : ok(value, problems);
    }

    public boolean isOk() {
        return problems.stream().noneMatch(Problem::isError);
    }

    public T value() {
        if (!isOk()) {
            throw new IllegalStateException("No value: " + problems);
        }
        return value;
    }

    public List<Problem> problems() {
        return problems;
    }

    public List<Problem> errors() {
        return problems.stream().filter(Problem::isError).toList();
    }

    public <R> Result<R> map(Function<T, R> f) {
        return isOk() ? new Result<>(f.apply(value), problems) : new Result<>(null, problems);
    }

    /** Runs the next step only when this one worked, keeping the warnings of both. */
    public <R> Result<R> then(Function<T, Result<R>> next) {
        if (!isOk()) {
            return new Result<>(null, problems);
        }
        Result<R> r = next.apply(value);
        List<Problem> all = new ArrayList<>(problems);
        all.addAll(r.problems);
        return new Result<>(r.isOk() ? r.value : null, all);
    }

    @Override
    public String toString() {
        return isOk() ? "Ok(" + value + ", " + problems + ")" : "Fail(" + problems + ")";
    }
}
