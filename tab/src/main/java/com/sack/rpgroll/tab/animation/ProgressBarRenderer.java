package com.sack.rpgroll.tab.animation;

/** Genera barras de progreso ASCII (ej. {@code ████████░░}) a partir de un porcentaje 0-100. */
public final class ProgressBarRenderer {

    private ProgressBarRenderer() {
    }

    public static String render(double percent0to100, int length, char filledChar, char emptyChar) {

        double clamped = Math.max(0, Math.min(100, percent0to100));
        int filled = (int) Math.round((clamped / 100.0) * length);

        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            sb.append(i < filled ? filledChar : emptyChar);
        }

        return sb.toString();
    }

}
