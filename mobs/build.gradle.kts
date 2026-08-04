plugins {
    id("rpgroll.addon-conventions")
}

group = "com.sack"
version = "1.0.0"

base.archivesName.set("RPGRoll-Mobs")

dependencies {
    // rpgroll.addon-conventions ya agrega compileOnly(:api) y compileOnly(:common).
    // :core para RPGRollAPI (jugador, razas, clases — usados en condiciones/loot).
    compileOnly(project(":core"))

    // Integraciones blandas (softdepend en plugin.yml, chequeadas en runtime):
    // reutiliza el sistema de Rareza de Items para colorear mobs de forma
    // consistente, y puede dar ítems de Items / iniciar quests de Quests
    // como loot, sin duplicar esos sistemas acá.
    compileOnly(project(":items"))
    compileOnly(project(":quests"))

    // VaultAPI es compileOnly en :core (no se propaga transitivamente) — hace falta
    // repetirlo acá porque el tipo Economy se referencia directamente (loot de dinero
    // vía RPGRollAPI.getEconomyProvider().getEconomy()) en MobEngine.
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") {
        exclude(group = "org.bukkit", module = "bukkit")
    }

    compileOnly("me.clip:placeholderapi:2.11.5")
}
