package com.sack.rpgroll.recipes.read;

import com.sack.rpgroll.common.lang.LangManager;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Las líneas extra de las recetas que lee el propio visor, en el idioma configurado. */
public final class Notes {

    private final LangManager lang;

    public Notes(LangManager lang) {
        this.lang = lang;
    }

    public static String number(double value) {
        return new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT)).format(value);
    }

    public String cookingTime(int ticks) {
        return lang.raw("note.cooking_time", "seconds", number(ticks / 20.0));
    }

    public String experience(float experience) {
        return lang.raw("note.experience", "value", number(experience));
    }

    public String keepsComponents() {
        return lang.raw("note.keeps_components");
    }

    public String appliesTrim() {
        return lang.raw("note.applies_trim");
    }

    public String brewing() {
        return lang.raw("note.brewing");
    }

    public String raw(String key, Object... placeholders) {
        return lang.raw(key, placeholders);
    }
}
