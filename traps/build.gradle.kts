plugins {
    id("rpgroll.addon-conventions")
}

group = "com.sack"
version = "1.0.0"

base.archivesName.set("RPGRoll-Traps")

dependencies {
    // rpgroll.addon-conventions ya agrega compileOnly(:api) y compileOnly(:common),
    // pero este addon también usa InventoryGUI/ItemBuilder (framework de GUIs) que
    // vive físicamente en :core.
    compileOnly(project(":core"))

    // Integraciones blandas (softdepend en plugin.yml, chequeadas en runtime vía
    // Bukkit.getPluginManager().isPluginEnabled(...) antes de tocar sus clases):
    // llaves de bloque protegido (RPGRoll-Items) y efectos de estado al disparar
    // una trampa (RPGRoll-Effects). El addon debe arrancar sin ninguno de los dos.
    compileOnly(project(":items"))
    compileOnly(project(":effects"))

    // Torretas: reconocer mobs custom de RPGRoll-Mobs como objetivo hostil
    // (ver traps/integration/MobsIntegration.java) — softdepend igual criterio.
    compileOnly(project(":mobs"))
}
