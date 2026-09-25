plugins {
    id("rpgroll.addon-conventions")
}

group = "com.sack"
version = "1.0.0"

base.archivesName.set("RPGRoll-Ascension")

dependencies {
    // rpgroll.addon-conventions ya agrega compileOnly(:api) y compileOnly(:common).
    // :core para RPGRollAPI (razas, clases, jobs, skills, traits, jugador, economía).
    compileOnly(project(":core"))

    // Integración opcional: los nodos de talento pueden otorgar encantamientos
    // custom si RPGRoll-Enchantments está presente (softdepend, se chequea en runtime).
    compileOnly(project(":enchantments"))

    // Integración opcional: requisitos de "quest completada" se validan contra
    // RPGRoll-Quests si está presente (softdepend, se chequea en runtime).
    compileOnly(project(":quests"))

    // Logros y reputación por matar mobs de RPGRoll-Mobs (MobDeathEvent).
    compileOnly(project(":mobs"))

    // Recompensas en dinero. VaultAPI es compileOnly en :core y no se
    // propaga, así que hay que repetirlo (igual que en Items y Guilds).
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") {
        exclude(group = "org.bukkit", module = "bukkit")
    }

    compileOnly("me.clip:placeholderapi:2.11.5")
}
