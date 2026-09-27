package com.sack.rpgroll.furniture.function;

import com.sack.rpgroll.furniture.core.Offset;

import java.util.List;

/**
 * Lo que hace un mueble además de estar ahí. Cada parte es opcional (null o lista vacía).
 * <p>
 * Con clic derecho se usa lo primero que aplique, en este orden: tinte (cambia de variante),
 * estante (poner o quitar un objeto), asiento, almacén, papelera, estación de trabajo, estados y
 * acciones. Con clic derecho agachado, lo que tenga {@link Trigger#SNEAK_CLICK}; si no hay nada,
 * gira el mueble.
 */
public record FurnitureFunctions(
        Seat seat,
        Storage storage,
        Light light,
        States states,
        Workstation workstation,
        Shelf shelf,
        Trash trash,
        Ambient ambient,
        List<Action> actions) {

    public static final FurnitureFunctions NONE =
            new FurnitureFunctions(null, null, null, null, null, null, null, null, List.of());

    public FurnitureFunctions {
        actions = List.copyOf(actions);
    }

    /** Cuándo se dispara algo: clic derecho normal o agachado. */
    public enum Trigger {
        CLICK,
        SNEAK_CLICK
    }

    /**
     * Asientos: uno por posición (un sofá de tres plazas tiene tres).
     *
     * @param positions dónde se sienta cada uno, en el marco del mueble mirando al sur
     * @param height    altura del asiento sobre el suelo del bloque, en bloques
     */
    public record Seat(List<Offset> positions, double height) {
        public Seat {
            positions = positions.isEmpty() ? List.of(Offset.ZERO) : List.copyOf(positions);
        }
    }

    /**
     * Guarda objetos. El contenido viaja con el mueble (no hay base de datos) y cae al suelo
     * si se retira.
     *
     * @param ownerOnly solo el dueño (o quien tenga bypass) lo abre
     */
    public record Storage(int rows, String title, boolean ownerOnly) {
        public Storage {
            rows = Math.max(1, Math.min(6, rows));
        }
    }

    /** Papelera: lo que se deja dentro se borra al cerrar. */
    public record Trash(int rows, String title) {
        public Trash {
            rows = Math.max(1, Math.min(6, rows));
        }
    }

    /**
     * Da luz poniendo un bloque de luz invisible.
     *
     * @param level nivel 1-15
     * @param at    dónde va el bloque de luz, en el marco del mueble
     */
    public record Light(int level, Offset at) {
        public Light {
            level = Math.max(0, Math.min(15, level));
        }
    }

    /**
     * Estados que se van alternando (lámpara encendida/apagada, cortina abierta/cerrada).
     * Cada estado usa el modelo del mueble con {@code suffix} al final.
     */
    public record States(List<State> states, Trigger trigger) {
        public States {
            states = List.copyOf(states);
        }

        public State get(int index) {
            return states.get(Math.floorMod(index, states.size()));
        }
    }

    /**
     * @param light nivel de luz en este estado, o -1 para el de {@link Light}
     * @param sound sonido al pasar a este estado, o null
     */
    public record State(String id, String suffix, int light, String sound) {
    }

    /**
     * Abre un menú de trabajo: una estación vanilla (CRAFTING, ANVIL, SMITHING, LOOM,
     * STONECUTTER, GRINDSTONE, CARTOGRAPHY_TABLE, ENCHANTMENT...) o un carpintero
     * ({@code CARPENTER}, con la estación de recetas en {@code station}).
     */
    public record Workstation(String type, String station) {
        public boolean carpenter() {
            return type.equalsIgnoreCase("CARPENTER");
        }
    }

    /** Huecos donde se exhibe un objeto (una repisa, una mesa con un plato, una maceta). */
    public record Shelf(List<ShelfSlot> slots) {
        public Shelf {
            slots = List.copyOf(slots);
        }
    }

    /**
     * @param position centro del objeto exhibido, en el marco del mueble
     * @param scale    tamaño del objeto
     * @param flat     tumbado (un plato, un libro sobre la mesa) en vez de de pie
     */
    public record ShelfSlot(Offset position, float scale, boolean flat) {
    }

    /**
     * Partículas que salen solas mientras haya alguien cerca (humo de chimenea, llama de vela).
     *
     * @param state solo en este estado (-1 = en todos)
     */
    public record Ambient(String particle, Offset at, int count, double spread, int everyTicks, int state) {
        public Ambient {
            count = Math.max(1, count);
            everyTicks = Math.max(1, everyTicks);
        }
    }

    /**
     * Un comando, un sonido o un mensaje al usar el mueble. En el comando, {player} es quien lo
     * usa y {x} {y} {z} {world} dónde está el mueble.
     */
    public record Action(Trigger trigger, String command, boolean console, String sound, String message) {
    }

    public String stateSuffix(int state) {
        return states == null || states.states().isEmpty() ? "" : states.get(state).suffix();
    }

    public int lightLevel(int state) {

        if (states != null && !states.states().isEmpty()) {
            int stateLight = states.get(state).light();
            if (stateLight >= 0) {
                return stateLight;
            }
        }
        return light == null ? 0 : light.level();
    }

    public boolean hasLight() {
        return light != null || (states != null && states.states().stream().anyMatch(s -> s.light() > 0));
    }
}
