plugins {
    id("rpgroll.addon-conventions")
}

group = "com.sack"
version = "1.0.0"

base.archivesName.set("RPGRoll-Ranching")

dependencies {
    // rpgroll.addon-conventions ya agrega compileOnly(:api) y compileOnly(:common).
    // :core es para RPGRollAPI (nivel del jugador) y el framework de GUIs compartido.
    compileOnly(project(":core"))

    // Tests necesitan las clases reales de :common (RPGContent) en el classpath.

    // Integraciones blandas (softdepend en plugin.yml, chequeadas en runtime):
    // el bienestar/producción puede disparar partículas/sonidos de RPGRoll-FX,
    // enfermedades/curas pueden aplicar efectos de estado de RPGRoll-Effects, y
    // la estación real de RPGRoll-Seasons modula reproducción/fertilidad/enfermedad/producción.
    compileOnly(project(":fx"))
    compileOnly(project(":effects"))
    compileOnly(project(":seasons"))
}
