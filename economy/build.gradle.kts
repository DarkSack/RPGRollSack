plugins {
    id("rpgroll.addon-conventions")
}

group = "com.sack"
version = "1.0.0"

base.archivesName.set("RPGRoll-Economy")

dependencies {
    // rpgroll.addon-conventions ya agrega compileOnly(:api) y compileOnly(:common).
    // :core para RPGRollAPI (jugador) y el framework de GUIs (InventoryGUI/ItemBuilder/TabCompleteUtil).
    compileOnly(project(":core"))

    // Tests necesitan las clases reales de :common (RPGContent/ContentManager) en el classpath,
    // no solo compileOnly (que rpgroll.addon-conventions no propaga a testImplementation).

    // VaultAPI: RPGRoll-Economy es PROVEEDOR del servicio Economy (no solo consumidor como :core),
    // así que el resto del ecosistema (Jobs, Guilds, Items, Workers) queda funcional automáticamente
    // en cuanto este addon está instalado, sin cambiar una línea de esos módulos.
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") {
        exclude(group = "org.bukkit", module = "bukkit")
    }

    compileOnly("me.clip:placeholderapi:2.11.5")

    // Integraciones blandas opcionales (softdepend en plugin.yml):
    compileOnly(project(":guilds"))
    compileOnly(project(":seasons"))
}
