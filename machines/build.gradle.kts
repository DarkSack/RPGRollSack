plugins {
    id("rpgroll.addon-conventions")
}

group = "com.sack"
version = "1.0.0"

base.archivesName.set("RPGRoll-Machines")

dependencies {
    // Solo RPGRoll-Lib (rpgroll.addon-conventions ya agrega :api y :common como compileOnly).
    // GriefPrevention se consulta por reflexión (Claims): no hace falta compilar contra él.

    // Las mejoras pueden cobrarse en dinero, por Vault.
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
}
