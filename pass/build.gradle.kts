plugins {
    id("rpgroll.addon-conventions")
}

group = "com.sack"
version = "1.0.0"

base.archivesName.set("RPGRoll-Pass")

dependencies {
    // InventoryGUI, ItemBuilder, la API de jugadores y la economía por Vault.
    compileOnly(project(":core"))
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") {
        exclude(group = "org.bukkit", module = "bukkit")
    }

    // Misiones enlazadas con otros módulos. Solo se registran si el plugin
    // está habilitado en el servidor; nunca se empaquetan.
    compileOnly(project(":quests"))
    compileOnly(project(":mobs"))

    testImplementation(project(":core"))
}
