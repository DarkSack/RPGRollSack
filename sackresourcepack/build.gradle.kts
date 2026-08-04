plugins {
    // A propósito NO usa "rpgroll.addon-conventions" — ese convention agrega
    // compileOnly(:api)/compileOnly(:common), y SackResourcePack es un
    // framework genuinamente independiente (Paper/Spigot/Folia, no solo
    // RPGRoll): no debe depender de ningún módulo del ecosistema RPGRoll.
    id("rpgroll.plugin-conventions")
}

group = "com.sack"
version = "1.0.0"

base.archivesName.set("SackResourcePack")

dependencies {
    // Gson ya viene embebido en el server de Paper/Spigot en tiempo de
    // ejecución (mismo criterio que :core) — compileOnly alcanza, no hace
    // falta empaquetarlo ni relocarlo.
    compileOnly("com.google.code.gson:gson:2.11.0")
}
