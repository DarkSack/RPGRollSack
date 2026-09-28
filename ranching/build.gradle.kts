plugins {
    id("rpgroll.addon-conventions")
}

group = "com.sack"
version = "1.0.0"

base.archivesName.set("RPGRoll-Ranching")

dependencies {
    // rpgroll.addon-conventions ya agrega compileOnly(:api) y compileOnly(:common).
    // :core es para RPGRollAPI (nivel del jugador) y el framework de GUIs compartido.

    // Tests necesitan las clases reales de :common (RPGContent) en el classpath.

    // Integraciones blandas (softdepend en plugin.yml, chequeadas en runtime):
    // el bienestar/producción puede disparar partículas/sonidos de RPGRoll-FX,
    // enfermedades/curas pueden aplicar efectos de estado de RPGRoll-Effects, y
    // la estación real de RPGRoll-Seasons modula reproducción/fertilidad/enfermedad/producción.
    compileOnly(project(":fx"))
    compileOnly(project(":effects"))
    compileOnly(project(":seasons"))

    // Registro del pack de modelos de fábrica (softdepend; ver registerPack()).
    compileOnly(project(":sackresourcepack"))

    // Modelos animados de las razas con FreeMinecraftModels (GPLv3, gratis). Integración
    // blanda: sin el plugin los animales se ven vanilla. Ver integration/ModelsIntegration.
    compileOnly("com.magmaguy:FreeMinecraftModels:2.12.3") {
        isTransitive = false
    }

    // El mercado de animales cobra y paga por Vault (VaultEconomy de :common).
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
}

repositories {
    maven("https://repo.magmaguy.com/releases")
}
