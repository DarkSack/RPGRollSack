package com.sack.rpgroll.quests.core;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Qué capítulo de una categoría (normalmente la historia principal) le toca a un jugador:
 * el que tiene activo o, si no hay ninguno, el primero sin completar. Es lo que usa
 * {@code /quest continuar}, para que un botón de menú baste para seguir la historia sin
 * escribir ids.
 * <p>
 * Orden de los capítulos: nivel requerido de menor a mayor y, a igual nivel, el id en orden
 * natural ({@code camino_2} antes que {@code camino_10}).
 */
public final class StoryProgress {

    private static final Pattern TRAILING_NUMBER = Pattern.compile("^(.*?)(\\d+)$");

    public static final Comparator<Quest> ORDER = Comparator
            .comparingInt((Quest quest) -> quest.requirements().level())
            .thenComparing(Quest::id, StoryProgress::compareNatural);

    /** El capítulo que toca y si ya está en curso. */
    public record Step(Quest quest, boolean active) {
    }

    private StoryProgress() {
    }

    public static Optional<Step> current(Collection<Quest> quests, QuestCategory category,
            Predicate<String> isActive, Predicate<String> hasCompleted) {

        var chapters = quests.stream().filter(quest -> quest.category() == category).sorted(ORDER).toList();

        for (Quest chapter : chapters) {
            if (isActive.test(chapter.id())) {
                return Optional.of(new Step(chapter, true));
            }
        }

        return chapters.stream()
                .filter(chapter -> !hasCompleted.test(chapter.id()))
                .findFirst()
                .map(chapter -> new Step(chapter, false));
    }

    static int compareNatural(String left, String right) {

        Matcher a = TRAILING_NUMBER.matcher(left);
        Matcher b = TRAILING_NUMBER.matcher(right);

        if (a.matches() && b.matches() && a.group(1).equals(b.group(1))) {
            int byNumber = Long.compare(Long.parseLong(a.group(2)), Long.parseLong(b.group(2)));
            if (byNumber != 0) {
                return byNumber;
            }
        }

        return left.compareTo(right);
    }

}
