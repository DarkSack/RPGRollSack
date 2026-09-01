package com.sack.rpgroll.traps.ammo;

import com.sack.rpgroll.common.content.RPGContent;
import com.sack.rpgroll.traps.core.TrapAction;

import org.bukkit.Material;

import java.util.Objects;

/**
 * Un tipo de munición para torretas.
 * <p>
 * La munición es la que decide QUÉ pasa al impactar: reusa el mismo
 * {@link TrapAction} que trampas y torretas, así que agregar una acción al
 * registro la habilita también como munición sin tocar nada más.
 * <p>
 * La torreta aporta la puntería (radio, objetivos, cadencia) y la munición
 * el efecto — por eso una misma torreta cambia de comportamiento según lo
 * que se le cargue.
 *
 * @param icon            material del ítem que representa esta munición.
 * @param customModelData null = sin CMD.
 * @param impact          qué ejecuta al impactar; null = usa el de la torreta.
 * @param stackSize       cuántas unidades entra por ítem al cargarla.
 */
public record AmmoDefinition(
        String id,
        String displayName,
        String description,
        Material icon,
        Integer customModelData,
        TrapAction impact,
        int stackSize) implements RPGContent {

    public AmmoDefinition {
        Objects.requireNonNull(id, "id no puede ser null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id no puede estar vacío");
        }

        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        description = description == null ? "" : description;
        icon = icon == null ? Material.ARROW : icon;
        stackSize = stackSize <= 0 ? 1 : stackSize;
    }

}
