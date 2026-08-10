plugins {
    id("rpgroll.addon-conventions")
}

group = "com.sack"
version = "1.0.0"

base.archivesName.set("RPGRoll-TAB")

dependencies {
    // rpgroll.addon-conventions ya agrega compileOnly(:api) y compileOnly(:common),
    // pero este addon también usa TabCompleteUtil/ComponentUtils/InventoryGUI, que
    // viven físicamente en :core.
    compileOnly(project(":core"))

    // ProtocolLib: solo para las funciones avanzadas que la API pública de
    // Paper no cubre (layout de grilla custom, nametag distinto por
    // observador, ocultar entradas del tablist por-viewer). Softdepend en
    // plugin.yml — el resto del addon funciona sin él. Mismo groupId/versión
    // que ya usa RPGRoll-NPCs (ya resuelto en caché en este entorno).
    compileOnly("com.comphenix.protocol:ProtocolLib:5.3.0")

    // PlaceholderAPI: puente opcional para exponer placeholders ya
    // registrados por otros addons (Economy, Ascension, Mobs, etc.) sin
    // depender de esos módulos directamente.
    compileOnly("me.clip:placeholderapi:2.11.5")
}
