package com.sack.rpgroll.extras.backpack;

import org.bukkit.Material;

import java.util.List;

/** El aspecto de un botón o relleno de la fila inferior. */
public record BackpackButton(Material material, String name, List<String> lore) {
}
