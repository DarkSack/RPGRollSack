plugins {
    id("rpgroll.addon-conventions")
}

group = "com.sack"
version = "1.0.0"

base.archivesName.set("RPGRoll-Dungeons")

dependencies {
    // rpgroll.addon-conventions ya agrega compileOnly(:api) y compileOnly(:common).
    // :core para RPGRollAPI (jugador, jobs, skills, economía) y el framework de
    // GUIs (InventoryGUI/ItemBuilder) que reutiliza el Dungeon Studio.
    compileOnly(project(":core"))

    // Los jefes de una dungeon SON mobs de RPGRoll-Mobs — integración dura (no
    // softdepend): sin este addon instalado, Dungeons no tiene forma de spawnear
    // enemigos, así que directamente depende de él.
    compileOnly(project(":mobs"))

    // El grupo con el que se entra a una dungeon es un Team real de
    // RPGRoll-Guilds — integración dura, reemplaza el DungeonParty temporal
    // que existía antes de que Guilds existiera.
    compileOnly(project(":guilds"))

    // Integraciones blandas (softdepend en plugin.yml, chequeadas en runtime):
    // loot puede dar ítems de Items / iniciar quests de Quests, igual que Mobs.
    compileOnly(project(":items"))
    compileOnly(project(":quests"))

    // VaultAPI es compileOnly en :core (no se propaga transitivamente) — hace falta
    // repetirlo acá porque el tipo Economy se referencia directamente (recompensas
    // de dinero) en el motor.
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") {
        exclude(group = "org.bukkit", module = "bukkit")
    }

    compileOnly("me.clip:placeholderapi:2.11.5")

    // WorldEdit — softdepend (ver plugin.yml), solo para importar schematics .schem.
    // compileOnly a propósito: todo el código que toca sus clases vive aislado en
    // SchematicBridge, nunca referenciado salvo detrás de un chequeo en runtime de
    // que el plugin WorldEdit esté cargado (así el addon sigue funcionando sin él).
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.4.5") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
}
