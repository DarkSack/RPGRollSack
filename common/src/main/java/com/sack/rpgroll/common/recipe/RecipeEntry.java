package com.sack.rpgroll.common.recipe;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Una receta tal como la muestra el visor: qué entra, qué sale y dónde se hace.
 *
 * @param id        id único dentro de su origen ({@code minecraft:torch}, {@code forja/espada_runica})
 * @param source    quién la aporta ("Minecraft", "RPGRoll-Crafting"...); si viene vacío el visor
 *                  pone el nombre del {@link RecipeSource}
 * @param station   dónde se hace
 * @param width     1 a 3 = receta con forma, {@code inputs} fila a fila con ese ancho (los huecos
 *                  vacíos son {@link RecipeSlot#EMPTY}); 0 = ingredientes sin orden
 * @param inputs    lo que entra
 * @param outputs   lo que sale (el primero es el principal)
 * @param notes     líneas extra (tiempo, experiencia, coste, probabilidad de fallo...), legacy
 *                  {@code &} o MiniMessage
 * @param visibleTo a quién se le muestra; null = a todos
 */
public record RecipeEntry(String id, String source, RecipeStation station, int width, List<RecipeSlot> inputs,
        List<ItemStack> outputs, List<String> notes, Predicate<Player> visibleTo) {

    public RecipeEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(station, "station");
        source = source == null ? "" : source;
        width = Math.max(0, Math.min(3, width));
        inputs = inputs == null ? List.of() : List.copyOf(inputs);
        List<ItemStack> out = new ArrayList<>();
        if (outputs != null) {
            for (ItemStack stack : outputs) {
                if (stack != null && !stack.getType().isAir()) {
                    out.add(stack.clone());
                }
            }
        }
        outputs = List.copyOf(out);
        notes = notes == null ? List.of() : List.copyOf(notes);
    }

    public static Builder builder(String id, RecipeStation station) {
        return new Builder(id, station);
    }

    public boolean shaped() {
        return width > 0;
    }

    public boolean isVisibleTo(Player player) {
        return visibleTo == null || visibleTo.test(player);
    }

    @Override
    public List<ItemStack> outputs() {
        return outputs.stream().map(ItemStack::clone).toList();
    }

    /** La misma receta con otro origen (el visor lo usa para rellenar {@code source}). */
    public RecipeEntry withSource(String newSource) {
        return new RecipeEntry(id, newSource, station, width, inputs, outputs, notes, visibleTo);
    }

    public static final class Builder {

        private final String id;
        private final RecipeStation station;
        private String source;
        private int width;
        private final List<RecipeSlot> inputs = new ArrayList<>();
        private final List<ItemStack> outputs = new ArrayList<>();
        private final List<String> notes = new ArrayList<>();
        private Predicate<Player> visibleTo;

        private Builder(String id, RecipeStation station) {
            this.id = id;
            this.station = station;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        /** Receta con forma: {@code slots} fila a fila, {@code width} por fila. */
        public Builder shaped(int width, List<RecipeSlot> slots) {
            this.width = width;
            this.inputs.clear();
            this.inputs.addAll(slots);
            return this;
        }

        public Builder input(RecipeSlot slot) {
            inputs.add(slot);
            return this;
        }

        public Builder input(ItemStack... options) {
            return input(RecipeSlot.of(options));
        }

        public Builder output(ItemStack stack) {
            outputs.add(stack);
            return this;
        }

        public Builder note(String line) {
            if (line != null && !line.isBlank()) {
                notes.add(line);
            }
            return this;
        }

        public Builder visibleTo(Predicate<Player> visibleTo) {
            this.visibleTo = visibleTo;
            return this;
        }

        public RecipeEntry build() {
            return new RecipeEntry(id, source, station, width, inputs, outputs, notes, visibleTo);
        }
    }
}
