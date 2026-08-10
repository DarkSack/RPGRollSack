plugins {
    id("rpgroll.addon-conventions")
}

group = "com.sack"
version = "1.0.0"

base.archivesName.set("RPGRoll-Extras")

dependencies {
    // rpgroll.addon-conventions ya agrega compileOnly(:api) y compileOnly(:common),
    // pero este addon también usa TabCompleteUtil/ComponentUtils, que viven
    // físicamente en :core.
    compileOnly(project(":core"))

    // PlaceholderAPI: puente opcional para exponer stats/conditions como
    // placeholders vía expansión propia.
    compileOnly("me.clip:placeholderapi:2.11.5")

    // RPGRoll-TAB: solo para el bridge opcional de la sección 13 (registrar
    // placeholders vía TABPlaceholderRegistry si el plugin está instalado) —
    // nunca se empaqueta, y todo acceso está guardeado por presencia real
    // del plugin en runtime (mismo patrón que PlaceholderApiBridge).
    compileOnly(project(":tab"))
}
