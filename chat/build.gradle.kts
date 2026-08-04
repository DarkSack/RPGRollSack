plugins {
    id("rpgroll.addon-conventions")
}

group = "com.sack"
version = "1.0.0"

base.archivesName.set("RPGRoll-Chat")

dependencies {
    // rpgroll.addon-conventions ya agrega compileOnly(:api) y compileOnly(:common).
    // :core para RPGRollAPI (nivel/clase/raza/job del jugador para los formatos
    // dinámicos) y el framework de GUIs (InventoryGUI/ItemBuilder).
    compileOnly(project(":core"))

    // Integración blanda (softdepend en plugin.yml): canales/menciones de
    // Guild y Team, y los placeholders %rpgrollguilds_...% si está instalado.
    compileOnly(project(":guilds"))

    compileOnly("me.clip:placeholderapi:2.11.5")
}
