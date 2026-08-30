plugins {
    id("rpgroll.addon-conventions")
}

group = "com.sack"
version = "1.0.0"

base.archivesName.set("RPGRoll-Crafting")

dependencies {
    // rpgroll.addon-conventions ya agrega compileOnly(:api) y compileOnly(:common).
    // :core para RPGRollAPI (jugador, jobs, skills) y el framework de GUIs (InventoryGUI/ItemBuilder/TabCompleteUtil).
    compileOnly(project(":core"))

    // Integraciones blandas opcionales (softdepend en plugin.yml):
    // :items para reconocer ítems personalizados de RPGRoll-Items como ingrediente/resultado (vía PDC).
    compileOnly(project(":items"))
    // :economy para costos/recompensas monetarias en recetas y estaciones.
    compileOnly(project(":economy"))
    // :guilds y :seasons para condiciones de receta (gremio, estación climática).
    compileOnly(project(":guilds"))
    compileOnly(project(":seasons"))

    // rpgroll.addon-conventions declara :api y :common como compileOnly, lo
    // que no se propaga al classpath de test — hace falta repetirlos acá
    // para que los tests unitarios puedan referenciar sus tipos (ej.
}
