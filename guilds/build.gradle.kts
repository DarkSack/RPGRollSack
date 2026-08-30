plugins {
    id("rpgroll.addon-conventions")
}

group = "com.sack"
version = "1.0.0"

base.archivesName.set("RPGRoll-Guilds")

dependencies {
    // rpgroll.addon-conventions ya agrega compileOnly(:api) y compileOnly(:common).
    // :core para RPGRollAPI (nivel/clase del jugador para matchmaking y requisitos
    // de creación de guild) y el framework de GUIs (InventoryGUI/ItemBuilder).
    compileOnly(project(":core"))

    // Integraciones blandas (softdepend en plugin.yml, chequeadas en runtime):
    // requisito de creación "item" (Items) y "quest" (Quests) para fundar una guild.
    compileOnly(project(":items"))
    compileOnly(project(":quests"))

    // VaultAPI: tesorería de guild (depósitos/retiros/impuestos) y requisito de
    // creación por dinero. compileOnly en :core no se propaga transitivamente.
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") {
        exclude(group = "org.bukkit", module = "bukkit")
    }

    compileOnly("me.clip:placeholderapi:2.11.5")

    // :common trae ContentParser/RPGContent/LangManager usados por parsers/enums testeados.

    // rpgroll.plugin-conventions fija mockito-core/mockito-junit-jupiter en 5.15.2, cuya
    // versión de ByteBuddy no soporta instrumentar interfaces de Bukkit en el JDK 25 del
    // toolchain (falla "Could not modify all classes ..."); Gradle resuelve por versión más
    // alta entre coordenadas iguales, así que forzamos una versión más nueva solo acá.
    testImplementation("org.mockito:mockito-core:5.23.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.23.0")
}
