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

    // :common trae ContentParser/RPGContent/EntityReskin usados por los parsers y
    // definiciones que se testean acá; compileOnly no alcanza para compilar los tests.

    // rpgroll.plugin-conventions fija mockito-core/mockito-junit-jupiter en 5.15.2, cuya
    // versión de ByteBuddy no soporta instrumentar interfaces de Bukkit en el JDK 25 del
    // toolchain (falla "Could not modify all classes ..."); Gradle resuelve por versión más
    // alta entre coordenadas iguales, así que forzamos una versión más nueva solo acá.
    testImplementation("org.mockito:mockito-core:5.23.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.23.0")

    // VaultAPI es compileOnly en :core (no se propaga transitivamente) — hace falta
    // repetirlo acá porque el tipo Economy se referencia directamente (loot de dinero
    // vía RPGRollAPI.getEconomyProvider().getEconomy()) en MobEngine.
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") {
        exclude(group = "org.bukkit", module = "bukkit")
    }

    compileOnly("me.clip:placeholderapi:2.11.5")
}
