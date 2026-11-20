plugins {
    id("rpgroll.addon-conventions")
}

group = "com.sack"
version = "1.1.12"

base.archivesName.set("RPGRoll-Emotes")

dependencies {
    // Motor de animación del jugador: lo aporta el plugin BetterModel en el servidor.
    compileOnly("io.github.toxicity188:bettermodel-api:3.6.0")
    compileOnly("io.github.toxicity188:bettermodel-bukkit-api:3.6.0")
    testImplementation("io.github.toxicity188:bettermodel-api:3.6.0")
    testImplementation("io.github.toxicity188:bettermodel-bukkit-api:3.6.0")

    // El pack se registra en SackResourcePack si está instalado (softdepend).
    compileOnly(project(":sackresourcepack"))
}
