plugins {
    id("rpgroll.addon-conventions")
}

group = "com.sack"
version = "1.0.0"

base.archivesName.set("RPGRoll-Seasons")

dependencies {
    // rpgroll.addon-conventions ya agrega compileOnly(:api) y compileOnly(:common).
    // :core es para el framework de GUIs compartido (InventoryGUI/ItemBuilder).
    compileOnly(project(":core"))

    // Tests necesitan las clases reales de :common (RPGContent/ContentManager) en el classpath.

    // Integraciones blandas (softdepend en plugin.yml, chequeadas en runtime):
    // clima/eventos pueden disparar partículas/sonidos de RPGRoll-Particles, aplicar
    // efectos de estado de RPGRoll-Effects, y hacer spawnear mobs/jefes de
    // temporada reusando definiciones de RPGRoll-Mobs.
    compileOnly(project(":sackeffects"))
    compileOnly(project(":effects"))
    compileOnly(project(":mobs"))
}
