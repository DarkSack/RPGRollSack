plugins {
    id("rpgroll.addon-conventions")
}

group = "com.sack"
version = "1.0.0"

base.archivesName.set("RPGRoll-Workers")

dependencies {
    // rpgroll.addon-conventions ya agrega compileOnly(:api) y compileOnly(:common).
    // :core es para RPGRollAPI (nivel del jugador), VaultEconomyProvider (salarios) y el
    // framework de GUIs compartido.
    compileOnly(project(":core"))

    // Tests necesitan las clases reales de :common (RPGContent/EntityReskin) en el classpath.

    // Integraciones blandas (softdepend en plugin.yml, chequeadas en runtime):
    // el ganadero de verdad cuida animales de Ranching, el pescador de verdad
    // pesca vía Fishing, el clima/estación real de Seasons modula la IA, los
    // gremios pueden compartir trabajadores, y las notificaciones de eventos
    // de worker pueden disparar partículas/sonidos de RPGRoll-Particles.
    compileOnly(project(":sackeffects"))
    compileOnly(project(":effects"))
    compileOnly(project(":seasons"))
    compileOnly(project(":ranching"))
    compileOnly(project(":fishing"))
    compileOnly(project(":guilds"))

    // Vault en sí — solo hace falta para el tipo Economy que expone
    // VaultEconomyProvider de :core (EconomyService lo usa para cobrar salarios).
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
}
