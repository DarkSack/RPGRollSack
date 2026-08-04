package com.sack.rpgroll.ranching.core.breeding;

public record BreedingAttemptResult(boolean success, String message) {

    public static BreedingAttemptResult ok(String message) {
        return new BreedingAttemptResult(true, message);
    }

    public static BreedingAttemptResult fail(String message) {
        return new BreedingAttemptResult(false, message);
    }

}
