package com.sack.rpgroll.structures.api;

import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Origen de materiales para las obras. Un id completo es {@code tipo:id}; aquí {@code id} llega sin el
 * prefijo de tipo y {@code stack} nunca es null ni aire.
 */
public interface MaterialProvider {

    String type();

    boolean knows(String id);

    /** Reconoce por identidad real del ítem, nunca por nombre o lore. */
    boolean matches(String id, ItemStack stack);

    /** Un ítem de muestra, para mostrarlo en los menús. */
    Optional<ItemStack> sample(String id);
}
