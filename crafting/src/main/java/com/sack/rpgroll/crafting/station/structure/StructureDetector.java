package com.sack.rpgroll.crafting.station.structure;

import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Verifica que los bloques alrededor del disparador de una {@code CustomStation}
 * cumplan su {@code structureRequirements}. Un requisito con un nombre de
 * Material inválido se ignora (no bloquea la apertura) — se documenta como
 * error de configuración del admin, no del jugador.
 */
public class StructureDetector {

    public Optional<StructureRequirement> findMissing(Block trigger, List<StructureRequirement> requirements) {

        for (StructureRequirement requirement : requirements) {

            Material required;
            try {
                required = Material.valueOf(requirement.material().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                continue;
            }

            Block relative = trigger.getRelative(requirement.dx(), requirement.dy(), requirement.dz());
            if (relative.getType() != required) {
                return Optional.of(requirement);
            }
        }

        return Optional.empty();
    }

    public boolean matches(Block trigger, List<StructureRequirement> requirements) {
        return findMissing(trigger, requirements).isEmpty();
    }

}
