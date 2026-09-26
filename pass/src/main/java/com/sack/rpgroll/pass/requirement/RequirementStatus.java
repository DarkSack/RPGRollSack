package com.sack.rpgroll.pass.requirement;

import net.kyori.adventure.text.Component;

/** Un requisito ya evaluado: si se cumple y la línea que se le enseña al jugador. */
public record RequirementStatus(boolean met, Component line) {
}
