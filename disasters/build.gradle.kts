plugins {
    id("rpgroll.addon-conventions")
}

group = "com.sack"
version = "1.1.5"

base.archivesName.set("RPGRoll-Disasters")

dependencies {
    // rpgroll.addon-conventions ya agrega compileOnly(:api) y compileOnly(:common).

    // Integraciones blandas (softdepend en plugin.yml, chequeadas en runtime): la estación
    // real de RPGRoll-Seasons para filtrar el sorteo, y los mobs de RPGRoll-Mobs.
    compileOnly(project(":seasons"))
    compileOnly(project(":mobs"))
}
