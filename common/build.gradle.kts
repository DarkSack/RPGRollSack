plugins {
    id("rpgroll.plugin-conventions")
}

group = "com.sack"
version = "1.1.14"

// :common se distribuye como un plugin propio, RPGRoll-Lib: todos los módulos
// lo declaran en depend y usan sus clases en runtime en vez de llevar cada uno
// su copia. Ver RPGRollLib.
base.archivesName.set("RPGRoll-Lib")

dependencies {
    compileOnly("com.google.code.gson:gson:2.11.0")
    testImplementation("com.google.code.gson:gson:2.11.0")

    compileOnly(project(":sackresourcepack"))

    // VaultEconomy: cualquier módulo cobra por Vault sin pasar por el core.
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") {
        exclude(group = "org.bukkit", module = "bukkit")
    }

    // WorldEdit/FAWE: softdepend (ver plugin.yml), solo para leer .schem. compileOnly: la única
    // clase que toca com.sk89q.* es structure.SchemBlockSource, y solo se carga si el plugin está activo.
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.4.5") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
}
