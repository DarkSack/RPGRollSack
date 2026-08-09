package com.sack.rpgroll.crafting.condition;

public record RecipeCondition(ConditionType type, String value, int minValue) {

    public static RecipeCondition levelMin(int level) {
        return new RecipeCondition(ConditionType.LEVEL_MIN, null, level);
    }

    public static RecipeCondition of(ConditionType type, String value) {
        return new RecipeCondition(type, value, 0);
    }

    public static RecipeCondition jobMin(String jobId, int level) {
        return new RecipeCondition(ConditionType.JOB_MIN, jobId, level);
    }

}
