plugins {
    id("rpgroll.addon-conventions")
}

group = "com.sack"
version = "1.0.0"

base.archivesName.set("RPGRoll-Fishing")

dependencies {
    // rpgroll.addon-conventions ya agrega compileOnly(:api) y compileOnly(:common).
    // :core es para RPGRollAPI (nivel del jugador) y el framework de GUIs compartido.
    compileOnly(project(":core"))

    // compileOnly no se propaga al source set de test (:api/:common ya los aporta
    // rpgroll.addon-conventions; :core hace falta declararlo acá).
    testImplementation(project(":core"))

    // Integraciones blandas (softdepend en plugin.yml, chequeadas en runtime):
    // capturas pueden disparar partículas/sonidos de RPGRoll-FX, aplicar efectos
    // de estado de RPGRoll-Effects (ej. una anguila eléctrica que aturde), y las
    // condiciones de especie pueden filtrar por la estación real de RPGRoll-Seasons.
    compileOnly(project(":fx"))
    compileOnly(project(":effects"))
    compileOnly(project(":seasons"))
}
