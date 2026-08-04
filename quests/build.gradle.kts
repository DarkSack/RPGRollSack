plugins {
    id("rpgroll.addon-conventions")
}

group = "com.sack"
version = "1.0.0"

base.archivesName.set("RPGRoll-Quests")

dependencies {
    // rpgroll.addon-conventions ya agrega compileOnly(:api) y compileOnly(:common).
    // Este addon usa RPGRollAPI (jugador, razas, clases, jobs, skills, economía Vault)
    // que vive físicamente en :core.
    compileOnly(project(":core"))

    // VaultAPI es compileOnly en :core (no se propaga transitivamente) — hace falta
    // repetirlo acá porque el tipo Economy se referencia directamente (a través de
    // RPGRollAPI.getEconomyProvider().getEconomy()) en varias clases de este addon.
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") {
        exclude(group = "org.bukkit", module = "bukkit")
    }

    compileOnly("me.clip:placeholderapi:2.11.5")
}
