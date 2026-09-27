plugins {
    id("rpgroll.addon-conventions")
}

group = "com.sack"
version = "1.0.0"

base.archivesName.set("RPGRoll-Furniture")

dependencies {
    // El carpintero cobra en dinero además de materiales, por Vault.
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") {
        exclude(group = "org.bukkit", module = "bukkit")
    }

    // El pack de Java que trae el jar (resourcepack/) se registra en
    // SackResourcePack si está instalado. softdepend: sin él, el dueño del
    // servidor sirve el pack por su cuenta.
    compileOnly(project(":sackresourcepack"))
}
